/* 校验 web 版计算层与安卓单元测试的期望值是否一致 */
global.window = global;
require('../web/js/math.js');
require('../web/js/seed.js');

var M = global.M, SEED = global.SEED;

function eq(name, got, want, tol) {
  var ok = Math.abs(got - want) <= (tol == null ? 0.6 : tol);
  console.log((ok ? 'PASS ' : 'FAIL ') + name + ' = ' + (Math.round(got * 100) / 100) + (ok ? '' : '  期望 ' + want));
}

// 用户实际资料：男 27 岁 181cm 76.5kg 久坐
var bmr = M.bmr('MALE', 76.5, 181, 27);
eq('BMR(男 76.5/181/27)', bmr, 1766.25, 0.1);
eq('TDEE 久坐', M.tdeeBase(bmr, 1.2), 2119.5, 0.1);

// 力量时长：4 组 12 次，组间 90 秒，每下 3 秒 → 414 秒
var min = M.strengthMinutes(4, 12, 90, 3);
eq('卧推时长(分钟)', min, 6.9, 0.01);

// 卧推 MET 5.0
var ex = SEED.exerciseById('bench_press');
var gross = M.grossKcal(ex.met, 76.5, min);
eq('卧推毛消耗', gross, 46.19, 0.1);
eq('卧推净消耗', M.netKcal(gross, bmr, min), 37.73, 0.2);

// 肩推 MET 4.5，3 组 10 次 → 270 秒 = 4.5 分钟
var min2 = M.strengthMinutes(3, 10, 90, 3);
eq('肩推时长', min2, 4.5, 0.01);
var g2 = M.grossKcal(4.5, 76.5, min2);
eq('肩推毛消耗', g2, 27.06, 0.1);
eq('肩推净消耗', M.netKcal(g2, bmr, min2), 21.54, 0.2);

// 有氧：爬坡走 10% / 4.5km/h
var met = SEED.cardioById('incline_walk').met({ incline: 10, speed: 4.5, min: 35 }, null);
eq('爬坡走 MET', met, 3.2 + 10 * 0.42 + 4.5 * 0.3, 0.01);
var g3 = M.grossKcal(met, 76.5, 35);
eq('爬坡走毛消耗', g3, 409.99, 1);
eq('爬坡走净消耗', M.netKcal(g3, bmr, 35), 367.06, 1);

eq('目标缺口 0.5kg/周', M.targetDeficit(0.5), 550, 0.1);
eq('蛋白目标 76.5×1.8', M.proteinTarget(76.5, true, 0), 138, 0.6);
