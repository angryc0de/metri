package com.leanlog.app.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 计算口径的回归测试（PRD §2）。这些数字一旦漂移，
 * 日历上所有红绿判断都会跟着错，所以每个公式都要钉死。
 */
class LeanLogMathTest {

    private val weight = 78.0
    private val height = 178.0
    private val age = 28

    @Test
    fun `BMR 男`() {
        // 10*78 + 6.25*178 - 5*28 + 5 = 1757.5
        assertEquals(1757.5, LeanLogMath.bmr(Gender.MALE, weight, height, age), 0.01)
    }

    @Test
    fun `BMR 女`() {
        // 10*60 + 6.25*165 - 5*30 - 161 = 1320.25
        assertEquals(1320.25, LeanLogMath.bmr(Gender.FEMALE, 60.0, 165.0, 30), 0.01)
    }

    @Test
    fun `力量时长要扣掉最后一组的休息`() {
        // 4 组 x (12 次 x 3 秒 + 90 秒) - 90 秒 = 414 秒 = 6.9 分钟
        assertEquals(6.9, LeanLogMath.strengthMinutes(4, 12, 90), 0.001)
        // 兜底 60 秒
        assertEquals(1.0, LeanLogMath.strengthMinutes(1, 1, 0), 0.001)
    }

    @Test
    fun `净消耗要扣除同时段静息代谢`() {
        val gross = LeanLogMath.grossKcal(9.0, weight, 30.0)
        assertEquals(368.55, gross, 0.01)
        val net = LeanLogMath.netKcal(gross, 1757.5, 30.0)
        // 1757.5 / 1440 * 30 = 36.61
        assertEquals(331.94, net, 0.02)
        // 不能为负
        assertEquals(0.0, LeanLogMath.netKcal(10.0, 1757.5, 120.0), 0.001)
    }

    @Test
    fun `缺口等于消耗减摄入`() {
        val tdeeBase = LeanLogMath.tdeeBase(1757.5, 1.2) // 2109
        assertEquals(714, LeanLogMath.deficit(tdeeBase, 331.94, 1727.0))
    }

    @Test
    fun `目标缺口按每周减重换算`() {
        assertEquals(550, LeanLogMath.targetDeficit(0.5))
        assertEquals(275, LeanLogMath.targetDeficit(0.25))
    }

    @Test
    fun `只有训练没有饮食的日子不参与评级`() {
        val s = LeanLogMath.calendarStatus(
            isFuture = false,
            hasFoodRecord = false,
            hasWorkout = true,
            deficit = 2109, // 即使算出来是巨大缺口
        )
        assertEquals(CalendarStatus.TRAINED_ONLY, s)
    }

    @Test
    fun `日历三档判定`() {
        assertEquals(
            CalendarStatus.DEFICIT,
            LeanLogMath.calendarStatus(false, true, false, 500),
        )
        assertEquals(
            CalendarStatus.EVEN,
            LeanLogMath.calendarStatus(false, true, false, -30),
        )
        assertEquals(
            CalendarStatus.SURPLUS,
            LeanLogMath.calendarStatus(false, true, false, -400),
        )
        assertEquals(
            CalendarStatus.FUTURE,
            LeanLogMath.calendarStatus(true, true, true, 0),
        )
    }

    @Test
    fun `关闭警示后只分已记录与未记录`() {
        assertEquals(
            CalendarStatus.PLAIN,
            LeanLogMath.plainStatus(false, true, false),
        )
        assertEquals(
            CalendarStatus.TRAINED_ONLY,
            LeanLogMath.plainStatus(false, false, true),
        )
        assertEquals(
            CalendarStatus.EMPTY,
            LeanLogMath.plainStatus(false, false, false),
        )
    }

    @Test
    fun `自适应 TDEE 用回归斜率反推`() {
        // 20 天，每天掉 0.1kg，日均摄入 1800
        val points = (0 until 20).map { WeightPoint(it, 80.0 - 0.1 * it) }
        val intake = (0 until 19).associateWith { 1800.0 }
        val r = LeanLogMath.adaptiveTdee(points, intake)
        assertNotNull(r.value)
        // raw = 1800 + 0.1 * 7700 = 2570
        assertEquals(2570.0, r.rawValue, 1.0)
        assertEquals(2570.0, r.value!!, 1.0)
        assertEquals(0.1, r.slopeKgPerDay * -1, 0.0001)
    }

