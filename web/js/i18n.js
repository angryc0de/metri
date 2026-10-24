/*
 * 轻量 i18n：中文原文即 key，系统语言非中文时查英文词典，查不到兜底显示中文。
 * 语言来源：?lang=en|zh 强制指定 > 浏览器 navigator.language（zh* 视为中文）。
 * 不设手动开关、不做持久化 —— 换系统语言重开页面即变。
 */
(function (g) {
  'use strict';

  var m = /[?&]lang=(en|zh)/.exec(location.search);
  var nav = (navigator.language || 'zh-CN').toLowerCase();
  var lang = m ? m[1] : (nav.indexOf('zh') === 0 ? 'zh' : 'en');

  var EN = {
    // ---- 通用词 / 单位（按整串精确匹配，不怕歧义）----
    '力量': 'Strength', '有氧': 'Cardio', '休': 'R', '训练': 'Train', '饮食': 'Diet', '我': 'Me',
    '男': 'Male', '女': 'Female', '开': 'On', '关': 'Off', '已开': 'On', '开启': 'Turn on',
    '次': 'reps', '组': 'sets', '分钟': 'min', '岁': 'yrs', '项': 'items', '份': 'serv',
    '碗': 'bowl', '块': 'pc', '个': 'pc', '片': 'slice', '盒': 'carton', '根': 'pc',
    '杯': 'cup', '勺': 'scoop', '罐': 'can', '档': 'lvl', '米': 'm', '层': 'fl',
    '时间': 'Time', '速度': 'Speed', '坡度': 'Incline', '距离': 'Distance', '阻力': 'Resistance',
    '桨频': 'Stroke', '个数': 'Count', '层数': 'Floors', '泳姿': 'Stroke', '强度': 'Intensity',
    '低': 'Low', '中': 'Mid', '高': 'High',
    '自重': 'Bodyweight', '净消耗': 'Net', '克数': 'Grams',
    '热量': 'Energy', '蛋白': 'Protein', '碳水': 'Carbs', '脂肪': 'Fat',
    '蛋白质': 'Protein', '热量缺口': 'Deficit', '热量超出': 'Over by',
    '基础代谢': 'BMR', '日消耗': 'Daily burn', '目标缺口': 'Target deficit',
    '今天': 'Today', '昨天': 'Yesterday', '今天 · ': 'Today · ',
    '使用中': 'In use', '点按切换': 'Tap to switch', '黑夜': 'Dark', '白天': 'Light',

    // ---- 顶栏 / 日历 ----
    '数字颜色 = 热量缺口': 'Number color = calorie deficit',
    '本周 平均缺口 ': 'This week avg deficit ',
    '达标 ': 'On target: ', ' 天（目标 ': ' days (goal ', '）': ')',

    // ---- 训练页 ----
    '力量训练': 'Strength training', '还没有记录': 'Nothing yet',
    '还没有力量记录，点上面的 + 添加': 'No strength entries — tap + above to add',
    '还没有有氧记录，点上面的 + 添加': 'No cardio entries — tap + above to add',
    ' · 自重': ' · bodyweight', ' 分钟': ' min',
    '取消「休息日」标记': 'Unmark rest day', '标记今天为休息日': 'Mark today as rest day',

    // ---- 饮食页 ----
    '已摄入 ': 'Intake ', '目标蛋白': 'Protein goal', '已记录': 'Logged',
    '早餐': 'Breakfast', '午餐': 'Lunch', '晚餐': 'Dinner', '其他': 'Other',

    // ---- 我页 ----
    '你的资料': 'Your profile', '已启用自适应 TDEE': 'Adaptive TDEE on', '按公式计算': 'Formula only',
    '设置': 'Settings', '个人资料': 'Profile', '性别、身高、体重、目标': 'Gender, height, weight, goals',
    '外观': 'Appearance', '黑夜模式': 'Dark mode', '白天模式': 'Light mode',
    '热量缺口警示': 'Deficit warning', '关闭后日历不再标绿/红': 'Off hides green/red marks on calendar',
    '数据': 'Data', '导出、导入、清空': 'Export, import, reset',
    '关于': 'About',
    'Metri · 完全本地的减脂记录': 'Metri · fully local deficit tracker',
    '无账号、无服务器、无上传，数据只存在这台设备的浏览器里。': 'No account, no server, no upload — data lives in this browser only.',
    '蛋白质目标 ': 'Protein goal ', ' g/天 · 目标体重 ': ' g/day · target weight ', ' kg': ' kg',

    // ---- 引导页 ----
    '先了解一下你': 'First, a quick intro',
    '这些只用来算你的基础代谢和目标，全部留在这台设备上': 'Only used to compute your BMR and goals — it all stays on this device',
    '示例数据只在你主动点「先看示例」时才会载入': 'Demo data is only loaded when you tap "Show demo"',
    '怎么称呼你（可留空）': 'Your name (optional)', '例如 ivan': 'e.g. Alex',
    '性别': 'Gender', '年龄': 'Age', '身高 cm': 'Height cm', '体重 kg': 'Weight kg',
    '开始记录': 'Start logging', '不想填，先看示例数据': 'Skip — show demo data instead',
    '基础代谢 ': 'BMR ', ' kcal/天': ' kcal/day',
    '基础消耗 ': 'Resting burn ', ' · 自适应': ' · adaptive',
    '你好': 'Hi', ' 岁': ' yrs',
    '身高': 'Height', '体重': 'Weight',
    '按久坐估算的日消耗 ': 'Est. daily burn (sedentary) ',
    '训练消耗会单独加上，不重复计入': 'Workout burn is added separately — never double counted',

    // ---- 弹层：器械 / 力量 ----
    '最近练过': 'Recent', '没找到这个器械': 'No such exercise',
    '选择器械': 'Choose exercise', '47 个动作 · 按部位分组': '47 moves · grouped by muscle',
    '搜索器械名': 'Search exercises', '本次训练的参数': 'Session parameters',
    '重量': 'Weight', '次数': 'Reps', '组数': 'Sets',
    '预计 ': 'Est. ', '（每下 3 秒 + 组间休息 90 秒，末组不休息）': ' (3 s/rep + 90 s rest between sets, none after the last)',
    '有效 MET <b>': 'Effective MET <b>', '</b>，毛消耗 <b>': '</b>, gross <b>', '</b><br>': '</b><br>',
    '扣掉这段时间的静息消耗后，净计入 <b>': 'Minus resting burn for this time, net <b>',
    '记录这一组': 'Log this set',
    '按体重计算，不填器械重量': 'Uses bodyweight — no equipment weight',
    '使用器械重量': 'Uses equipment weight',

    // ---- 弹层：有氧 ----
    '选择有氧': 'Choose cardio', 'MET 随参数实时计算': 'MET computed live from params',
    ' 项参数': ' params',
    '记录这次有氧': 'Log this cardio',
    '毛消耗 <b>': 'Gross <b>', '，扣掉这段时间的静息消耗后净计入 <b>': ', minus resting burn, net <b>', '</b>。': '</b>.',

    // ---- 弹层：食物 ----
    '最近吃过 · 点一下直接记': 'Recent · tap to log',
    '本地食物库没有「': 'Not in the local food library: "', '」': '"',
    '点下面的按钮，按常见分量拆解这一餐': 'Use the button below to break this meal down',
    '✨ 拆解「': '✨ Break down "', '添加到': 'Add to ',
    '输入食物名，或直接描述「牛肉减脂便当」': 'Type a food, or describe it e.g. "beef bento"',
    '搜索食物 / 描述这一餐': 'Search food / describe a meal',
    ' kcal / 100g · 1': ' kcal/100g · 1', ' ≈ ': ' ≈ ', 'g': 'g',
    '这一份约 <b>': 'About <b>', '</b>，热量 <b>': '</b> servings, <b>', ' kcal</b>，蛋白 <b>': ' kcal</b>, protein <b>',
    '添加 ': 'Add ',

    // ---- 弹层：拆解结果 ----
    '拆解成 ': 'Broken into ', ' 项 · 可核对，不必全信': ' items · verify, trust sparingly',
    '份量倍数': 'Portion multiplier', '拆解（可核对）': 'Breakdown (verify)',

    // ---- 弹层：外观 / 资料 / 数据 ----
    '两套配色，不跟随系统': 'Two themes, never follows system',
    '改了会立即重算消耗与目标': 'Changes recalc burn & goals instantly',
    '称呼': 'Name', '身体数据': 'Body stats',
    '目标体重': 'Target weight', '减重速度': 'Rate of loss',
    ' kg/周 · ': ' kg/week · ',
    '日常活动量（不含训练）': 'Daily activity (workouts excluded)',
    '久坐': 'Sedentary', '办公室为主，几乎不额外走动': 'Mostly desk work, little extra walking',
    '轻度': 'Light', '每周 1–2 次轻活动 / 常走动': '1–2 light sessions/wk or often on foot',
    '中度': 'Moderate', '每周 3–4 次训练 / 站立工作': '3–4 workouts/wk or standing job',
    '高度': 'High', '体力工作 / 每周 5 次以上训练': 'Physical job / 5+ workouts/wk',
    '只有你自己能拿到，没有云端': 'Yours only — there is no cloud',
    '导出全部数据（JSON）': 'Export everything (JSON)',
    '完整备份，换设备时导入即可还原': 'Full backup — import on a new device to restore',
    '导出 CSV': 'Export CSV', '5 个文件，可直接用 Excel 打开': '5 files, opens in Excel',
    '从备份导入': 'Import from backup', '会覆盖当前全部数据': 'Overwrites all current data',
    '数据只存在这台设备的浏览器里。清理浏览器数据会一起消失，建议每周导出一次 JSON 存到网盘。': 'Data lives in this browser only — clearing browser data erases it. Export a JSON weekly to cloud drive.',
    '清空所有数据': 'Erase all data',

    // ---- 忙碌遮罩 / toast / 确认 ----
    '正在拆解这一餐…': 'Breaking down this meal…', '本地规则计算，很快': 'Local rules, quick',
    '已记录 ': 'Logged ', ' · 净消耗 ': ' · net ', '已添加 ': 'Added ', '已删除': 'Deleted',
    '已导出 JSON 备份': 'JSON backup exported', '正在导出 ': 'Exporting ', ' 个 CSV': ' CSV files',
    '已导入备份': 'Backup imported', '这个文件不是 Metri 的备份': 'Not a Metri backup file',
    '已清空': 'Cleared',
    '已载入示例数据，可在「我 → 数据」里清空': 'Demo loaded — reset anytime in Me → Data',
    '（': ' (', ' 份）': ' srv)',
    '清空后所有记录都无法恢复。确定要先导出备份再清空吗？': 'All records will be lost. Export a backup first?',

    // ---- 器械（47）----
    '杠铃卧推': 'Barbell bench press', '上斜哑铃推': 'Incline DB press', '下斜卧推': 'Decline press',
    '蝴蝶机夹胸': 'Pec deck', '龙门架夹胸': 'Cable crossover', '哑铃飞鸟': 'DB fly',
    '坐姿推胸机': 'Machine chest press', '双杠臂屈伸': 'Dips', '高位下拉': 'Lat pulldown',
    '坐姿划船': 'Seated row', '杠铃划船': 'Barbell row', 'T杠划船': 'T-bar row',
    '单臂哑铃划船': 'One-arm DB row', '直臂下压': 'Straight-arm pulldown', '引体向上': 'Pull-up',
    '硬拉': 'Deadlift', '山羊挺身': 'Back extension', '杠铃深蹲': 'Barbell squat',
    '腿举': 'Leg press', '哈克深蹲': 'Hack squat', '保加利亚分腿蹲': 'Bulgarian split squat',
    '坐姿腿屈伸': 'Leg extension', '俯卧腿弯举': 'Leg curl', '站姿提踵': 'Standing calf raise',
    '坐姿提踵': 'Seated calf raise', '罗马尼亚硬拉': 'Romanian deadlift', '杠铃臀推': 'Barbell hip thrust',
    '坐姿髋外展': 'Hip abduction', '臀桥机': 'Hip thrust machine', '绳索后踢腿': 'Cable kickback',
    '哑铃肩推': 'DB shoulder press', '哑铃侧平举': 'Lateral raise', '哑铃前平举': 'Front raise',
    '反向飞鸟': 'Reverse fly', '直立划船': 'Upright row', '绳索面拉': 'Face pull',
    '杠铃弯举': 'Barbell curl', '锤式弯举': 'Hammer curl', '牧师凳弯举': 'Preacher curl',
    '绳索下压': 'Tricep pushdown', '仰卧臂屈伸': 'Skull crusher', '窄距卧推': 'Close-grip bench',
    '哑铃颈后臂屈伸': 'Overhead DB extension', '卷腹': 'Crunch', '悬垂举腿': 'Hanging leg raise',
    '平板支撑': 'Plank', '绳索卷腹': 'Cable crunch',

    // ---- 部位（7）----
    '胸': 'Chest', '背': 'Back', '腿': 'Legs', '臀': 'Glutes', '肩': 'Shoulders', '手臂': 'Arms', '核心': 'Core',

    // ---- 有氧（10）----
    '跑步机': 'Treadmill', '户外跑': 'Outdoor run', '爬坡走': 'Incline walk', '椭圆机': 'Elliptical',
    '动感单车': 'Spin bike', '划船机': 'Rowing machine', '跳绳': 'Jump rope', '游泳': 'Swimming',
    '爬楼梯': 'Stair climb', '操课 / HIIT': 'Class / HIIT',
    '自由泳': 'Freestyle', '蛙泳': 'Breaststroke', '仰泳': 'Backstroke', '蝶泳': 'Butterfly',

    // ---- 食物（29）----
    '米饭': 'Rice', '鸡胸肉': 'Chicken breast', '鸡蛋': 'Egg', '全麦面包': 'Whole-wheat bread',
    '牛奶': 'Milk', '燕麦': 'Oats', '香蕉': 'Banana', '苹果': 'Apple', '西兰花': 'Broccoli',
    '瘦牛肉': 'Lean beef', '三文鱼': 'Salmon', '豆腐': 'Tofu', '番茄炒蛋': 'Tomato & egg',
    '红烧肉': 'Braised pork', '拿铁': 'Latte', '可乐': 'Cola', '蛋白粉': 'Whey protein',
    '红薯': 'Sweet potato', '玉米': 'Corn', '蔬菜沙拉': 'Garden salad', '面条': 'Noodles',
    '包子': 'Baozi', '油条': 'Fried dough stick', '炸鸡': 'Fried chicken', '啤酒': 'Beer',
    '酸奶': 'Yogurt', '美式咖啡': 'Americano', '黑巧克力': 'Dark chocolate', '牛油果': 'Avocado',

    // ---- 估算器拆解项 / 说明 ----
    '卤牛肉': 'Braised beef', '猪肉片': 'Sliced pork', '鱼类主菜': 'Fish main', '主菜': 'Main dish',
    '杂粮饭': 'Multigrain rice', '白米饭': 'Steamed rice', '烹调油': 'Cooking oil',
    '烹调油·酱汁': 'Oil & sauce', '西兰花·胡萝卜': 'Broccoli & carrot', '水煮蛋': 'Boiled egg',
    '生菜·番茄·黄瓜': 'Lettuce, tomato & cucumber', '甜玉米粒': 'Sweet corn',
    '沙拉酱·油醋汁': 'Dressing & vinaigrette', '牛肉·浇头': 'Beef & topping', '青菜·萝卜': 'Greens & radish',
    '汤底浮油': 'Broth oil', '肉类·丸子': 'Meat & balls', '蔬菜': 'Vegetables',
    '红油锅底·蘸料': 'Spicy broth & dip', '主料（肉/蛋）': 'Main (meat/egg)', '配菜': 'Side dishes',
    '烹调油·糖': 'Oil & sugar', ' · 主料': ' · main',
    '按常见外卖便当分量拆解': 'Portions follow typical takeout bento sizes',
    '「减脂」描述已下调主食与用油': '"Light" wording — staple and oil already reduced',
    '若实际饭量更多，可把份量调到 1.2–1.3 份': 'Bigger appetite? Scale to 1.2–1.3 servings',
    '沙拉酱是这类餐最容易漏掉的热量来源': 'Dressing is the easiest calories to miss in salads',
    '汤面按整碗估算，汤没喝完也要计入大部分热量': 'Counted as a whole bowl — leftover soup still carries most calories',
    '汤底含盐偏高，减脂期注意水肿': 'Salty broth — expect water retention',
    '蘸料和锅底占这餐热量近一半': 'Dip and broth are nearly half this meal',
    '清汤锅底可减掉约 150 kcal': 'Clear broth saves ~150 kcal',
    '家常炒菜用油量通常比预期多': 'Home stir-fry uses more oil than you think',
    '红烧/糖醋类含糖，热量按家常做法估': 'Braised/sweet-and-sour has sugar — home-style estimate',
    '没有匹配到明确品类，按「家常一荤一素」的均值估': 'No category matched — averaged over a typical meat-and-veggies meal',
    '来自本地食物库「': 'From local library: "', '」，按 100g 营养值折算': '" (scaled from 100g values)',
    '未经 AI 校验': 'Not AI-verified', '本地规则估算，误差约 ±25%': 'Local rules, ±25% margin',

    // ---- TDEE 备注（store/math）----
    '样本 ': 'Sample: ', ' 天 · 日均摄入 ': ' days · avg intake ',
    '体重记录不足 ': 'Needs ', ' 天（当前 ': ' weight days (now ',
    '时间跨度不足 ': 'Span under ',
    '饮食记录完整度不足（': 'Food logs incomplete (', ' 天里只有 ': ' days covered out of ', '，需 ': ', need ',
    '天）': ' days)', '）': ')',
    '数据波动较大，已取两者中位数': 'High variance — using the median of both'
  };

  function T(s) {
    if (lang !== 'en') return s;
    var v = EN[s];
    return v === undefined ? s : v;
  }

  g.LANG = lang;
  g.T = T;
})(window);
