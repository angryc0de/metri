package com.leanlog.app.ai

import com.leanlog.app.core.FoodEstimate
import com.leanlog.app.core.FoodEstimateItem
import com.leanlog.app.core.SeedData

/**
 * 断网 / 没配 Key / 被限流时的本地兜底。
 *
 * 它不比大模型聪明，但必须做到两件事：
 * 1. 绝不返回空 —— 记录不能因为网络问题中断；
 * 2. 把「拆解」照旧做出来 —— 用户看到「杂粮饭 140g / 卤牛肉 110g / 烹调油 8g」，
 *    才知道该改哪一项，而不是面对一个无法验证的总数。
 */
object LocalEstimator {

    private fun item(name: String, grams: Double, kcal: Double, p: Double, c: Double, f: Double) =
        FoodEstimateItem(name, grams, kcal, p, c, f, confidence = 0.55)

    fun estimate(query: String): FoodEstimate {
        val q = query.trim()
        val lower = q.lowercase()

        // 1. 本地食物库能直接命中的单品，优先用真实数据
        val hit = SeedData.foods.firstOrNull { q.contains(it.name) || lower.contains(it.name) }
        if (hit != null && !hasMealKeyword(q)) {
            val count = parseCount(q)
            val grams = (hit.servingGrams ?: 100.0) * count
            return FoodEstimate(
                items = listOf(
                    item(
                        name = "${hit.name} ${fmt(count)}${hit.servingUnit ?: "份"}",
                        grams = grams,
                        kcal = round1(hit.kcalPer100g * grams / 100.0),
                        p = round1(hit.proteinPer100g * grams / 100.0),
                        c = round1((hit.carbPer100g ?: 0.0) * grams / 100.0),
                        f = round1((hit.fatPer100g ?: 0.0) * grams / 100.0),
                    )
                ),
                servingQty = count,
                servingUnit = hit.servingUnit ?: "份",
                servingGrams = grams,
                assumptions = listOf("来自本地食物库「${hit.name}」，按 100g 营养值折算", "未经 AI 校验"),
                fromAi = false,
            )
        }

        val (items, assumptions) = when {
            q.containsAny("便当", "盒饭", "套餐", "外卖", "减脂餐", "轻食碗") -> bento(q)
            q.containsAny("沙拉", "沙律") -> salad()
            q.containsAny("面", "米线", "粉", "拉面", "馄饨", "饺子") -> noodleBowl()
            q.containsAny("火锅", "麻辣烫", "香锅", "冒菜", "干锅") -> hotpot()
            q.containsAny("炒", "红烧", "煎", "炸", "焖", "炖", "烤") -> stirFry(q)
            else -> generic(q)
        }

        val grams = items.sumOf { it.grams }
        return FoodEstimate(
            items = items,
            servingQty = 1.0,
            servingUnit = "份",
            servingGrams = grams,
            assumptions = assumptions + "本地规则估算，误差约 ±25%",
            fromAi = false,
        )
    }

    // ---------------------------------------------------------------- 规则

    private fun bento(q: String): Pair<List<FoodEstimateItem>, List<String>> {
        val light = q.contains("减脂") || q.contains("低脂") || q.contains("健身")
        val protein = when {
            q.containsAny("牛", "肥牛", "牛腩") -> item("卤牛肉", 110.0, 148.0, 28.0, 2.0, 3.6)
            q.containsAny("鸡", "鸡胸") -> item("鸡胸肉", 110.0, 146.0, 27.0, 1.0, 3.3)
            q.containsAny("猪", "排骨", "肉") -> item("猪肉片", 110.0, 220.0, 20.0, 2.0, 15.0)
            q.containsAny("鱼", "三文鱼", "鳕鱼") -> item("鱼类主菜", 110.0, 150.0, 21.0, 1.0, 7.0)
            else -> item("主菜", 110.0, 180.0, 18.0, 4.0, 9.0)
        }
        val staple = if (light) {
            item("杂粮饭", 140.0, 122.0, 4.2, 26.0, 1.1)
        } else {
            item("白米饭", 180.0, 209.0, 4.7, 46.0, 0.5)
        }
        val oil = if (light) item("烹调油", 8.0, 71.0, 0.0, 0.0, 8.0)
        else item("烹调油·酱汁", 12.0, 106.0, 0.0, 1.0, 12.0)

        val items = mutableListOf(staple, protein, item("西兰花·胡萝卜", 120.0, 40.0, 3.4, 7.0, 0.4), oil)
        if (light) items.add(1, item("水煮蛋", 50.0, 72.0, 6.7, 0.5, 5.0))

        val notes = mutableListOf("按常见外卖便当分量拆解")
        if (light) notes.add("「减脂」描述已下调主食与用油")
        notes.add("若实际饭量更多，可把份量调到 1.2–1.3 份")
        return items to notes
    }

