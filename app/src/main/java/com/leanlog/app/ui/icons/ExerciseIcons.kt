package com.leanlog.app.ui.icons

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 器械 / 有氧项目线稿图标。
 *
 * 为什么不用图片：几十个器械的实物照片有版权，风格也统一不了；
 * 用同一套 48×48 线稿自绘，体积几乎为零、颜色跟随主题、任何尺寸都清晰。
 * 这里直接沿用原型里已经画好的路径，两边的图形完全一致。
 */
private const val VIEW = 48f

private class Pen(val d: DrawScope, val s: Float, val color: Color) {

    private val stroke = Stroke(width = 2f * s, cap = StrokeCap.Round, join = StrokeJoin.Round)

    fun line(x1: Float, y1: Float, x2: Float, y2: Float) = d.drawLine(
        color = color,
        start = Offset(x1 * s, y1 * s),
        end = Offset(x2 * s, y2 * s),
        strokeWidth = 2f * s,
        cap = StrokeCap.Round,
    )

    fun circle(cx: Float, cy: Float, r: Float) =
        d.drawCircle(color, r * s, Offset(cx * s, cy * s), style = stroke)

    fun rect(x: Float, y: Float, w: Float, h: Float, r: Float = 2f) = d.drawRoundRect(
        color = color,
        topLeft = Offset(x * s, y * s),
        size = Size(w * s, h * s),
        cornerRadius = CornerRadius(r * s),
        style = stroke,
    )

    fun fillRect(x: Float, y: Float, w: Float, h: Float, r: Float = 1.2f) = d.drawRoundRect(
        color = color,
        topLeft = Offset(x * s, y * s),
        size = Size(w * s, h * s),
        cornerRadius = CornerRadius(r * s),
    )

    fun strokePath(path: Path) = d.drawPath(path, color, style = stroke)

    fun fillPath(path: Path) = d.drawPath(path, color)

    fun path(build: Path.() -> Unit): Path = Path().apply(build)

    /** 杠铃：横杆 + 两端杠铃片 */
    fun bar(y: Float, x1: Float, x2: Float) {
        line(x1, y, x2, y)
        fillRect(x1 - 4f, y - 5f, 4f, 10f, 1.5f)
        fillRect(x2, y - 5f, 4f, 10f, 1.5f)
    }

    /** 人头 */
    fun head(x: Float, y: Float) = circle(x, y, 3f)

    /** 哑铃 */
    fun dumbbell(cx: Float, cy: Float) {
        line(cx - 6f, cy, cx + 6f, cy)
        fillRect(cx - 9f, cy - 4f, 3.6f, 8f, 1.2f)
        fillRect(cx + 5.4f, cy - 4f, 3.6f, 8f, 1.2f)
    }
}

