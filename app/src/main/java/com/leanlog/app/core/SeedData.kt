package com.leanlog.app.core

/**
 * 内置数据（PRD §7）。首次启动时灌入 Room，之后用户可增删改，这里只作为种子。
 */
object SeedData {

    // ------------------------------------------------------------ 7 个部位

    val muscleGroups: List<MuscleGroupSeed> = listOf(
        MuscleGroupSeed("chest", "胸", 0),
        MuscleGroupSeed("back", "背", 1),
        MuscleGroupSeed("legs", "腿", 2),
        MuscleGroupSeed("glute", "臀", 3),
        MuscleGroupSeed("shoulder", "肩", 4),
        MuscleGroupSeed("arm", "手臂", 5),
        MuscleGroupSeed("core", "核心", 6),
    )

    // ------------------------------------------------------------ 47 个器械

    val exercises: List<ExerciseSeed> = listOf(
        // 胸 8
        ExerciseSeed("bench_press", "杠铃卧推", 5.0, false, listOf("chest")),
        ExerciseSeed("incline_press", "上斜哑铃推", 5.0, false, listOf("chest")),
        ExerciseSeed("decline_press", "下斜卧推", 5.0, false, listOf("chest")),
        ExerciseSeed("pec_deck", "蝴蝶机夹胸", 3.5, false, listOf("chest")),
        ExerciseSeed("cable_crossover", "龙门架夹胸", 3.5, false, listOf("chest")),
        ExerciseSeed("fly_db", "哑铃飞鸟", 3.5, false, listOf("chest")),
        ExerciseSeed("machine_press", "坐姿推胸机", 4.5, false, listOf("chest")),
        ExerciseSeed("dip", "双杠臂屈伸", 5.0, true, listOf("chest", "arm")),

        // 背 9
        ExerciseSeed("lat_pulldown", "高位下拉", 4.5, false, listOf("back")),
        ExerciseSeed("seated_row", "坐姿划船", 4.5, false, listOf("back")),
        ExerciseSeed("barbell_row", "杠铃划船", 5.0, false, listOf("back")),
        ExerciseSeed("tbar_row", "T杠划船", 5.0, false, listOf("back")),
        ExerciseSeed("db_row", "单臂哑铃划船", 4.5, false, listOf("back")),
        ExerciseSeed("straight_arm", "直臂下压", 3.5, false, listOf("back")),
        ExerciseSeed("pullup", "引体向上", 5.0, true, listOf("back")),
        ExerciseSeed("deadlift", "硬拉", 6.0, false, listOf("back", "legs", "glute")),
        ExerciseSeed("back_extension", "山羊挺身", 3.5, true, listOf("back", "core", "glute")),

        // 腿 9
        ExerciseSeed("squat", "杠铃深蹲", 6.0, false, listOf("legs", "glute")),
        ExerciseSeed("leg_press", "腿举", 5.0, false, listOf("legs")),
        ExerciseSeed("hack_squat", "哈克深蹲", 5.5, false, listOf("legs", "glute")),
        ExerciseSeed("split_squat", "保加利亚分腿蹲", 5.0, false, listOf("legs", "glute")),
        ExerciseSeed("leg_extension", "坐姿腿屈伸", 3.5, false, listOf("legs")),
        ExerciseSeed("leg_curl", "俯卧腿弯举", 3.5, false, listOf("legs")),
        ExerciseSeed("calf_raise", "站姿提踵", 3.5, false, listOf("legs")),
        ExerciseSeed("seated_calf", "坐姿提踵", 3.5, false, listOf("legs")),
        ExerciseSeed("rdl", "罗马尼亚硬拉", 5.5, false, listOf("legs", "glute")),

        // 臀 4
        ExerciseSeed("hip_thrust", "杠铃臀推", 4.5, false, listOf("glute")),
        ExerciseSeed("hip_abduction", "坐姿髋外展", 3.5, false, listOf("glute")),
        ExerciseSeed("hip_extension", "臀桥机", 3.5, false, listOf("glute")),
        ExerciseSeed("cable_kickback", "绳索后踢腿", 3.5, false, listOf("glute")),

        // 肩 6
        ExerciseSeed("shoulder_press", "哑铃肩推", 4.5, false, listOf("shoulder")),
        ExerciseSeed("lateral_raise", "哑铃侧平举", 3.5, false, listOf("shoulder")),
        ExerciseSeed("front_raise", "哑铃前平举", 3.5, false, listOf("shoulder")),
        ExerciseSeed("reverse_fly", "反向飞鸟", 3.5, false, listOf("shoulder", "back")),
        ExerciseSeed("upright_row", "直立划船", 4.0, false, listOf("shoulder")),
        ExerciseSeed("face_pull", "绳索面拉", 3.5, false, listOf("shoulder", "back")),

        // 手臂 7
        ExerciseSeed("barbell_curl", "杠铃弯举", 3.5, false, listOf("arm")),
        ExerciseSeed("hammer_curl", "锤式弯举", 3.5, false, listOf("arm")),
        ExerciseSeed("preacher_curl", "牧师凳弯举", 3.5, false, listOf("arm")),
        ExerciseSeed("tricep_pushdown", "绳索下压", 3.5, false, listOf("arm")),
        ExerciseSeed("skull_crusher", "仰卧臂屈伸", 3.5, false, listOf("arm")),
        ExerciseSeed("close_grip", "窄距卧推", 4.5, false, listOf("arm", "chest")),
        ExerciseSeed("overhead_ext", "哑铃颈后臂屈伸", 3.5, false, listOf("arm")),

        // 核心 4
        ExerciseSeed("crunch", "卷腹", 3.5, true, listOf("core")),
        ExerciseSeed("hanging_leg", "悬垂举腿", 3.5, true, listOf("core")),
        ExerciseSeed("plank", "平板支撑", 3.5, true, listOf("core")),
        ExerciseSeed("cable_crunch", "绳索卷腹", 3.5, false, listOf("core")),
    )

