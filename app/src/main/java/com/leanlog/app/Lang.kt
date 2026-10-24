package com.leanlog.app

import java.util.Locale

/**
 * 轻量双语：中文原文即 key，系统语言非中文时查英文词典，查不到原样返回中文兜底。
 * 语言跟随系统（Locale），不设应用内开关；系统切换语言后 Activity 重建即生效。
 *
 * 用法：把 UI 层的用户可见文案包成 t("训练")；
 * 含插值的模板只包其中的中文片段："$reps ${t("次")}"。
 * 词典与网页版 web/js/i18n.js 保持同一份口径（器械/食物名等）。
 * 校验：node tools/check-i18n-android.js
 */
private val EN: Map<String, String> = mapOf(
    // ---- 通用词 / 单位 ----
    "力量" to "Strength", "有氧" to "Cardio", "有氧训练" to "Cardio", "休" to "R",
    "训练" to "Train", "饮食" to "Diet", "我" to "Me",
    "男" to "Male", "女" to "Female",
    "次" to "reps", "组" to "sets", "分钟" to "min", "岁" to "yrs",
    "项" to "items", "份" to "serv", "个" to "moves", "克" to "g", "天" to "d",
    "年" to "Year", "月" to "Month",
    "一" to "Mon", "二" to "Tue", "三" to "Wed", "四" to "Thu", "五" to "Fri", "六" to "Sat", "日" to "Sun",
    "时间" to "Time", "速度" to "Speed", "坡度" to "Incline", "距离" to "Distance",
    "阻力" to "Resistance", "桨频" to "Stroke", "个数" to "Count", "层数" to "Floors",
    "泳姿" to "Stroke", "强度" to "Intensity", "低" to "Low", "中" to "Mid", "高" to "High",
    "自重" to "Bodyweight", "自重动作" to "Bodyweight move", "净消耗" to "Net", "克数" to "Grams",
    "热量" to "Energy", "蛋白" to "Protein", "碳水" to "Carbs", "脂肪" to "Fat",
    "蛋白质" to "Protein", "热量缺口" to "Deficit", "热量超出" to "Over by",
    "基础代谢" to "BMR", "日消耗" to "Daily burn", "目标缺口" to "Target deficit",
    "今天" to "Today", "昨天" to "Yesterday", "今天 · " to "Today · ",
    "公斤" to "kg", "厘米" to "cm", "千卡" to "kcal",
    "确定" to "OK", "取消" to "Cancel", "删除" to "Delete", "关闭" to "Close",
    "保存" to "Save", "添加" to "Add", "清除" to "Clear", "撤销" to "Undo",
    "自动" to "Auto", "自定义" to "Custom", "· 自动" to "· Auto", "· 自定义" to "· Custom",
    "隐藏" to "Hide", "显示" to "Show",
    "早餐" to "Breakfast", "午餐" to "Lunch", "晚餐" to "Dinner", "其他" to "Other",

    // ---- 顶栏 / 日历 ----
    "你好" to "Hi", "数字颜色 = 热量缺口" to "Number color = calorie deficit",
    "缺口警示已关闭" to "Deficit warning off",
    "本周" to "This week", "平均缺口 " to "avg deficit ", "达标 " to "on target ",
    "（目标 " to " (goal ", "天" to "d", "未记录" to "Not logged",
    "摄入" to "Intake", "缺口" to "Deficit", "超出" to "Over",

    // ---- 训练页 ----
    "力量训练" to "Strength training",
    "还没有力量记录，点上面的 + 添加" to "No strength entries — tap + above to add",
    "还没有有氧记录" to "No cardio entries yet",
    "基础消耗 " to "Resting burn ", "训练净消耗 " to "Workout net ",
    "上次 " to "Last ", "已记录 " to "Logged ", " · 净消耗 " to " · net ",
    "记录这一组训练" to "Log this set", "记录这一次有氧" to "Log this cardio",
    "编辑记录" to "Edit entry", "添加记录" to "Add entry", "保存修改" to "Save changes",
    "已保存修改" to "Changes saved", "已删除 " to "Deleted ", "已加入" to "Added",

    // ---- 饮食页 ----
    "添加到" to "Add to",
    "输入食物名，或直接描述「牛肉减脂便当」" to "Type a food, or describe it e.g. \"beef bento\"",
    "搜索食物 / 描述这一餐" to "Search food / describe a meal",
    "让 AI 拆解「" to "AI-break \"", "」" to "\"",
    "最近吃过 · 点一下直接记" to "Recent · tap to log",
    "本地库没有匹配" to "Not in the local library",
    "点上面的「让 AI 拆解」估算这一餐" to "Tap \"AI-break\" above to estimate this meal",
    "自建" to "Custom", "按" to "by", "记录" to "log",
    "数量（" to "Qty (", "）" to ")", "克数（g）" to "Grams (g)",
    "蛋白 " to "Protein ",
    "AI 估算" to "AI estimate", "AI 拆解 · 可逐项核对" to "AI breakdown · verify items",
    "本地规则估算 · 未连 AI" to "Local rules · no AI",
    "改份量" to "Adjust portion", "添加 " to "Add ",
    " · 约 " to " · about ", "本地估算" to "Local est.",
    "拆解（可核对，不必全信）" to "Breakdown (verify, trust sparingly)",
    " · 蛋白 " to " · protein ", "份量倍数：" to "Portion multiplier:", " 份" to " srv",

    // ---- 我页 ----
    "当前体重" to "Current weight", "目标 " to "Goal ", "每周 " to "per week ",
    "记录体重" to "Weight log", "日体重" to "Daily weight", "7 日平均" to "7-day avg",
    "条记录" to " entries",
    "再记录几天体重，这里会出现趋势曲线。单日体重波动 ±1kg 多半是水，看趋势才有意义。"
        to "Log a few more days and a trend curve appears. ±1 kg daily swings are mostly water — trends matter.",
    "能量消耗估算" to "Energy burn estimate", "公式消耗" to "Formula", "实测消耗" to "Measured",
    "实测消耗由最近 30 天的「摄入 − 体重变化」反推，比公式更贴近你自己。日历颜色用的是这个值。"
        to "Measured burn is back-calculated from the last 30 days (intake − weight change) — closer to the real you. Calendar colors use this value.",
    "还没有记录" to "Nothing yet",
    "个人信息" to "Profile", "姓名" to "Name", "未填写" to "Not set",
    "用于页面问候，可留空" to "Shown in greetings; optional",
    "BMR 公式需要年龄，10–100" to "BMR needs age, 10–100",
    "BMR 公式需要身高，120–230" to "BMR needs height, 120–230",
    "今天体重" to "Today's weight", "记录后会写入体重曲线" to "Saved into the weight curve",
    "减脂目标，仅用于展示" to "Fat-loss goal, display only",
    "BMR 公式的常数项不同" to "BMR constant differs by gender",
    "只描述训练之外的日常，训练消耗单独计算" to "Daily life only — workouts are counted separately",
    "目标缺口 = 每周减重 × 7700 / 7" to "Deficit = weekly loss × 7700 / 7",
    " kg / 周" to " kg/week", "每天目标缺口 " to "Daily deficit target ",
    "每周超过 1kg 的减重速度很难只掉脂肪，通常会带走肌肉。"
        to "Losing over 1 kg/week rarely spares fat — it usually takes muscle with it.",
    "性别" to "Gender", "年龄" to "Age", "身高" to "Height", "体重" to "Weight",
    "日常活动量" to "Daily activity", "目标体重" to "Target weight", "每周减重" to "Weekly loss",
    "每日蛋白目标" to "Daily protein goal",
    "只影响蛋白进度条，不参与热量缺口计算" to "Only drives the protein bar — not the deficit math",
    " g / 天" to " g/day",
    "当前：自动" to "Now: auto", "当前：自定义" to "Now: custom",
    "按体重折算" to "Scaled by bodyweight",
    "保存 " to "Save ", "恢复按体重自动计算（" to "Back to bodyweight-based (",
    "设置" to "Settings",
    "外观" to "Appearance", "黑夜模式" to "Dark mode", "白天模式" to "Light mode",
    "换肤立即生效，默认黑夜模式" to "Applies instantly; dark is the default",
    "两套皮肤只交换底色和文字，绿=缺口、红=超出、蓝=力量、黄=有氧的语义不变，换皮不改变任何判断。"
        to "Themes only swap surfaces and text — green=deficit, red=over, blue=strength, yellow=cardio stay the same.",
    "使用中" to "In use", "点按切换" to "Tap to switch", "黑夜" to "Dark", "白天" to "Light",
    "热量缺口警示" to "Deficit warning",
    "关闭后日历不再按缺口标绿/红，只保留中性数字" to "Off keeps calendar numbers neutral, no green/red",
    "AI 模型" to "AI model", "OpenAI 兼容接口" to "OpenAI-compatible API",
    "OpenAI 兼容接口，Key 只存在本机" to "OpenAI-compatible; the key never leaves this device",
    "文本模型" to "Text model", "图片识别模型（可留空）" to "Vision model (optional)",
    "为什么必须自己填 Key" to "Why you must bring your own key",
    "Key 打进 APK 会被反编译提取，所以这个 App 不内置任何 Key。"
        to "A key bundled in the APK can be extracted by decompiling — so none is built in.",
    "填自己的 Key 后，热量估算和拍照识别才会真正联网；"
        to "With your own key, estimation and photo recognition go online;",
    "不填也能用 —— 会走本地规则估算，只是精度低一些。"
        to "without one it still works via local rules, just less accurately.",
    "数据导出" to "Data export", "JSON 备份 + CSV" to "JSON backup + CSV",
    "备份能恢复，CSV 能拿去分析" to "Backups restore; CSVs are ready for analysis",
    "JSON 全量备份" to "Full JSON backup",
    "个人资料、体重、饮食、力量、有氧，可用它恢复" to "Profile, weight, meals, strength, cardio — restores everything",
    "每次进食一条，含克数与营养" to "One row per meal item, grams & nutrition",
    "每组力量训练与净消耗" to "Per-set strength entries & net burn",
    "有氧参数、MET 与净消耗" to "Cardio params, MET & net burn",
    "体重记录" to "Weight log",
    "按天汇总的摄入、消耗与缺口，可直接做透视表" to "Per-day intake, burn & deficit — pivot-table ready",
    "文件会同时写入系统「下载 / LeanLog」目录，并保留一份在应用私有目录里。"
        to "Files are written to the system Downloads/LeanLog folder, with a copy kept in app storage.",
    "目前共 " to "Currently ", " 条饮食（当天）· 体重 " to " meals (today) · weight ",
    " 条记录" to " records", "导出全部格式" to "Export all formats",
    "关于" to "About", "本地记录 · 无账号" to "Local only · no account",
    "所有数据只存在这台设备上，没有账号也没有云端"
        to "All data stays on this device — no account, no cloud",
    "待积累" to "Pending data",
    "久坐" to "Sedentary", "轻度" to "Light", "中度" to "Moderate", "高度" to "High",
    "办公室为主，几乎不额外走动" to "Mostly desk work, little extra walking",
    "每周 1–2 次轻活动 / 常走动" to "1–2 light sessions/wk or often on foot",
    "每周 3–4 次训练 / 站立工作" to "3–4 workouts/wk or standing job",
    "体力工作 / 每周 5 次以上训练" to "Physical job / 5+ workouts/wk",

    // ---- 引导页 ----
    "先了解一下你" to "First, a quick intro",
    "这几个数决定你的基础代谢和每日热量缺口。之后随时能在「我 → 个人信息」里改。"
        to "These numbers set your BMR and daily deficit target — editable anytime in Me → Profile.",
    "称呼" to "Name", "怎么称呼你（可留空）" to "Your name (optional)",
    "记完训练和饮食，日历用颜色告诉你当天有没有缺口"
        to "Log workouts and meals — the calendar colors show each day's deficit",
    "数据全部存在本机，断网也能记录和查看" to "Everything stays on-device; works fully offline",
    "AI 估算可选，填了自己的 Key 才会联网" to "AI is optional — only connects with your own key",
    "基础代谢 " to "BMR ", "每日消耗约 " to "Daily burn ≈ ", "开始使用" to "Start",
    "资料已保存，开始记录吧" to "Saved — happy logging",

    // ---- TDEE 备注 / 自适应 ----
    "体重记录不足 " to "Needs ", " 天（当前 " to " weight days (now ", " 天）" to " days)",
    "时间跨度不足 " to "Span under ",
    "饮食记录完整度不足（" to "Food logs incomplete (",
    " 天里只有 " to " days covered out of ", " 天，需 " to ", need ",
    "数据波动较大，已取两者中位数" to "High variance — using the median of both",

    // ---- 估算器拆解项 / 说明 ----
    "卤牛肉" to "Braised beef", "猪肉片" to "Sliced pork", "鱼类主菜" to "Fish main", "主菜" to "Main dish",
    "主料" to "Main", "杂粮饭" to "Multigrain rice", "白米饭" to "Steamed rice", "烹调油" to "Cooking oil",
    "烹调油·酱汁" to "Oil & sauce", "西兰花·胡萝卜" to "Broccoli & carrot", "水煮蛋" to "Boiled egg",
    "生菜·番茄·黄瓜" to "Lettuce, tomato & cucumber", "甜玉米粒" to "Sweet corn",
    "沙拉酱·油醋汁" to "Dressing & vinaigrette", "牛肉·浇头" to "Beef & topping", "青菜·萝卜" to "Greens & radish",
    "汤底浮油" to "Broth oil", "肉类·丸子" to "Meat & balls", "蔬菜" to "Vegetables",
    "红油锅底·蘸料" to "Spicy broth & dip", "主料（肉/蛋）" to "Main (meat/egg)", "配菜" to "Side dishes",
    "烹调油·糖" to "Oil & sugar", " · 主料" to " · main",
    "按常见外卖便当分量拆解" to "Portions follow typical takeout bento sizes",
    "「减脂」描述已下调主食与用油" to "\"Light\" wording — staple and oil already reduced",
    "若实际饭量更多，可把份量调到 1.2–1.3 份" to "Bigger appetite? Scale to 1.2–1.3 servings",
    "沙拉酱是这类餐最容易漏掉的热量来源" to "Dressing is the easiest calories to miss in salads",
    "汤面按整碗估算，汤没喝完也要计入大部分热量" to "Counted as a whole bowl — leftover soup still carries most calories",
    "汤底含盐偏高，减脂期注意水肿" to "Salty broth — expect water retention",
    "蘸料和锅底占这餐热量近一半" to "Dip and broth are nearly half this meal",
    "清汤锅底可减掉约 150 kcal" to "Clear broth saves ~150 kcal",
    "家常炒菜用油量通常比预期多" to "Home stir-fry uses more oil than you think",
    "红烧/糖醋类含糖，热量按家常做法估" to "Braised/sweet-and-sour has sugar — home-style estimate",
    "没有匹配到明确品类，按「家常一荤一素」的均值估" to "No category matched — averaged over a typical meat-and-veggies meal",
    "来自本地食物库「" to "From local library: \"", "」，按 100g 营养值折算" to "\" (scaled from 100g values)",
    "未经 AI 校验" to "Not AI-verified", "本地规则估算，误差约 ±25%" to "Local rules, ±25% margin",
    "AI 正在估算…" to "AI estimating…", "通常 10–20 秒，免费模型高峰期更久"
        to "Usually 10–20 s; longer at peak times on free models",
    "AI 请求失败，已用本地规则估算" to "AI request failed — used local rules",
    "AI 已配置" to "AI configured", "已清除 API Key" to "API key cleared",
    "蛋白目标已恢复自动" to "Protein goal back to auto", "蛋白目标已设为 " to "Protein goal set to ",
    "已导出 " to "Exported ", "个文件" to " files", "导出失败：" to "Export failed: ",

    // ---- 弹层：器械 / 有氧 ----
    "选择器械" to "Choose exercise", "搜索器械，例如：划船" to "Search exercises, e.g. rowing",
    "最近" to "Recent", "选择有氧项目" to "Choose cardio",
    "预计训练时长 " to "Est. duration ",
    " 分钟（组数 ×（每组约 " to " min (sets × (about ",
    " 秒 + 组间休息 90 秒））" to " s/rep + 90 s rest))",
    "开启后不记重量，只记次数与组数" to "On: no weight — only reps & sets",
    "重量 kg" to "Weight kg", "次数" to "Reps", "组数" to "Sets",
    "消耗按「MET × 体重 × 时长」估算，已扣除这段时间的静息消耗。实际误差约 ±15%。"
        to "Burn = MET × weight × duration, minus resting burn for that time. Real-world margin ±15%.",

    // ---- 部位（7）----
    "胸" to "Chest", "背" to "Back", "腿" to "Legs", "臀" to "Glutes",
    "肩" to "Shoulders", "手臂" to "Arms", "核心" to "Core",

    // ---- 器械（47）----
    "杠铃卧推" to "Barbell bench press", "上斜哑铃推" to "Incline DB press", "下斜卧推" to "Decline press",
    "蝴蝶机夹胸" to "Pec deck", "龙门架夹胸" to "Cable crossover", "哑铃飞鸟" to "DB fly",
    "坐姿推胸机" to "Machine chest press", "双杠臂屈伸" to "Dips", "高位下拉" to "Lat pulldown",
    "坐姿划船" to "Seated row", "杠铃划船" to "Barbell row", "T杠划船" to "T-bar row",
    "单臂哑铃划船" to "One-arm DB row", "直臂下压" to "Straight-arm pulldown", "引体向上" to "Pull-up",
    "硬拉" to "Deadlift", "山羊挺身" to "Back extension", "杠铃深蹲" to "Barbell squat",
    "腿举" to "Leg press", "哈克深蹲" to "Hack squat", "保加利亚分腿蹲" to "Bulgarian split squat",
    "坐姿腿屈伸" to "Leg extension", "俯卧腿弯举" to "Leg curl", "站姿提踵" to "Standing calf raise",
    "坐姿提踵" to "Seated calf raise", "罗马尼亚硬拉" to "Romanian deadlift", "杠铃臀推" to "Barbell hip thrust",
    "坐姿髋外展" to "Hip abduction", "臀桥机" to "Hip thrust machine", "绳索后踢腿" to "Cable kickback",
    "哑铃肩推" to "DB shoulder press", "哑铃侧平举" to "Lateral raise", "哑铃前平举" to "Front raise",
    "反向飞鸟" to "Reverse fly", "直立划船" to "Upright row", "绳索面拉" to "Face pull",
    "杠铃弯举" to "Barbell curl", "锤式弯举" to "Hammer curl", "牧师凳弯举" to "Preacher curl",
    "绳索下压" to "Tricep pushdown", "仰卧臂屈伸" to "Skull crusher", "窄距卧推" to "Close-grip bench",
    "哑铃颈后臂屈伸" to "Overhead DB extension", "卷腹" to "Crunch", "悬垂举腿" to "Hanging leg raise",
    "平板支撑" to "Plank", "绳索卷腹" to "Cable crunch",

    // ---- 有氧（10）+ 泳姿枚举 ----
    "跑步机" to "Treadmill", "户外跑" to "Outdoor run", "爬坡走" to "Incline walk", "椭圆机" to "Elliptical",
    "动感单车" to "Spin bike", "划船机" to "Rowing machine", "跳绳" to "Jump rope", "游泳" to "Swimming",
    "爬楼梯" to "Stair climb", "操课 / HIIT" to "Class / HIIT",
    "自由泳" to "Freestyle", "蛙泳" to "Breaststroke", "仰泳" to "Backstroke", "蝶泳" to "Butterfly",

    // ---- 食物（29）----
    "米饭" to "Rice", "鸡胸肉" to "Chicken breast", "鸡蛋" to "Egg", "全麦面包" to "Whole-wheat bread",
    "牛奶" to "Milk", "燕麦" to "Oats", "香蕉" to "Banana", "苹果" to "Apple", "西兰花" to "Broccoli",
    "瘦牛肉" to "Lean beef", "三文鱼" to "Salmon", "豆腐" to "Tofu", "番茄炒蛋" to "Tomato & egg",
    "红烧肉" to "Braised pork", "拿铁" to "Latte", "可乐" to "Cola", "蛋白粉" to "Whey protein",
    "红薯" to "Sweet potato", "玉米" to "Corn", "蔬菜沙拉" to "Garden salad", "面条" to "Noodles",
    "包子" to "Baozi", "油条" to "Fried dough stick", "炸鸡" to "Fried chicken", "啤酒" to "Beer",
    "酸奶" to "Yogurt", "美式咖啡" to "Americano", "黑巧克力" to "Dark chocolate", "牛油果" to "Avocado"
)

/** 当前系统语言是否为英文（zh* 之外一律按英文处理）。 */
fun isEnglish(): Boolean = Locale.getDefault().language != "zh"

/** 翻译：英文模式查词典，命中返回英文，否则原样返回中文。 */
fun t(zh: String): String = if (isEnglish()) EN[zh] ?: zh else zh