    @Test
    fun `自适应 TDEE 数据不足时给出原因而不是假数字`() {
        val few = (0 until 5).map { WeightPoint(it, 80.0 - 0.1 * it) }
        val r1 = LeanLogMath.adaptiveTdee(few, (0 until 5).associateWith { 1800.0 })
        assertNull(r1.value)
        assertNotNull(r1.reason)

        val shortSpan = (0 until 8).map { WeightPoint(it, 80.0) }
        val r2 = LeanLogMath.adaptiveTdee(shortSpan, (0 until 8).associateWith { 1800.0 })
        assertNull(r2.value)

        val badCoverage = (0 until 20).map { WeightPoint(it, 80.0 - 0.1 * it) }
        val r3 = LeanLogMath.adaptiveTdee(badCoverage, mapOf(0 to 1800.0, 1 to 1800.0))
        assertNull(r3.value)
        assertTrue(r3.reason!!.contains("完整度"))
    }

    @Test
    fun `异常自适应值取中位数`() {
        val r = LeanLogMath.reconcileTdee(adaptive = 1200.0, formula = 2400.0)
        assertTrue(r.anomaly)
        assertEquals(1800.0, r.value, 0.01)

        val ok = LeanLogMath.reconcileTdee(adaptive = 2300.0, formula = 2400.0)
        assertTrue(!ok.anomaly)
        assertEquals(2300.0, ok.value, 0.01)
    }

    @Test
    fun `蛋白目标自动档跟随体重`() {
        assertEquals(140, LeanLogMath.proteinTarget(78.0, auto = true, customG = 0))
        assertEquals(110, LeanLogMath.proteinTarget(78.0, auto = false, customG = 110))
        assertEquals(94, LeanLogMath.proteinPreset(78.0, 1.2))
    }

    @Test
    fun `周汇总忽略空数据`() {
        assertNull(LeanLogMath.averageDeficit(emptyList()))
        assertEquals(-485, LeanLogMath.averageDeficit(listOf(-400, -570)))
        // 目标 550，±100 内算达标
        assertEquals(2, LeanLogMath.targetHitDays(listOf(500, 640, 200), 550))
    }

    @Test
    fun `种子数据完整性`() {
        assertEquals(47, SeedData.exercises.size)
        assertEquals(47, SeedData.exercises.map { it.id }.distinct().size)
        assertEquals(10, SeedData.cardioTypes.size)
        assertEquals(29, SeedData.foods.size)
        // 每个器械至少属于一个部位，且部位 id 都存在
        val validGroups = SeedData.muscleGroups.map { it.id }.toSet()
        SeedData.exercises.forEach { e ->
            assertTrue("${e.id} 没有部位", e.groups.isNotEmpty())
            e.groups.forEach { assertTrue("${e.id} 部位非法: $it", it in validGroups) }
        }
        // 多标签器械示例
        assertEquals(
            setOf("back", "legs", "glute"),
            SeedData.exercises.first { it.id == "deadlift" }.groups.toSet(),
        )
        // 所有内置食物都必须带常用份量，否则份量页没法用
        SeedData.foods.forEach { f ->
            assertNotNull("${f.name} 缺 servingUnit", f.servingUnit)
            assertNotNull("${f.name} 缺 servingGrams", f.servingGrams)
        }
    }

    @Test
    fun `有氧 MET 随参数变化`() {
        val inclineWalk = SeedData.cardioById("incline_walk")!!
        val flat = inclineWalk.met(mapOf("incline" to 1.0, "speed" to 4.5, "min" to 35.0))
        val steep = inclineWalk.met(mapOf("incline" to 15.0, "speed" to 4.5, "min" to 35.0))
        assertTrue("坡度越高 MET 必须越大", steep > flat)

        val swim = SeedData.cardioById("swim")!!
        val free = swim.met(swim.defaultParams(), "自由泳")
        val breast = swim.met(swim.defaultParams(), "蛙泳")
        assertTrue("自由泳 MET 应高于蛙泳", free > breast)
    }
}
