/* 内置数据，从 SeedData.kt 移植：7 部位 / 47 器械 / 10 有氧 / 29 食物 / 4 档活动系数。 */
(function (global) {
  'use strict';

  var muscleGroups = [
    { id: 'chest', name: '胸' },
    { id: 'back', name: '背' },
    { id: 'legs', name: '腿' },
    { id: 'glute', name: '臀' },
    { id: 'shoulder', name: '肩' },
    { id: 'arm', name: '手臂' },
    { id: 'core', name: '核心' }
  ];

  /* [id, 名称, MET, 默认自重, 部位[]] */
  var EX = [
    ['bench_press', '杠铃卧推', 5.0, false, ['chest']],
    ['incline_press', '上斜哑铃推', 5.0, false, ['chest']],
    ['decline_press', '下斜卧推', 5.0, false, ['chest']],
    ['pec_deck', '蝴蝶机夹胸', 3.5, false, ['chest']],
    ['cable_crossover', '龙门架夹胸', 3.5, false, ['chest']],
    ['fly_db', '哑铃飞鸟', 3.5, false, ['chest']],
    ['machine_press', '坐姿推胸机', 4.5, false, ['chest']],
    ['dip', '双杠臂屈伸', 5.0, true, ['chest', 'arm']],
    ['lat_pulldown', '高位下拉', 4.5, false, ['back']],
    ['seated_row', '坐姿划船', 4.5, false, ['back']],
    ['barbell_row', '杠铃划船', 5.0, false, ['back']],
    ['tbar_row', 'T杠划船', 5.0, false, ['back']],
    ['db_row', '单臂哑铃划船', 4.5, false, ['back']],
    ['straight_arm', '直臂下压', 3.5, false, ['back']],
    ['pullup', '引体向上', 5.0, true, ['back']],
    ['deadlift', '硬拉', 6.0, false, ['back', 'legs', 'glute']],
    ['back_extension', '山羊挺身', 3.5, true, ['back', 'core', 'glute']],
    ['squat', '杠铃深蹲', 6.0, false, ['legs', 'glute']],
    ['leg_press', '腿举', 5.0, false, ['legs']],
    ['hack_squat', '哈克深蹲', 5.5, false, ['legs', 'glute']],
    ['split_squat', '保加利亚分腿蹲', 5.0, false, ['legs', 'glute']],
    ['leg_extension', '坐姿腿屈伸', 3.5, false, ['legs']],
    ['leg_curl', '俯卧腿弯举', 3.5, false, ['legs']],
    ['calf_raise', '站姿提踵', 3.5, false, ['legs']],
    ['seated_calf', '坐姿提踵', 3.5, false, ['legs']],
    ['rdl', '罗马尼亚硬拉', 5.5, false, ['legs', 'glute']],
    ['hip_thrust', '杠铃臀推', 4.5, false, ['glute']],
    ['hip_abduction', '坐姿髋外展', 3.5, false, ['glute']],
    ['hip_extension', '臀桥机', 3.5, false, ['glute']],
    ['cable_kickback', '绳索后踢腿', 3.5, false, ['glute']],
    ['shoulder_press', '哑铃肩推', 4.5, false, ['shoulder']],
    ['lateral_raise', '哑铃侧平举', 3.5, false, ['shoulder']],
    ['front_raise', '哑铃前平举', 3.5, false, ['shoulder']],
    ['reverse_fly', '反向飞鸟', 3.5, false, ['shoulder', 'back']],
    ['upright_row', '直立划船', 4.0, false, ['shoulder']],
    ['face_pull', '绳索面拉', 3.5, false, ['shoulder', 'back']],
    ['barbell_curl', '杠铃弯举', 3.5, false, ['arm']],
    ['hammer_curl', '锤式弯举', 3.5, false, ['arm']],
    ['preacher_curl', '牧师凳弯举', 3.5, false, ['arm']],
    ['tricep_pushdown', '绳索下压', 3.5, false, ['arm']],
    ['skull_crusher', '仰卧臂屈伸', 3.5, false, ['arm']],
    ['close_grip', '窄距卧推', 4.5, false, ['arm', 'chest']],
    ['overhead_ext', '哑铃颈后臂屈伸', 3.5, false, ['arm']],
    ['crunch', '卷腹', 3.5, true, ['core']],
    ['hanging_leg', '悬垂举腿', 3.5, true, ['core']],
    ['plank', '平板支撑', 3.5, true, ['core']],
    ['cable_crunch', '绳索卷腹', 3.5, false, ['core']]
  ];

  var exercises = EX.map(function (e) {
    return { id: e[0], name: e[1], met: e[2], bodyweightDefault: e[3], groups: e[4] };
  });

  /*
   * 有氧项目。MET 由参数实时算出，不是固定值 ——
   * 同样 30 分钟，坡度 10% 和坡度 0 的爬坡走差将近一倍。
   */
  var cardioTypes = [
    {
      id: 'treadmill', name: '跑步机',
      params: [
        { key: 'speed', label: '速度', unit: 'km/h', min: 3, max: 15, step: 0.5, def: 6 },
        { key: 'incline', label: '坡度', unit: '%', min: 0, max: 15, step: 1, def: 0 },
        { key: 'min', label: '时间', unit: '分钟', min: 5, max: 90, step: 5, def: 30 }
      ],
      met: function (p) { return 3.0 + (p.speed || 0) * 0.72 + (p.incline || 0) * 0.28; }
    },
    {
      id: 'outdoor_run', name: '户外跑',
      params: [
        { key: 'dist', label: '距离', unit: 'km', min: 0.5, max: 21, step: 0.5, def: 5 },
        { key: 'min', label: '时间', unit: '分钟', min: 5, max: 180, step: 5, def: 32 }
      ],
      met: function (p) {
        var speed = (p.dist || 0) / Math.max(1, p.min || 1) * 60;
        return 3.5 + speed * 0.62;
      }
    },
    {
      id: 'incline_walk', name: '爬坡走',
      params: [
        { key: 'incline', label: '坡度', unit: '%', min: 1, max: 20, step: 1, def: 10 },
        { key: 'speed', label: '速度', unit: 'km/h', min: 2, max: 8, step: 0.5, def: 4.5 },
        { key: 'min', label: '时间', unit: '分钟', min: 5, max: 90, step: 5, def: 35 }
      ],
      met: function (p) { return 3.2 + (p.incline || 0) * 0.42 + (p.speed || 0) * 0.3; }
    },
    {
      id: 'elliptical', name: '椭圆机',
      params: [
        { key: 'res', label: '阻力', unit: '档', min: 1, max: 20, step: 1, def: 8 },
        { key: 'min', label: '时间', unit: '分钟', min: 5, max: 90, step: 5, def: 30 }
      ],
      met: function (p) { return 4.2 + (p.res || 0) * 0.18; }
    },
    {
      id: 'spin_bike', name: '动感单车',
      params: [
        { key: 'res', label: '阻力', unit: '档', min: 1, max: 24, step: 1, def: 10 },
        { key: 'min', label: '时间', unit: '分钟', min: 5, max: 90, step: 5, def: 35 },
        { key: 'dist', label: '距离', unit: 'km', min: 0, max: 40, step: 1, def: 12 }
      ],
      met: function (p) { return 4.5 + (p.res || 0) * 0.2; }
    },
    {
      id: 'rower', name: '划船机',
      params: [
        { key: 'dist', label: '距离', unit: 'km', min: 0.5, max: 20, step: 0.5, def: 5 },
        { key: 'min', label: '时间', unit: '分钟', min: 3, max: 90, step: 1, def: 25 },
        { key: 'spm', label: '桨频', unit: 'spm', min: 16, max: 36, step: 1, def: 24 }
      ],
      met: function (p) { return 5.5 + (p.spm || 0) * 0.06; }
    },
    {
      id: 'jump_rope', name: '跳绳',
      params: [
        { key: 'cnt', label: '个数', unit: '个', min: 100, max: 5000, step: 100, def: 1500 },
        { key: 'min', label: '时间', unit: '分钟', min: 1, max: 60, step: 1, def: 15 }
      ],
      met: function () { return 11.0; }
    },
    {
      id: 'swim', name: '游泳',
      params: [
        { key: 'dist', label: '距离', unit: '米', min: 100, max: 3000, step: 100, def: 1000 },
        { key: 'min', label: '时间', unit: '分钟', min: 5, max: 90, step: 5, def: 30 }
      ],
      enumSpec: { key: 'style', label: '泳姿', options: ['自由泳', '蛙泳', '仰泳', '蝶泳'], def: '自由泳' },
      met: function (p, style) {
        if (style === '蛙泳') return 7.5;
        if (style === '仰泳') return 6.8;
        if (style === '蝶泳') return 9.8;
        return 8.3;
      }
    },
    {
      id: 'stairs', name: '爬楼梯',
      params: [
        { key: 'floor', label: '层数', unit: '层', min: 5, max: 200, step: 5, def: 40 },
        { key: 'min', label: '时间', unit: '分钟', min: 3, max: 90, step: 1, def: 20 }
      ],
      met: function () { return 8.8; }
    },
    {
      id: 'hiit', name: '操课 / HIIT',
      params: [
        { key: 'min', label: '时间', unit: '分钟', min: 5, max: 120, step: 5, def: 30 }
      ],
      enumSpec: { key: 'level', label: '强度', options: ['低', '中', '高'], def: '中' },
      met: function (p, level) {
        if (level === '高') return 11.0;
        if (level === '低') return 5.5;
        return 8.0;
      }
    }
  ];

  /* [id, 名称, 别名, kcal/100g, 蛋白/100g, 单位, 单份克数, emoji] */
  var FD = [
    ['rice', '米饭', 'mifan 白米饭', 116, 2.6, '碗', 150, '🍚'],
    ['chicken', '鸡胸肉', 'jixiong', 133, 24.6, '块', 120, '🍗'],
    ['egg', '鸡蛋', 'jidan', 144, 13.3, '个', 50, '🥚'],
    ['bread', '全麦面包', 'mianbao', 246, 9.0, '片', 35, '🍞'],
    ['milk', '牛奶', 'niunai', 54, 3.0, '盒', 250, '🥛'],
    ['oats', '燕麦', 'yanmai', 377, 13.0, '份', 40, '🥣'],
    ['banana', '香蕉', 'xiangjiao', 93, 1.4, '根', 120, '🍌'],
    ['apple', '苹果', 'pingguo', 53, 0.2, '个', 200, '🍎'],
    ['broccoli', '西兰花', 'xilanhua', 34, 2.8, '份', 150, '🥦'],
    ['beef', '瘦牛肉', 'niurou', 106, 20.2, '份', 120, '🥩'],
    ['salmon', '三文鱼', 'sanwenyu', 139, 17.2, '份', 100, '🐟'],
    ['tofu', '豆腐', 'doufu', 82, 8.1, '块', 100, '🍲'],
    ['tomato_egg', '番茄炒蛋', 'fanqiechaodan', 86, 5.4, '份', 200, '🍅'],
    ['pork', '红烧肉', 'hongshaorou', 470, 13.0, '份', 100, '🥓'],
    ['latte', '拿铁', 'natie coffee', 55, 3.0, '杯', 350, '☕'],
    ['cola', '可乐', 'kele', 43, 0, '罐', 330, '🥤'],
    ['whey', '蛋白粉', 'danbaifen', 400, 78.0, '勺', 30, '💪'],
    ['sweetpotato', '红薯', 'hongshu', 86, 1.6, '个', 150, '🍠'],
    ['corn', '玉米', 'yumi', 112, 4.0, '根', 150, '🌽'],
    ['salad', '蔬菜沙拉', 'shala', 70, 2.0, '份', 200, '🥗'],
    ['noodle', '面条', 'miantiao', 137, 4.5, '碗', 200, '🍜'],
    ['baozi', '包子', 'baozi', 227, 7.0, '个', 80, '🥟'],
    ['youtiao', '油条', 'youtiao', 388, 6.9, '根', 60, '🥖'],
    ['fried_chicken', '炸鸡', 'zhaji', 280, 18.0, '块', 100, '🍗'],
    ['beer', '啤酒', 'pijiu', 43, 0.4, '罐', 330, '🍺'],
    ['yogurt', '酸奶', 'suannai', 72, 3.2, '杯', 150, '🥛'],
    ['americano', '美式咖啡', 'meishi', 2, 0.1, '杯', 350, '☕'],
    ['choco', '黑巧克力', 'qiaokeli', 546, 7.8, '块', 20, '🍫'],
    ['avocado', '牛油果', 'niuyouguo', 160, 2.0, '个', 150, '🥑']
  ];

  var foods = FD.map(function (f) {
    return {
      id: f[0], name: f[1], alias: f[2], kcalPer100g: f[3], proteinPer100g: f[4],
      servingUnit: f[5], servingGrams: f[6], emoji: f[7], source: 'BUILTIN'
    };
  });

  var activityLevels = [
    { factor: 1.2, label: '久坐', desc: '办公室为主，几乎不额外走动' },
    { factor: 1.375, label: '轻度', desc: '每周 1–2 次轻活动 / 常走动' },
    { factor: 1.55, label: '中度', desc: '每周 3–4 次训练 / 站立工作' },
    { factor: 1.725, label: '高度', desc: '体力工作 / 每周 5 次以上训练' }
  ];

  global.SEED = {
    muscleGroups: muscleGroups,
    exercises: exercises,
    cardioTypes: cardioTypes,
    foods: foods,
    activityLevels: activityLevels,
    exerciseById: function (id) {
      for (var i = 0; i < exercises.length; i++) if (exercises[i].id === id) return exercises[i];
      return null;
    },
    cardioById: function (id) {
      for (var i = 0; i < cardioTypes.length; i++) if (cardioTypes[i].id === id) return cardioTypes[i];
      return null;
    },
    foodById: function (id) {
      for (var i = 0; i < foods.length; i++) if (foods[i].id === id) return foods[i];
      return null;
    }
  };
})(window);