    // ------------------------------------------------------------ 10 个有氧

    val cardioTypes: List<CardioType> = listOf(
        CardioType(
            id = "treadmill",
            name = "跑步机",
            params = listOf(
                ParamSpec("speed", "速度", "km/h", 3.0, 15.0, 0.5, 6.0),
                ParamSpec("incline", "坡度", "%", 0.0, 15.0, 1.0, 0.0),
                ParamSpec("min", "时间", "分钟", 5.0, 90.0, 5.0, 30.0),
            ),
        ) { p, _ -> 3.0 + (p["speed"] ?: 0.0) * 0.72 + (p["incline"] ?: 0.0) * 0.28 },

        CardioType(
            id = "outdoor_run",
            name = "户外跑",
            params = listOf(
                ParamSpec("dist", "距离", "km", 0.5, 21.0, 0.5, 5.0),
                ParamSpec("min", "时间", "分钟", 5.0, 180.0, 5.0, 32.0),
            ),
        ) { p, _ ->
            val speedKmh = (p["dist"] ?: 0.0) / (p["min"] ?: 1.0).coerceAtLeast(1.0) * 60.0
            3.5 + speedKmh * 0.62
        },

        CardioType(
            id = "incline_walk",
            name = "爬坡走",
            params = listOf(
                ParamSpec("incline", "坡度", "%", 1.0, 20.0, 1.0, 10.0),
                ParamSpec("speed", "速度", "km/h", 2.0, 8.0, 0.5, 4.5),
                ParamSpec("min", "时间", "分钟", 5.0, 90.0, 5.0, 35.0),
            ),
        ) { p, _ -> 3.2 + (p["incline"] ?: 0.0) * 0.42 + (p["speed"] ?: 0.0) * 0.3 },

        CardioType(
            id = "elliptical",
            name = "椭圆机",
            params = listOf(
                ParamSpec("res", "阻力", "档", 1.0, 20.0, 1.0, 8.0),
                ParamSpec("min", "时间", "分钟", 5.0, 90.0, 5.0, 30.0),
            ),
        ) { p, _ -> 4.2 + (p["res"] ?: 0.0) * 0.18 },

        CardioType(
            id = "spin_bike",
            name = "动感单车",
            params = listOf(
                ParamSpec("res", "阻力", "档", 1.0, 24.0, 1.0, 10.0),
                ParamSpec("min", "时间", "分钟", 5.0, 90.0, 5.0, 35.0),
                ParamSpec("dist", "距离", "km", 0.0, 40.0, 1.0, 12.0),
            ),
        ) { p, _ -> 4.5 + (p["res"] ?: 0.0) * 0.2 },

        CardioType(
            id = "rower",
            name = "划船机",
            params = listOf(
                ParamSpec("dist", "距离", "km", 0.5, 20.0, 0.5, 5.0),
                ParamSpec("min", "时间", "分钟", 3.0, 90.0, 1.0, 25.0),
                ParamSpec("spm", "桨频", "spm", 16.0, 36.0, 1.0, 24.0),
            ),
        ) { p, _ -> 5.5 + (p["spm"] ?: 0.0) * 0.06 },

        CardioType(
            id = "jump_rope",
            name = "跳绳",
            params = listOf(
                ParamSpec("cnt", "个数", "个", 100.0, 5000.0, 100.0, 1500.0),
                ParamSpec("min", "时间", "分钟", 1.0, 60.0, 1.0, 15.0),
            ),
        ) { _, _ -> 11.0 },

        CardioType(
            id = "swim",
            name = "游泳",
            params = listOf(
                ParamSpec("dist", "距离", "米", 100.0, 3000.0, 100.0, 1000.0),
                ParamSpec("min", "时间", "分钟", 5.0, 90.0, 5.0, 30.0),
            ),
            enumSpec = EnumSpec("style", "泳姿", listOf("自由泳", "蛙泳", "仰泳", "蝶泳"), "自由泳"),
        ) { _, style ->
            when (style) {
                "蛙泳" -> 7.5
                "仰泳" -> 6.8
                "蝶泳" -> 9.8
                else -> 8.3
            }
        },

        CardioType(
            id = "stairs",
            name = "爬楼梯",
            params = listOf(
                ParamSpec("floor", "层数", "层", 5.0, 200.0, 5.0, 40.0),
                ParamSpec("min", "时间", "分钟", 3.0, 90.0, 1.0, 20.0),
            ),
        ) { _, _ -> 8.8 },

        CardioType(
            id = "hiit",
            name = "操课 / HIIT",
            params = listOf(
                ParamSpec("min", "时间", "分钟", 5.0, 120.0, 5.0, 30.0),
            ),
            enumSpec = EnumSpec("level", "强度", listOf("低", "中", "高"), "中"),
        ) { _, level ->
            when (level) {
                "高" -> 11.0
                "低" -> 5.5
                else -> 8.0
            }
        },
    )