private val icons: Map<String, Pen.() -> Unit> = mapOf(

    // ---------------------------------------------------------------- 胸
    "bench_press" to {
        line(13f, 29f, 37f, 29f); line(15f, 29f, 15f, 40f); line(35f, 29f, 35f, 40f)
        line(21f, 19f, 21f, 29f); line(27f, 19f, 27f, 29f); bar(19f, 5f, 42f); head(13f, 25f)
    },
    "incline_press" to {
        line(10f, 38f, 30f, 25f); line(30f, 25f, 38f, 31f); line(38f, 31f, 38f, 40f)
        line(16f, 31f, 16f, 40f); dumbbell(17f, 17f); dumbbell(28f, 12f)
    },
    "pec_deck" to {
        line(24f, 40f, 24f, 15f); circle(24f, 12f, 2.6f); line(24f, 22f, 13f, 15f)
        line(24f, 22f, 35f, 15f); rect(4f, 10f, 9f, 9f, 2.5f); rect(35f, 10f, 9f, 9f, 2.5f)
        line(17f, 40f, 31f, 40f)
    },
    "cable_crossover" to {
        rect(5f, 7f, 4f, 33f, 1.5f); rect(39f, 7f, 4f, 33f, 1.5f)
        circle(7f, 11f, 2f); circle(41f, 11f, 2f)
        line(7f, 11f, 24f, 27f); line(41f, 11f, 24f, 27f); circle(24f, 27f, 2.6f)
        line(6f, 40f, 42f, 40f)
    },
    "decline_press" to {
        line(8f, 22f, 38f, 35f); line(11f, 34f, 15f, 41f); line(36f, 33f, 39f, 41f)
        line(8f, 22f, 8f, 28f); bar(19f, 6f, 40f); head(11f, 17f)
    },
    "fly_db" to {
        line(10f, 34f, 38f, 34f); line(12f, 34f, 12f, 42f); line(36f, 34f, 36f, 42f)
        head(33f, 31f); line(28f, 33f, 21f, 26f); dumbbell(9f, 24f); dumbbell(41f, 25f)
    },
    "machine_press" to {
        line(7f, 41f, 41f, 41f); line(10f, 41f, 10f, 12f); line(10f, 12f, 34f, 12f)
        line(34f, 12f, 34f, 21f); line(34f, 21f, 27f, 21f); rect(12f, 29f, 10f, 4f, 2f)
        line(20f, 29f, 20f, 21f); dumbbell(17f, 26f); line(10f, 29f, 10f, 41f)
    },
    "dip" to {
        line(6f, 26f, 42f, 26f); line(6f, 26f, 6f, 41f); line(42f, 26f, 42f, 41f)
        head(24f, 8f); line(24f, 11f, 24f, 26f); line(17f, 18f, 31f, 18f)
        line(17f, 18f, 17f, 26f); line(31f, 18f, 31f, 26f); line(24f, 26f, 24f, 40f)
    },

    // ---------------------------------------------------------------- 背
    "lat_pulldown" to {
        line(9f, 6f, 9f, 40f); line(39f, 6f, 39f, 40f); line(9f, 6f, 39f, 6f)
        line(24f, 6f, 24f, 19f); line(15f, 19f, 33f, 19f); rect(19f, 32f, 10f, 4f, 2f)
        line(24f, 36f, 24f, 40f)
    },
    "seated_row" to {
        line(9f, 13f, 9f, 40f); circle(9f, 16f, 2f); line(9f, 16f, 21f, 23f)
        circle(22f, 23f, 2.4f); rect(21f, 28f, 13f, 4f, 2f); line(27f, 32f, 27f, 40f)
        line(6f, 40f, 42f, 40f)
    },
    "barbell_row" to {
        head(28f, 13f); line(26f, 16f, 18f, 26f); line(18f, 26f, 22f, 39f)
        line(18f, 26f, 12f, 36f); line(25f, 18f, 23f, 33f); bar(33f, 5f, 42f)
    },
    "pullup" to {
        line(7f, 11f, 41f, 11f); line(9f, 11f, 9f, 5f); line(39f, 11f, 39f, 5f)
        circle(18f, 11f, 2.2f); circle(30f, 11f, 2.2f); line(18f, 11f, 20f, 19f)
        line(30f, 11f, 28f, 19f); head(24f, 18f); line(24f, 21f, 24f, 32f)
        line(24f, 32f, 19f, 41f); line(24f, 32f, 29f, 41f)
    },
    "deadlift" to {
        head(26f, 12f); line(26f, 15f, 22f, 25f); line(22f, 25f, 19f, 34f)
        line(22f, 25f, 27f, 34f); line(25f, 17f, 24f, 34f); line(24f, 17f, 28f, 34f)
        bar(35f, 6f, 42f)
    },
    "tbar_row" to {
        bar(32f, 8f, 40f); line(24f, 32f, 24f, 16f); circle(24f, 11f, 3.2f)
        line(16f, 42f, 32f, 42f); line(34f, 32f, 34f, 40f)
    },
    "db_row" to {
        line(9f, 31f, 31f, 31f); line(11f, 31f, 11f, 41f); line(29f, 31f, 29f, 41f)
        head(19f, 22f); line(22f, 25f, 26f, 31f); line(24f, 20f, 33f, 26f); dumbbell(36f, 28f)
    },
    "straight_arm" to {
        rect(36f, 8f, 4f, 33f, 1.5f); circle(38f, 11f, 2f); line(38f, 11f, 22f, 21f)
        head(15f, 19f); line(17f, 22f, 17f, 36f); line(17f, 36f, 13f, 41f)
        line(17f, 36f, 21f, 41f); line(20f, 24f, 22f, 21f); line(6f, 41f, 42f, 41f)
    },
    "back_extension" to {
        line(12f, 41f, 24f, 22f); line(24f, 22f, 38f, 30f); line(38f, 30f, 38f, 41f)
        line(8f, 41f, 14f, 41f); head(12f, 17f); line(16f, 20f, 24f, 22f)
    },

    // ---------------------------------------------------------------- 腿
    "squat" to {
        head(24f, 10f); bar(14f, 5f, 42f); line(24f, 13f, 24f, 16f); line(21f, 18f, 21f, 25f)
        line(21f, 25f, 27f, 31f); line(27f, 31f, 27f, 40f); line(21f, 25f, 16f, 32f)
        line(16f, 32f, 16f, 40f); line(20f, 19f, 28f, 19f)
    },
    "leg_press" to {
        line(5f, 40f, 43f, 40f); line(9f, 36f, 31f, 12f); line(31f, 12f, 38f, 19f)
        line(10f, 29f, 19f, 38f); circle(28f, 18f, 4.5f); circle(34f, 15f, 4.5f)
    },
    "leg_extension" to {
        rect(15f, 27f, 15f, 4f, 2f); line(30f, 27f, 30f, 15f); line(20f, 31f, 34f, 33f)
        circle(37f, 33.5f, 3f); line(10f, 40f, 40f, 40f); line(22f, 31f, 22f, 40f)
    },
    "leg_curl" to {
        line(10f, 29f, 33f, 29f); line(10f, 29f, 10f, 38f); line(33f, 29f, 38f, 23f)
        circle(39.5f, 21.5f, 3f); line(6f, 40f, 42f, 40f); line(24f, 29f, 24f, 38f)
    },
    "calf_raise" to {
        line(6f, 38f, 28f, 38f); line(6f, 38f, 6f, 41f); head(18f, 10f)
        line(18f, 13f, 18f, 27f); line(18f, 27f, 18f, 38f); line(13f, 15f, 23f, 15f)
        line(13f, 9f, 23f, 9f); line(28f, 32f, 34f, 32f)
    },
    "rdl" to {
        head(29f, 15f); line(27f, 18f, 20f, 27f); line(20f, 27f, 21f, 39f)
        line(20f, 27f, 14f, 37f); line(25f, 20f, 26f, 33f); bar(33f, 5f, 42f)
    },
    "hack_squat" to {
        line(8f, 41f, 26f, 12f); line(26f, 12f, 34f, 20f); line(34f, 20f, 34f, 41f)
        line(8f, 41f, 34f, 41f); rect(15f, 26f, 10f, 5f, 2f); line(11f, 41f, 11f, 32f)
    },
    "split_squat" to {
        rect(5f, 31f, 15f, 4f, 2f); line(7f, 35f, 7f, 41f); line(18f, 35f, 18f, 41f)
        head(28f, 11f); line(28f, 14f, 28f, 24f); line(28f, 24f, 22f, 32f)
        line(22f, 32f, 22f, 41f); line(28f, 24f, 33f, 29f); line(33f, 29f, 31f, 35f)
        dumbbell(20f, 27f); dumbbell(36f, 27f)
    },
    "seated_calf" to {
        line(9f, 41f, 41f, 41f); line(13f, 41f, 13f, 28f); line(13f, 28f, 30f, 28f)
        line(30f, 28f, 30f, 35f); rect(23f, 20f, 12f, 5f, 2f); head(19f, 17f)
        line(19f, 20f, 19f, 27f); line(34f, 35f, 34f, 41f)
    },

    // ---------------------------------------------------------------- 臀
    "hip_thrust" to {
        line(27f, 30f, 41f, 30f); line(34f, 30f, 34f, 40f); line(12f, 36f, 26f, 30f)
        circle(9.5f, 37f, 3f); line(26f, 30f, 22f, 25f); bar(24f, 14f, 33f)
        line(6f, 41f, 42f, 41f)
    },
    "hip_abduction" to {
        rect(19f, 28f, 11f, 4f, 2f); line(21f, 32f, 21f, 40f); line(28f, 32f, 28f, 40f)
        line(24f, 29f, 13f, 35f); line(24f, 29f, 35f, 35f)
        circle(12f, 35.5f, 3f); circle(36f, 35.5f, 3f)
    },
    "hip_extension" to {
        line(6f, 41f, 42f, 41f); line(10f, 41f, 24f, 27f); line(24f, 27f, 34f, 33f)
        rect(30f, 23f, 10f, 4f, 2f); dumbbell(16f, 22f); line(34f, 33f, 34f, 41f)
    },
    "cable_kickback" to {
        rect(5f, 8f, 4f, 33f, 1.5f); circle(7f, 11f, 2f); line(7f, 11f, 19f, 24f)
        head(21f, 19f); line(23f, 22f, 23f, 31f); line(23f, 31f, 27f, 40f)
        line(23f, 31f, 37f, 29f); line(18f, 42f, 44f, 42f)
    },

    // ---------------------------------------------------------------- 肩
    "shoulder_press" to {
        head(24f, 15f); line(24f, 18f, 24f, 30f); line(24f, 30f, 20f, 41f)
        line(24f, 30f, 28f, 41f); line(21f, 21f, 18f, 11f); line(27f, 21f, 30f, 11f)
        bar(9f, 7f, 41f)
    },
    "lateral_raise" to {
        head(24f, 12f); line(24f, 15f, 24f, 30f); line(24f, 30f, 20f, 41f)
        line(24f, 30f, 28f, 41f); line(22f, 20f, 11f, 16f); line(26f, 20f, 37f, 16f)
        dumbbell(9f, 16f); dumbbell(39f, 16f)
    },
    "face_pull" to {
        rect(39f, 9f, 4f, 31f, 1.5f); circle(41f, 13f, 2f); line(41f, 13f, 22f, 19f)
        head(15f, 23f); line(17f, 26f, 17f, 39f); line(20f, 20f, 22f, 19f)
        line(24f, 21f, 22f, 23f); line(13f, 41f, 43f, 41f)
    },
    "front_raise" to {
        head(24f, 12f); line(24f, 15f, 24f, 28f); line(24f, 28f, 20f, 40f)
        line(24f, 28f, 28f, 40f); line(22f, 19f, 34f, 16f); dumbbell(37f, 15f)
        line(27f, 19f, 31f, 22f)
    },
    "reverse_fly" to {
        head(24f, 13f); line(24f, 16f, 24f, 29f); line(24f, 29f, 20f, 40f)
        line(24f, 29f, 28f, 40f); line(22f, 20f, 9f, 17f); line(26f, 20f, 39f, 17f)
        dumbbell(7f, 17f); dumbbell(41f, 17f)
    },
    "upright_row" to {
        head(24f, 11f); line(24f, 14f, 24f, 27f); line(24f, 27f, 20f, 40f)
        line(24f, 27f, 28f, 40f); line(21f, 17f, 18f, 23f); line(27f, 17f, 30f, 23f)
        bar(20f, 9f, 37f)
    },

    // ---------------------------------------------------------------- 手臂
    "barbell_curl" to {
        head(24f, 12f); line(24f, 15f, 24f, 30f); line(24f, 30f, 20f, 41f)
        line(24f, 30f, 28f, 41f); line(21f, 19f, 19f, 25f); line(27f, 19f, 29f, 25f)
        bar(25f, 9f, 39f)
    },
    "tricep_pushdown" to {
        rect(8f, 7f, 4f, 33f, 1.5f); circle(10f, 10f, 2f); line(10f, 10f, 26f, 26f)
        head(33f, 15f); line(33f, 18f, 33f, 30f); line(33f, 30f, 29f, 41f)
        line(33f, 30f, 37f, 41f); line(30f, 21f, 26f, 26f)
    },
    "skull_crusher" to {
        line(10f, 30f, 33f, 30f); line(11f, 30f, 11f, 40f); head(14f, 26f)
        line(20f, 26f, 22f, 18f); line(24f, 17f, 32f, 17f); bar(16f, 10f, 36f)
        circle(28f, 26f, 2.6f)
    },
    "close_grip" to {
        line(13f, 29f, 37f, 29f); line(15f, 29f, 15f, 40f); line(35f, 29f, 35f, 40f)
        line(19f, 20f, 19f, 29f); line(29f, 20f, 29f, 29f); line(17f, 20f, 31f, 20f)
        head(13f, 25f)
    },
    "hammer_curl" to {
        head(24f, 11f); line(24f, 14f, 24f, 27f); line(24f, 27f, 20f, 40f)
        line(24f, 27f, 28f, 40f); line(21f, 18f, 18f, 25f); dumbbell(17f, 27f)
        line(27f, 18f, 30f, 25f); dumbbell(31f, 27f)
    },
    "overhead_ext" to {
        head(24f, 15f); line(24f, 18f, 24f, 30f); line(24f, 30f, 20f, 41f)
        line(24f, 30f, 28f, 41f); line(22f, 19f, 27f, 10f); line(27f, 10f, 33f, 13f)
        dumbbell(23f, 6f)
    },
    "preacher_curl" to {
        line(8f, 41f, 40f, 41f); line(24f, 41f, 24f, 25f); line(12f, 25f, 32f, 18f)
        line(24f, 25f, 30f, 22f); dumbbell(36f, 22f)
    },

    // ---------------------------------------------------------------- 核心
    "crunch" to {
        line(5f, 39f, 43f, 39f); head(31f, 23f)
        strokePath(path { moveTo(29f * s, 26f * s); quadraticTo(22f * s, 29f * s, 15f * s, 35f * s) })
        line(15f, 35f, 20f, 39f); line(22f, 28f, 16f, 40f); line(25f, 26f, 19f, 39f)
    },
    "hanging_leg" to {
        line(5f, 8f, 43f, 8f); line(18f, 8f, 20f, 15f); line(30f, 8f, 28f, 15f)
        head(24f, 15f); line(24f, 18f, 24f, 28f); line(24f, 28f, 35f, 32f)
        line(24f, 28f, 34f, 25f)
    },
    "plank" to {
        line(4f, 41f, 44f, 41f); head(8f, 25f); line(11f, 28f, 28f, 31f)
        line(28f, 31f, 38f, 41f); line(15f, 30f, 15f, 41f); line(34f, 33f, 38f, 26f)
    },
    "cable_crunch" to {
        rect(36f, 8f, 4f, 33f, 1.5f); circle(38f, 11f, 2f); line(38f, 11f, 26f, 17f)
        head(20f, 16f); line(20f, 19f, 20f, 27f); line(20f, 27f, 27f, 33f)
        line(12f, 41f, 37f, 41f)
    },

    // ---------------------------------------------------------------- 有氧
    "treadmill" to {
        line(5f, 39f, 35f, 39f); line(7f, 37f, 29f, 31f); line(29f, 31f, 32f, 13f)
        rect(27f, 9f, 10f, 6f, 1.5f); line(30f, 27f, 33f, 19f); line(33f, 39f, 39f, 39f)
        line(39f, 39f, 39f, 34f)
    },
    "outdoor_run" to {
        head(27f, 11f); line(26f, 14f, 21f, 25f); line(23f, 16f, 31f, 13f)
        line(23f, 17f, 14f, 22f); line(21f, 25f, 28f, 32f); line(21f, 25f, 16f, 33f)
        line(21f, 25f, 24f, 36f); line(5f, 41f, 43f, 41f)
    },
    "incline_walk" to {
        line(4f, 40f, 30f, 22f); line(8f, 38f, 33f, 24f); line(33f, 24f, 36f, 10f)
        rect(31f, 6f, 9f, 6f, 1.5f); head(19f, 20f); line(19f, 23f, 17f, 30f)
        line(17f, 30f, 22f, 35f); line(17f, 30f, 14f, 35f); line(19f, 23f, 21f, 30f)
    },
    "elliptical" to {
        line(9f, 39f, 22f, 15f); line(22f, 15f, 32f, 8f); line(22f, 15f, 26f, 38f)
        circle(31f, 33f, 5.5f); line(20f, 30f, 15f, 34f); line(30f, 28f, 36f, 32f)
        line(4f, 41f, 44f, 41f)
    },
    "spin_bike" to {
        circle(32f, 31f, 6.5f); line(32f, 31f, 24f, 17f); line(24f, 17f, 14f, 30f)
        line(14f, 30f, 20f, 32f); rect(17f, 14f, 10f, 3.5f, 1.5f); line(11f, 27f, 11f, 21f)
        line(11f, 21f, 15f, 21f); line(4f, 41f, 44f, 41f)
    },
    "rower" to {
        line(6f, 37f, 41f, 34f); rect(14f, 31f, 10f, 4f, 2f); line(38f, 35f, 41f, 22f)
        circle(39f, 24f, 2f); line(36f, 29f, 28f, 30f); line(6f, 41f, 44f, 41f)
        line(22f, 31f, 26f, 27f)
    },
    "jump_rope" to {
        head(24f, 13f); line(24f, 16f, 24f, 29f); line(24f, 29f, 20f, 40f)
        line(24f, 29f, 28f, 40f); circle(13f, 21f, 2.2f); circle(35f, 21f, 2.2f)
        strokePath(path {
            moveTo(13f * s, 21f * s)
            quadraticTo(10f * s, 44f * s, 24f * s, 44f * s)
            quadraticTo(38f * s, 44f * s, 35f * s, 21f * s)
        })
        line(22f, 18f, 13f, 21f); line(26f, 18f, 35f, 21f)
    },
    "swim" to {
        strokePath(path { wave(33f, s) })
        strokePath(path { wave(40f, s) })
        head(17f, 22f); line(20f, 24f, 29f, 19f); line(16f, 26f, 22f, 31f)
    },
    "stairs" to {
        strokePath(path {
            moveTo(5f * s, 41f * s); lineTo(14f * s, 41f * s); lineTo(14f * s, 35f * s)
            lineTo(23f * s, 35f * s); lineTo(23f * s, 29f * s); lineTo(32f * s, 29f * s)
            lineTo(32f * s, 23f * s); lineTo(42f * s, 23f * s); lineTo(42f * s, 41f * s)
        })
        head(34f, 14f); line(34f, 17f, 34f, 22f)
    },
    "hiit" to {
        fillPath(path {
            moveTo(27f * s, 6f * s); lineTo(13f * s, 26f * s); lineTo(21f * s, 26f * s)
            lineTo(17f * s, 42f * s); lineTo(33f * s, 20f * s); lineTo(24f * s, 20f * s)
            close()
        })
    },

    // ---------------------------------------------------------------- 兜底
    "food" to {
        // 碗：上沿 + 半圆碗身 + 三缕热气
        line(8f, 21f, 40f, 21f)
        strokePath(path {
            moveTo(9f * s, 21f * s)
            quadraticTo(9f * s, 35f * s, 24f * s, 35f * s)
            quadraticTo(39f * s, 35f * s, 39f * s, 21f * s)
        })
        line(16f, 14f, 16f, 8f); line(24f, 14f, 24f, 8f); line(32f, 14f, 32f, 8f)
    },
)

/** 泳道波浪。坐标按 viewBox 给，乘以 s 变成像素。 */
private fun Path.wave(y: Float, s: Float) {
    val step = 8f
    var x = 5f
    moveTo(x * s, y * s)
    var up = true
    while (x < 43f) {
        val nx = (x + step).coerceAtMost(43f)
        quadraticTo((x + step / 2f) * s, (if (up) y - 4f else y + 4f) * s, nx * s, y * s)
        x = nx
        up = !up
    }
}

@Composable
fun ExerciseIcon(
    id: String,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    tint: Color = Color.Unspecified,
) {
    val color = if (tint == Color.Unspecified) LocalContentColor.current else tint
    val draw = icons[id] ?: icons.getValue("food")
    Canvas(modifier.size(size)) {
        val scale = this.size.minDimension / VIEW
        draw(Pen(this, scale, color))
    }
}
