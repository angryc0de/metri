/*
 * 数据层：localStorage 持久化 + 按天聚合 + 导出导入。
 * 日期统一用 epochDay（1970-01-01 为 0），与安卓版的 Room 表口径完全一致，
 * 这样以后两边数据可以互相导入。
 */
(function (global) {
  'use strict';

  var KEY = 'metri.v1';
  var DAY_MS = 86400000;

  function defaultState() {
    return {
      version: 1,
      onboarded: false,
      darkTheme: true,
      heatWarn: true,
      profile: {
        name: '',
        gender: 'MALE',
        age: 28,
        heightCm: 178,
        weightKg: 78,
        activityFactor: 1.2,
        targetWeightKg: 72,
        weeklyRateKg: 0.5,
        proteinAuto: true,
        proteinCustomG: 140
      },
      weightLog: {},
      strength: [],
      cardio: [],
      meals: [],
      restDays: [],
      seq: 1
    };
  }

  var state = null;

  function load() {
    if (state) return state;
    var raw = null;
    try { raw = localStorage.getItem(KEY); } catch (e) { /* 隐私模式 */ }
    if (!raw) { state = defaultState(); return state; }
    try {
      var parsed = JSON.parse(raw);
      state = Object.assign(defaultState(), parsed);
      state.profile = Object.assign(defaultState().profile, parsed.profile || {});
      if (!Array.isArray(state.strength)) state.strength = [];
      if (!Array.isArray(state.cardio)) state.cardio = [];
      if (!Array.isArray(state.meals)) state.meals = [];
      if (!Array.isArray(state.restDays)) state.restDays = [];
    } catch (e) {
      state = defaultState();
    }
    return state;
  }

  /** 丢弃内存缓存、从 localStorage 重读。demo 数据直接写盘后必须调它，否则 save 会把空库写回去。 */
  function reload() {
    state = null;
    return load();
  }

  function save() {
    try { localStorage.setItem(KEY, JSON.stringify(state)); } catch (e) {
      console.warn('保存失败（可能是存储空间不足或隐私模式）', e);
    }
  }

  function nextId() {
    state.seq = (state.seq || 1) + 1;
    return state.seq;
  }

  // ---------------------------------------------------------------- 日期

  function toEpochDay(date) {
    return Math.floor(Date.UTC(date.getFullYear(), date.getMonth(), date.getDate()) / DAY_MS);
  }

  function today() {
    return toEpochDay(new Date());
  }

  function fromEpochDay(day) {
    var d = new Date(day * DAY_MS);
    return {
      y: d.getUTCFullYear(),
      m: d.getUTCMonth() + 1,
      d: d.getUTCDate(),
      dow: d.getUTCDay() // 0=周日
    };
  }

  function ymd(day) {
    var o = fromEpochDay(day);
    return o.y + '-' + (o.m < 10 ? '0' : '') + o.m + '-' + (o.d < 10 ? '0' : '') + o.d;
  }

  var WEEK = ['周日', '周一', '周二', '周三', '周四', '周五', '周六'];

  // ---------------------------------------------------------------- 写入

  function setProfile(patch) {
    Object.assign(state.profile, patch);
    save();
  }

  function setHeatWarn(v) {
    state.heatWarn = !!v;
    save();
  }

  function setWeight(day, kg) {
    state.weightLog[day] = kg;
    var t = today();
    if (day === t) state.profile.weightKg = kg;
    save();
  }

  function addStrength(rec) {
    rec.id = nextId();
    rec.createdAt = Date.now();
    state.strength.push(rec);
    save();
    return rec;
  }

  function addCardio(rec) {
    rec.id = nextId();
    rec.createdAt = Date.now();
    state.cardio.push(rec);
    save();
    return rec;
  }

  function addMeal(rec) {
    rec.id = nextId();
    rec.createdAt = Date.now();
    state.meals.push(rec);
    save();
    return rec;
  }

  function removeRecord(kind, id) {
    var list = kind === 'strength' ? state.strength : kind === 'cardio' ? state.cardio : state.meals;
    for (var i = 0; i < list.length; i++) {
      if (list[i].id === id) { list.splice(i, 1); break; }
    }
    save();
  }

  function toggleRest(day) {
    var i = state.restDays.indexOf(day);
    if (i >= 0) state.restDays.splice(i, 1); else state.restDays.push(day);
    save();
  }

  function isRest(day) {
    return state.restDays.indexOf(day) >= 0;
  }

  // ---------------------------------------------------------------- 聚合

  function intakeOfDay(day) {
    var kcal = 0, protein = 0, n = 0;
    for (var i = 0; i < state.meals.length; i++) {
      var m = state.meals[i];
      if (m.day !== day) continue;
      kcal += m.kcal;
      protein += m.protein;
      n++;
    }
    return { kcal: kcal, protein: protein, entries: n };
  }

  function burnOfDay(day) {
    var net = 0, n = 0, hasStrength = false, hasCardio = false;
    for (var i = 0; i < state.strength.length; i++) {
      if (state.strength[i].day === day) { net += state.strength[i].netKcal; n++; hasStrength = true; }
    }
    for (var j = 0; j < state.cardio.length; j++) {
      if (state.cardio[j].day === day) { net += state.cardio[j].netKcal; n++; hasCardio = true; }
    }
    return { net: net, entries: n, hasStrength: hasStrength, hasCardio: hasCardio };
  }

  /*
   * 当前使用的 TDEE：公式值 vs 自适应值。
   * 自适应只在样本够了才有值，异常时取中位数，与安卓版口径一致。
   */
  function effectiveTdee() {
    var p = state.profile;
    var bmrValue = global.M.bmr(p.gender, p.weightKg, p.heightCm, p.age);
    var formula = global.M.tdeeBase(bmrValue, p.activityFactor);

    var points = [];
    for (var day in state.weightLog) {
      if (!Object.prototype.hasOwnProperty.call(state.weightLog, day)) continue;
      points.push({ day: parseInt(day, 10), kg: state.weightLog[day] });
    }
    var intakeByDay = {};
    for (var i = 0; i < state.meals.length; i++) {
      var m = state.meals[i];
      intakeByDay[m.day] = (intakeByDay[m.day] || 0) + m.kcal;
    }

    var ad = global.M.adaptiveTdee(points, intakeByDay, p.adaptiveTdee || null);
    if (ad.value == null) {
      return { bmr: bmrValue, formula: formula, value: formula, adaptive: null, note: ad.reason };
    }
    var rec = global.M.reconcileTdee(ad.value, formula);
    return {
      bmr: bmrValue,
      formula: formula,
      value: rec.value,
      adaptive: ad.value,
      anomaly: rec.anomaly,
      note: rec.note || (T('样本 ') + ad.sampleDays + T(' 天 · 日均摄入 ') + Math.round(ad.avgIntake) + ' kcal')
    };
  }

  /* 某一天的全部数字，日历和详情卡都从这里取。 */
  function dayDetail(day) {
    var p = state.profile;
    var t = today();
    var tdee = effectiveTdee();
    var intake = intakeOfDay(day);
    var burn = burnOfDay(day);
    var def = global.M.deficit(tdee.value, burn.net, intake.kcal);
    var isFuture = day > t;
    var hasFood = intake.entries > 0;
    var hasWorkout = burn.entries > 0;

    var status = state.heatWarn
      ? global.M.calendarStatus(isFuture, hasFood, hasWorkout, def)
      : global.M.plainStatus(isFuture, hasFood, hasWorkout);

    if (!hasFood && !hasWorkout) def = null; // 什么都没记的日子不冒充缺口

    return {
      day: day,
      tdee: tdee,
      intakeKcal: intake.kcal,
      intakeProtein: intake.protein,
      mealCount: intake.entries,
      burnNet: burn.net,
      hasStrength: burn.hasStrength,
      hasCardio: burn.hasCardio,
      workoutCount: burn.entries,
      hasFood: hasFood,
      hasWorkout: hasWorkout,
      rateHint: null,
      deficit: hasFood ? global.M.deficit(tdee.value, burn.net, intake.kcal) : null,
      status: status,
      isFuture: isFuture,
      isRest: isRest(day)
    };
  }

  /* 周汇总：只统计有饮食记录的日子。 */
  function weekSummary(dayOfWeekMondayFirst) {
    var t = today();
    var monday = t - ((fromEpochDay(t).dow + 6) % 7);
    var list = [], hit = 0;
    var target = global.M.targetDeficit(state.profile.weeklyRateKg);
    for (var i = 0; i < 7; i++) {
      var d = monday + i;
      if (d > t) break;
      var det = dayDetail(d);
      if (det.hasFood) list.push(det.deficit);
    }
    if (!list.length) return null;
    return {
      avg: global.M.averageDeficit(list),
      hit: global.M.targetHitDays(list, target),
      days: list.length,
      target: target
    };
  }

  // ---------------------------------------------------------------- 导出

  function exportJSON() {
    return JSON.stringify(state, null, 2);
  }

  function importJSON(text) {
    var parsed = JSON.parse(text);
    if (!parsed || typeof parsed !== 'object' || !parsed.profile) {
      throw new Error('不是 Metri 的备份文件');
    }
    state = Object.assign(defaultState(), parsed);
    state.profile = Object.assign(defaultState().profile, parsed.profile || {});
    save();
  }

  function csvEscape(v) {
    var s = v == null ? '' : String(v);
    return /[",\n]/.test(s) ? '"' + s.replace(/"/g, '""') + '"' : s;
  }

  function toCSV(rows) {
    return rows.map(function (r) { return r.map(csvEscape).join(','); }).join('\r\n');
  }

  function exportCSVs() {
    var daily = [['日期', '基础消耗', '训练净消耗', '摄入', '缺口', '蛋白质g', '记录数']];
    var days = {};
    state.meals.forEach(function (m) { days[m.day] = 1; });
    state.strength.forEach(function (s) { days[s.day] = 1; });
    state.cardio.forEach(function (c) { days[c.day] = 1; });
    Object.keys(days).map(Number).sort(function (a, b) { return a - b; }).forEach(function (d) {
      var det = dayDetail(d);
      daily.push([ymd(d), Math.round(det.tdee.value), Math.round(det.burnNet),
        Math.round(det.intakeKcal), det.deficit == null ? '' : det.deficit,
        Math.round(det.intakeProtein), det.mealCount + det.workoutCount]);
    });

    var strength = [['日期', '器械', '重量kg', '次数', '组数', '自重', '时长min', '净消耗']];
    state.strength.slice().sort(function (a, b) { return a.day - b.day; }).forEach(function (s) {
      strength.push([ymd(s.day), s.exerciseName, s.weightKg, s.reps, s.sets,
        s.bodyweight ? '是' : '否', s.minutes.toFixed(1), Math.round(s.netKcal)]);
    });

    var cardio = [['日期', '项目', '参数', '时长min', 'MET', '净消耗']];
    state.cardio.slice().sort(function (a, b) { return a.day - b.day; }).forEach(function (c) {
      var parts = [];
      for (var k in c.params) if (Object.prototype.hasOwnProperty.call(c.params, k)) parts.push(k + '=' + c.params[k]);
      if (c.enumValue) parts.push(c.enumValue);
      cardio.push([ymd(c.day), c.typeName, parts.join(' '), c.minutes.toFixed(1), c.met.toFixed(2), Math.round(c.netKcal)]);
    });

    var meals = [['日期', '餐次', '食物', '克数', '份数', '热量', '蛋白质g']];
    state.meals.slice().sort(function (a, b) { return a.day - b.day; }).forEach(function (m) {
      meals.push([ymd(m.day), mealLabel(m.meal), m.foodName, m.grams, m.servings || '', Math.round(m.kcal), m.protein.toFixed(1)]);
    });

    var weight = [['日期', '体重kg']];
    Object.keys(state.weightLog).map(Number).sort(function (a, b) { return a - b; }).forEach(function (d) {
      weight.push([ymd(d), state.weightLog[d]]);
    });

    return {
      'daily.csv': toCSV(daily),
      'strength.csv': toCSV(strength),
      'cardio.csv': toCSV(cardio),
      'meals.csv': toCSV(meals),
      'weight.csv': toCSV(weight)
    };
  }

  function mealLabel(key) {
    return T({ BREAKFAST: '早餐', LUNCH: '午餐', DINNER: '晚餐', SNACK: '其他' }[key] || key);
  }

  function reset() {
    state = defaultState();
    save();
  }

  global.DB = {
    KEY: KEY,
    load: load,
    reload: reload,
    save: save,
    get: function () { return load(); },
    toEpochDay: toEpochDay,
    today: today,
    ymd: ymd,
    fromEpochDay: fromEpochDay,
    WEEK: WEEK,
    setProfile: setProfile,
    setHeatWarn: setHeatWarn,
    setWeight: setWeight,
    addStrength: addStrength,
    addCardio: addCardio,
    addMeal: addMeal,
    removeRecord: removeRecord,
    toggleRest: toggleRest,
    isRest: isRest,
    intakeOfDay: intakeOfDay,
    burnOfDay: burnOfDay,
    effectiveTdee: effectiveTdee,
    dayDetail: dayDetail,
    weekSummary: weekSummary,
    exportJSON: exportJSON,
    importJSON: importJSON,
    exportCSVs: exportCSVs,
    mealLabel: mealLabel,
    reset: reset
  };
})(window);
