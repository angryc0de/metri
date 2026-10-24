/*
 * Metri 的全部计算口径，从 LeanLogMath.kt 1:1 移植，公式一个字没改。
 * 界面上任何数字都必须经过这里，不允许出现第二套算法。
 */
(function (global) {
  'use strict';

  var KCAL_PER_KG_FAT = 7700;
  var MINUTES_PER_DAY = 1440;
  var PROTEIN_AUTO_FACTOR = 1.8;
  var MIN_WEIGHT_POINTS = 8;
  var MIN_SPAN_DAYS = 14;
  var MIN_INTAKE_COVERAGE = 0.6;
  var ANOMALY_THRESHOLD = 800;

  /* Mifflin-St Jeor。体重取最近一条体重记录。 */
  function bmr(gender, weightKg, heightCm, age) {
    var base = 10 * weightKg + 6.25 * heightCm - 5 * age;
    return gender === 'MALE' ? base + 5 : base - 161;
  }

  /* 活动系数只描述「训练之外的日常活动」，训练消耗单独加，不重复计入。 */
  function tdeeBase(bmrValue, activityFactor) {
    return bmrValue * activityFactor;
  }

  /*
   * 单条力量记录的时长（分钟）：
   * sets × (reps × tempo + rest) − rest   最后一组结束后不再休息，下限 60 秒。
   */
  function strengthMinutes(sets, reps, restSec, tempoSecPerRep) {
    var tempo = tempoSecPerRep == null ? 3 : tempoSecPerRep;
    var sec = sets * (reps * tempo + restSec) - restSec;
    return Math.max(60, sec) / 60;
  }

  /* 毛消耗：MET × 3.5 × 体重kg / 200 × 分钟。有氧与力量共用。 */
  function grossKcal(met, weightKg, minutes) {
    return met * 3.5 * weightKg / 200 * minutes;
  }

  /*
   * 净消耗 = 毛消耗 − BMR/1440 × 分钟。
   * TDEE 已经用「BMR × 活动系数」覆盖了全天 24 小时的静息消耗，
   * 训练那一小时的静息部分必须扣掉，否则算了两遍。
   */
  function netKcal(gross, bmrValue, minutes) {
    return Math.max(0, gross - bmrValue / MINUTES_PER_DAY * minutes);
  }

  /* 缺口 = 消耗 − 摄入。正数=亏，负数=超。 */
  function deficit(tdee, netBurn, intakeKcal) {
    return Math.round(tdee + netBurn - intakeKcal);
  }

  /*
   * 力量单条记录的一次性计算，对应安卓 Figures.strengthCalc。
   * 注意两个容易搞混的量：
   * - weightKg 是「人的体重」，进 MET 公式的那个质量项；
   * - weightValue 是「器械重量」，只用来按 1RM 比例粗略抬高强度（每 100kg +25%）。
   * 把器械重量当体重传进去，45kg 卧推会算成 19 kcal 这种离谱的低值。
   */
  function strengthCalc(exerciseMet, weightKg, weightValue, reps, sets, bodyweight, bmrValue, restSec) {
    var rest = restSec == null ? 90 : restSec;
    var minutes = strengthMinutes(sets, reps, rest, 3);
    var effectiveMet = bodyweight
      ? exerciseMet
      : exerciseMet * (1 + (weightValue / 100) * 0.25);
    var gross = grossKcal(effectiveMet, weightKg, minutes);
    return {
      minutes: minutes,
      met: effectiveMet,
      gross: gross,
      net: netKcal(gross, bmrValue, minutes)
    };
  }

  /* 目标缺口 = 每周减重 × 7700 / 7。例：0.5kg/周 → 550 kcal/天。 */
  function targetDeficit(weeklyRateKg) {
    return Math.round(weeklyRateKg * KCAL_PER_KG_FAT / 7);
  }

  /*
   * 日历评级只认饮食记录。
   * 只记训练没记饮食的日子 → TRAINED_ONLY，数字灰色、无描边、不出缺口值。
   */
  function calendarStatus(isFuture, hasFoodRecord, hasWorkout, deficitValue) {
    if (isFuture) return 'FUTURE';
    if (!hasFoodRecord) return hasWorkout ? 'TRAINED_ONLY' : 'EMPTY';
    if (Math.abs(deficitValue) <= 50) return 'EVEN';
    return deficitValue > 0 ? 'DEFICIT' : 'SURPLUS';
  }

  /* 关闭「热量缺口警示」后的中性状态：中间三档合并。 */
  function plainStatus(isFuture, hasFoodRecord, hasWorkout) {
    if (isFuture) return 'FUTURE';
    if (!hasFoodRecord) return hasWorkout ? 'TRAINED_ONLY' : 'EMPTY';
    return 'PLAIN';
  }

  /* 最小二乘斜率。x = 天数，y = 体重。 */
  function linearSlope(points) {
    var n = points.length;
    if (n < 2) return 0;
    var meanX = 0, meanY = 0, i;
    for (i = 0; i < n; i++) { meanX += points[i].day; meanY += points[i].kg; }
    meanX /= n; meanY /= n;
    var num = 0, den = 0;
    for (i = 0; i < n; i++) {
      var dx = points[i].day - meanX;
      num += dx * (points[i].kg - meanY);
      den += dx * dx;
    }
    return den === 0 ? 0 : num / den;
  }

  /*
   * 用「摄入 − 体重变化」反推真实消耗。
   * 窗口 30 天；体重记录 ≥ 8 天；饮食完整度 ≥ 60%。
   * 斜率用线性回归而不是首尾两点，首尾两点对单日水肿极其敏感。
   * 平滑：0.6 × 上次 + 0.4 × 新值。
   */
  function adaptiveTdee(points, intakeByDay, previousEstimate, windowDays) {
    var win = windowDays == null ? 30 : windowDays;
    if (points.length < MIN_WEIGHT_POINTS) {
      return { value: null, reason: '体重记录不足 ' + MIN_WEIGHT_POINTS + ' 天（当前 ' + points.length + ' 天）', sampleDays: 0 };
    }
    var sorted = points.slice().sort(function (a, b) { return a.day - b.day; });
    var span = sorted[sorted.length - 1].day - sorted[0].day + 1;
    if (span > win) {
      var lastDay = sorted[sorted.length - 1].day;
      return adaptiveTdee(sorted.filter(function (p) { return p.day > lastDay - win; }), intakeByDay, previousEstimate, win);
    }
    if (span < MIN_SPAN_DAYS) {
      return { value: null, reason: '时间跨度不足 ' + MIN_SPAN_DAYS + ' 天（当前 ' + span + ' 天）', sampleDays: span };
    }

    var firstDay = sorted[0].day, last = sorted[sorted.length - 1].day;
    var observed = [];
    for (var day in intakeByDay) {
      if (!Object.prototype.hasOwnProperty.call(intakeByDay, day)) continue;
      var d = parseInt(day, 10);
      if (d >= firstDay && d <= last) observed.push(intakeByDay[day]);
    }
    var requiredDays = Math.floor(span * MIN_INTAKE_COVERAGE);
    if (observed.length < requiredDays) {
      return {
        value: null,
        reason: '饮食记录完整度不足（' + span + ' 天里只有 ' + observed.length + ' 天，需 ' + requiredDays + ' 天）',
        sampleDays: span
      };
    }

    var sum = 0;
    for (var k = 0; k < observed.length; k++) sum += observed[k];
    var avgIntake = sum / observed.length;
    var slope = linearSlope(sorted);
    var raw = avgIntake - slope * KCAL_PER_KG_FAT;
    var smoothed = previousEstimate == null ? raw : 0.6 * previousEstimate + 0.4 * raw;

    return {
      value: smoothed,
      reason: null,
      sampleDays: span,
      avgIntake: avgIntake,
      slopeKgPerDay: slope,
      rawValue: raw
    };
  }

  /* 自适应值与公式值差异过大时取中位数，避免一条水肿数据把整月日历染红。 */
  function reconcileTdee(adaptive, formula) {
    var diff = adaptive - formula;
    if (Math.abs(diff) > ANOMALY_THRESHOLD) {
      return { value: (adaptive + formula) / 2, anomaly: true, note: '数据波动较大，已取两者中位数' };
    }
    return { value: adaptive, anomaly: false, note: null };
  }

  function proteinTarget(weightKg, auto, customG) {
    return auto ? Math.round(weightKg * PROTEIN_AUTO_FACTOR) : customG;
  }

  function proteinPreset(weightKg, gPerKg) {
    return Math.round(weightKg * gPerKg);
  }

  /* 本周平均缺口，只统计已评级（有饮食记录）的日期。 */
  function averageDeficit(list) {
    if (!list.length) return null;
    var s = 0;
    for (var i = 0; i < list.length; i++) s += list[i];
    return Math.round(s / list.length);
  }

  /* 本周达标天数：缺口在目标 ±100 内。 */
  function targetHitDays(list, target) {
    var n = 0;
    for (var i = 0; i < list.length; i++) if (Math.abs(list[i] - target) <= 100) n++;
    return n;
  }

  global.M = {
    KCAL_PER_KG_FAT: KCAL_PER_KG_FAT,
    PROTEIN_AUTO_FACTOR: PROTEIN_AUTO_FACTOR,
    MIN_WEIGHT_POINTS: MIN_WEIGHT_POINTS,
    bmr: bmr,
    tdeeBase: tdeeBase,
    strengthMinutes: strengthMinutes,
    grossKcal: grossKcal,
    netKcal: netKcal,
    deficit: deficit,
    strengthCalc: strengthCalc,
    targetDeficit: targetDeficit,
    calendarStatus: calendarStatus,
    plainStatus: plainStatus,
    linearSlope: linearSlope,
    adaptiveTdee: adaptiveTdee,
    reconcileTdee: reconcileTdee,
    proteinTarget: proteinTarget,
    proteinPreset: proteinPreset,
    averageDeficit: averageDeficit,
    targetHitDays: targetHitDays
  };
})(window);