    private fun salad() = listOf(
        item("生菜·番茄·黄瓜", 150.0, 45.0, 2.0, 7.0, 0.5),
        item("鸡胸肉", 100.0, 133.0, 24.6, 1.0, 3.0),
        item("甜玉米粒", 60.0, 67.0, 2.4, 13.0, 0.7),
        item("沙拉酱·油醋汁", 15.0, 60.0, 0.3, 1.5, 6.0),
    ) to listOf("沙拉酱是这类餐最容易漏掉的热量来源")

    private fun noodleBowl() = listOf(
        item("面条", 250.0, 343.0, 11.3, 68.0, 2.5),
        item("牛肉·浇头", 80.0, 108.0, 15.0, 1.0, 4.5),
        item("青菜·萝卜", 60.0, 12.0, 0.9, 2.0, 0.1),
        item("汤底浮油", 10.0, 90.0, 0.0, 0.0, 10.0),
    ) to listOf("汤面按整碗估算，汤没喝完也要计入大部分热量", "汤底含盐偏高，减脂期注意水肿")

    private fun hotpot() = listOf(
        item("肉类·丸子", 180.0, 260.0, 22.0, 6.0, 16.0),
        item("蔬菜", 180.0, 54.0, 4.0, 8.0, 0.6),
        item("红油锅底·蘸料", 25.0, 220.0, 1.0, 3.0, 23.0),
    ) to listOf("蘸料和锅底占这餐热量近一半", "清汤锅底可减掉约 150 kcal")

    private fun stirFry(q: String): Pair<List<FoodEstimateItem>, List<String>> {
        val veggie = q.containsAny("青菜", "菜", "茄子", "土豆丝", "豆角") &&
            !q.containsAny("肉", "鸡", "牛", "猪", "虾")
        return if (veggie) {
            listOf(
                item("蔬菜", 200.0, 60.0, 3.0, 9.0, 0.5),
                item("烹调油", 12.0, 106.0, 0.0, 0.0, 12.0),
            ) to listOf("家常炒菜用油量通常比预期多")
        } else {
            listOf(
                item("主料（肉/蛋）", 150.0, 235.0, 22.0, 5.0, 13.0),
                item("配菜", 100.0, 30.0, 2.0, 5.0, 0.3),
                item("烹调油·糖", 12.0, 106.0, 0.0, 2.0, 11.0),
            ) to listOf("红烧/糖醋类含糖，热量按家常做法估")
        }
    }

    private fun generic(q: String): Pair<List<FoodEstimateItem>, List<String>> {
        val name = if (q.length > 12) q.take(12) else q
        return listOf(
            item("$name · 主料", 150.0, 235.0, 16.0, 18.0, 9.0),
            item("烹调油", 10.0, 89.0, 0.0, 0.0, 10.0),
        ) to listOf("没有匹配到明确品类，按「家常一荤一素」的均值估")
    }

    // ---------------------------------------------------------------- 工具

    private fun hasMealKeyword(q: String) =
        q.containsAny("便当", "盒饭", "套餐", "外卖", "沙拉", "面", "火锅", "炒", "饭")

    private fun parseCount(q: String): Double {
        val cn = mapOf("一" to 1.0, "两" to 2.0, "二" to 2.0, "三" to 3.0, "四" to 4.0, "五" to 5.0)
        cn.forEach { (k, v) -> if (q.contains("${k}个") || q.contains("${k}杯") || q.contains("${k}碗") || q.contains("${k}份")) return v }
        Regex("""(\d+(?:\.\d+)?)\s*(个|杯|碗|份|根|片|勺|块)""").find(q)?.let {
            return it.groupValues[1].toDoubleOrNull()?.coerceIn(0.5, 20.0) ?: 1.0
        }
        return 1.0
    }

    private fun String.containsAny(vararg keys: String) = keys.any { contains(it) }

    private fun round1(v: Double) = Math.round(v * 10) / 10.0

    private fun fmt(v: Double) = if (v == v.toLong().toDouble()) v.toLong().toString() else v.toString()
}
