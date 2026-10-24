package com.leanlog.app.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

/**
 * 一套完整的皮肤。
 *
 * 语义色（绿=缺口、红=超出、蓝=力量、黄=有氧）在白天模式下**保持色相不变，只降明度**：
 * #3DDC84 这种荧光绿放在白底上对比度不到 1.5:1，日期数字会糊成一片，
 * 但换成深绿又会丢掉"绿=缺口"的直觉，所以只压亮度不换色。
 */
data class Palette(
    val isDark: Boolean,
    val bg: Color,
    val surface: Color,
    val surface2: Color,
    val surface3: Color,
    val line: Color,
    val line2: Color,
    val text: Color,
    val muted: Color,
    val dim: Color,
    val faint: Color,
    /** 极淡的描边/底色。深色用白、浅色用黑，透明叠层才不会在换肤后消失。 */
    val hair: Color,
    val future: Color,
    val green: Color,
    val greenDeep: Color,
    val red: Color,
    val redDeep: Color,
    val blue: Color,
    val purple: Color,
    val amber: Color,
    /** 绿/蓝/黄实心块上面的文字色 */
    val onAccent: Color,
    val scrim: Color,
)

object LL {

    val Dark = Palette(
        isDark = true,
        bg = Color(0xFF0B0D10),
        surface = Color(0xFF14171C),
        surface2 = Color(0xFF1B1F26),
        surface3 = Color(0xFF22272F),
        line = Color(0xFF262B34),
        line2 = Color(0xFF313742),
        text = Color(0xFFE9ECF1),
        muted = Color(0xFF8A919E),
        dim = Color(0xFF5D6470),
        faint = Color(0xFF3A414C),
        hair = Color(0xFFFFFFFF),
        future = Color(0xFF2E343D),
        green = Color(0xFF3DDC84),
        greenDeep = Color(0xFF1C3B2A),
        red = Color(0xFFFF5C5C),
        redDeep = Color(0xFF3B1E22),
        blue = Color(0xFF5B8CFF),
        purple = Color(0xFFB07CFF),
        amber = Color(0xFFFFC247),
        onAccent = Color(0xFF04150C),
        scrim = Color(0xFF000000),
    )

    val Light = Palette(
        isDark = false,
        bg = Color(0xFFFFFFFF),
        surface = Color(0xFFF6F7F9),
        surface2 = Color(0xFFEDEFF3),
        surface3 = Color(0xFFE2E5EA),
        line = Color(0xFFE5E8EC),
        line2 = Color(0xFFD4D9E0),
        text = Color(0xFF14171C),
        muted = Color(0xFF5B636F),
        dim = Color(0xFF8B939F),
        faint = Color(0xFFC2C8D0),
        hair = Color(0xFF000000),
        future = Color(0xFFD8DCE2),
        green = Color(0xFF0F9D58),
        greenDeep = Color(0xFFDCF3E6),
        red = Color(0xFFD93A3E),
        redDeep = Color(0xFFFBE3E3),
        blue = Color(0xFF3C6FE0),
        purple = Color(0xFF7C5CE0),
        amber = Color(0xFFC07C05),
        onAccent = Color(0xFFFFFFFF),
        scrim = Color(0xFF000000),
    )

    // 换肤靠读这个 state：所有 getter 都走它，所以 LL.Xxx 的用法一个字都不用改。
    private var palette by mutableStateOf(Dark)

    val isDark: Boolean get() = palette.isDark

    fun use(dark: Boolean) {
        palette = if (dark) Dark else Light
    }

    val Bg: Color get() = palette.bg
    val Surface: Color get() = palette.surface
    val Surface2: Color get() = palette.surface2
    val Surface3: Color get() = palette.surface3
    val Line: Color get() = palette.line
    val Line2: Color get() = palette.line2

    val Text: Color get() = palette.text
    val Muted: Color get() = palette.muted
    val Dim: Color get() = palette.dim
    val Faint: Color get() = palette.faint
    val Hair: Color get() = palette.hair
    val Future: Color get() = palette.future

    val Green: Color get() = palette.green
    val GreenDeep: Color get() = palette.greenDeep
    val Red: Color get() = palette.red
    val RedDeep: Color get() = palette.redDeep
    val Blue: Color get() = palette.blue
    val Purple: Color get() = palette.purple
    val Amber: Color get() = palette.amber

    val OnAccent: Color get() = palette.onAccent
    val Scrim: Color get() = palette.scrim

    /** 力量圆点 */
    val StrengthDot: Color get() = palette.blue

    /** 有氧圆点（用户要求：黄色，不用紫色） */
    val CardioDot: Color get() = palette.amber
}