    fun cardioById(id: String): CardioType? = cardioTypes.firstOrNull { it.id == id }

    // ------------------------------------------------------------ 29 个食物

    /** 碳水/脂肪未收录的填 null，界面上不显示该项，而不是给个假数字。 */
    val foods: List<FoodSeed> = listOf(
        FoodSeed("rice", "米饭", "mifan 白米饭", 116.0, 2.6, null, null, "碗", 150.0, "🍚"),
        FoodSeed("chicken", "鸡胸肉", "jixiong", 133.0, 24.6, null, null, "块", 120.0, "🍗"),
        FoodSeed("egg", "鸡蛋", "jidan", 144.0, 13.3, null, null, "个", 50.0, "🥚"),
        FoodSeed("bread", "全麦面包", "mianbao", 246.0, 9.0, null, null, "片", 35.0, "🍞"),
        FoodSeed("milk", "牛奶", "niunai", 54.0, 3.0, null, null, "盒", 250.0, "🥛"),
        FoodSeed("oats", "燕麦", "yanmai", 377.0, 13.0, null, null, "份", 40.0, "🥣"),
        FoodSeed("banana", "香蕉", "xiangjiao", 93.0, 1.4, null, null, "根", 120.0, "🍌"),
        FoodSeed("apple", "苹果", "pingguo", 53.0, 0.2, null, null, "个", 200.0, "🍎"),
        FoodSeed("broccoli", "西兰花", "xilanhua", 34.0, 2.8, null, null, "份", 150.0, "🥦"),
        FoodSeed("beef", "瘦牛肉", "niurou", 106.0, 20.2, null, null, "份", 120.0, "🥩"),
        FoodSeed("salmon", "三文鱼", "sanwenyu", 139.0, 17.2, null, null, "份", 100.0, "🐟"),
        FoodSeed("tofu", "豆腐", "doufu", 82.0, 8.1, null, null, "块", 100.0, "🍲"),
        FoodSeed("tomato_egg", "番茄炒蛋", "fanqiechaodan", 86.0, 5.4, null, null, "份", 200.0, "🍅"),
        FoodSeed("pork", "红烧肉", "hongshaorou", 470.0, 13.0, null, null, "份", 100.0, "🥓"),
        FoodSeed("latte", "拿铁", "natie coffee", 55.0, 3.0, null, null, "杯", 350.0, "☕"),
        FoodSeed("cola", "可乐", "kele", 43.0, 0.0, null, null, "罐", 330.0, "🥤"),
        FoodSeed("whey", "蛋白粉", "danbaifen", 400.0, 78.0, null, null, "勺", 30.0, "💪"),
        FoodSeed("sweetpotato", "红薯", "hongshu", 86.0, 1.6, null, null, "个", 150.0, "🍠"),
        FoodSeed("corn", "玉米", "yumi", 112.0, 4.0, null, null, "根", 150.0, "🌽"),
        FoodSeed("salad", "蔬菜沙拉", "shala", 70.0, 2.0, null, null, "份", 200.0, "🥗"),
        FoodSeed("noodle", "面条", "miantiao", 137.0, 4.5, null, null, "碗", 200.0, "🍜"),
        FoodSeed("baozi", "包子", "baozi", 227.0, 7.0, null, null, "个", 80.0, "🥟"),
        FoodSeed("youtiao", "油条", "youtiao", 388.0, 6.9, null, null, "根", 60.0, "🥖"),
        FoodSeed("fried_chicken", "炸鸡", "zhaji", 280.0, 18.0, null, null, "块", 100.0, "🍗"),
        FoodSeed("beer", "啤酒", "pijiu", 43.0, 0.4, null, null, "罐", 330.0, "🍺"),
        FoodSeed("yogurt", "酸奶", "suannai", 72.0, 3.2, null, null, "杯", 150.0, "🥛"),
        FoodSeed("americano", "美式咖啡", "meishi", 2.0, 0.1, null, null, "杯", 350.0, "☕"),
        FoodSeed("choco", "黑巧克力", "qiaokeli", 546.0, 7.8, null, null, "块", 20.0, "🍫"),
        FoodSeed("avocado", "牛油果", "niuyouguo", 160.0, 2.0, null, null, "个", 150.0, "🥑"),
    )

    fun foodById(id: String): FoodSeed? = foods.firstOrNull { it.id == id }

    // ------------------------------------------------------------ 活动系数

    val activityLevels: List<ActivityLevel> = listOf(
        ActivityLevel(1.2, "久坐", "办公室为主，几乎不额外走动"),
        ActivityLevel(1.375, "轻度", "每周 1–2 次轻活动 / 常走动"),
        ActivityLevel(1.55, "中度", "每周 3–4 次训练 / 站立工作"),
        ActivityLevel(1.725, "高度", "体力工作 / 每周 5 次以上训练"),
    )
}

data class ActivityLevel(val factor: Double, val label: String, val desc: String)
