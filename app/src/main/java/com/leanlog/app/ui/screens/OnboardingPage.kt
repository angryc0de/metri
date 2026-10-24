package com.leanlog.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.leanlog.app.core.Gender
import com.leanlog.app.core.LeanLogMath
import com.leanlog.app.ui.components.PrimaryButton
import com.leanlog.app.ui.components.WheelPicker
import com.leanlog.app.ui.theme.LL

/**
 * 首次引导。只做一件事：在记第一条数据之前，把算 BMR 必需的四个值拿到手。
 *
 * 只问**必填**的性别/年龄/身高/体重。活动系数、目标体重、减重速度全部给合理默认值 ——
 * 引导页问得越多，用户填得越敷衍，而这几项恰恰是「我」页里随时能改的。
 *
 * 三个滚轮并排而不是竖排折叠：折叠要么引入同向嵌套滚动，要么把按钮顶出屏幕，
 * 并排是一屏之内能放下的唯一干净解法（和训练参数的重量/次数/组数一致）。
 */
@Composable
fun OnboardingPage(onDone: (String, Gender, Int, Double, Double) -> Unit) {
    var name by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf(Gender.MALE) }
    var age by remember { mutableStateOf(28) }
    var height by remember { mutableStateOf(175) }
    var weight by remember { mutableStateOf(75) }

    val bmr = LeanLogMath.bmr(gender, weight.toDouble(), height.toDouble(), age)
    // 和个人资料默认值保持一致，避免"引导页看到一个数、进主页又变成另一个数"
    val tdee = LeanLogMath.tdeeBase(bmr, 1.2)

    Column(
        Modifier.fillMaxSize().background(LL.Bg)
            .statusBarsPadding().navigationBarsPadding()
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(34.dp))
        Text(
            "Metri", color = LL.Text, fontSize = 13.sp,
            fontWeight = FontWeight.Black, fontStyle = FontStyle.Italic,
        )
        Spacer(Modifier.height(12.dp))
        Text("先了解一下你", color = LL.Text, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(9.dp))
        Text(
            "这几个数决定你的基础代谢和每日热量缺口。之后随时能在「我 → 个人信息」里改。",
            color = LL.Muted, fontSize = 12.sp, lineHeight = 19.sp,
        )

        Spacer(Modifier.height(18.dp))

        Text("称呼", color = LL.Dim, fontSize = 10.sp)
        Spacer(Modifier.height(6.dp))
        Box(
            Modifier.fillMaxWidth().height(44.dp)
                .background(LL.Surface, RoundedCornerShape(13.dp))
                .padding(horizontal = 14.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            BasicTextField(
                value = name,
                onValueChange = { if (it.length <= 12) name = it },
                singleLine = true,
                textStyle = TextStyle(color = LL.Text, fontSize = 14.sp, fontWeight = FontWeight.Medium),
                cursorBrush = SolidColor(LL.Green),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { inner ->
                    if (name.isEmpty()) {
                        Text("怎么称呼你（可留空）", color = LL.Dim, fontSize = 12.5.sp)
                    }
                    inner()
                },
            )
        }

        Spacer(Modifier.height(16.dp))

        Text("性别", color = LL.Dim, fontSize = 10.sp)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GenderChip("男", gender == Gender.MALE, Modifier.weight(1f)) { gender = Gender.MALE }
            GenderChip("女", gender == Gender.FEMALE, Modifier.weight(1f)) { gender = Gender.FEMALE }
        }

        Spacer(Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            WheelColumn("年龄", "岁", (10..100).map { "$it" }, age - 10, Modifier.weight(1f)) {
                age = it + 10
            }
            WheelColumn("身高", "cm", (130..220).map { "$it" }, height - 130, Modifier.weight(1f)) {
                height = it + 130
            }
            WheelColumn("体重", "kg", (35..150).map { "$it" }, weight - 35, Modifier.weight(1f)) {
                weight = it + 35
            }
        }

        Spacer(Modifier.height(18.dp))

        // 这段不只是填空白：三个"为什么值得填"的答案，
        // 比单纯说"请输入资料"更容易让人愿意把数填准。
        Column(
            Modifier.fillMaxWidth()
                .background(LL.Surface, RoundedCornerShape(14.dp))
                .padding(horizontal = 14.dp, vertical = 13.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            IntroLine("记完训练和饮食，日历用颜色告诉你当天有没有缺口")
            IntroLine("数据全部存在本机，断网也能记录和查看")
            IntroLine("AI 估算可选，填了自己的 Key 才会联网")
        }

        Spacer(Modifier.weight(1f))

        // 实时反馈：让用户当场看到这几个数是怎么变成一个消耗值的，而不是填完就没了
        Row(
            Modifier.fillMaxWidth().background(LL.Surface, RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("基础代谢 ${bmr.toInt()}", color = LL.Muted, fontSize = 11.sp)
            Spacer(Modifier.weight(1f))
            Text("每日消耗约 ${tdee.toInt()} kcal", color = LL.Green, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }

        Spacer(Modifier.height(14.dp))
        PrimaryButton("开始使用", Modifier.fillMaxWidth()) {
            onDone(name.trim(), gender, age, height.toDouble(), weight.toDouble())
        }
        Spacer(Modifier.height(18.dp))
    }
}

@Composable
private fun GenderChip(text: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .height(46.dp)
            .background(if (selected) LL.Green else LL.Surface, RoundedCornerShape(13.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            color = if (selected) LL.OnAccent else LL.Muted,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun WheelColumn(
    label: String,
    unit: String,
    items: List<String>,
    index: Int,
    modifier: Modifier = Modifier,
    onIndexChange: (Int) -> Unit,
) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("$label · $unit", color = LL.Dim, fontSize = 10.sp)
        Spacer(Modifier.height(5.dp))
        Box(
            Modifier.fillMaxWidth()
                .background(LL.Surface, RoundedCornerShape(14.dp))
                .padding(vertical = 5.dp),
        ) {
            WheelPicker(
                items = items,
                index = index.coerceIn(0, (items.size - 1).coerceAtLeast(0)),
                // 默认 32dp 在 450dpi 上偏挤，放大到 40dp 手指更好停
                itemHeight = 40.dp,
                onIndexChange = onIndexChange,
            )
        }
    }
}

@Composable
private fun IntroLine(text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            Modifier.padding(top = 6.dp).size(4.dp).background(LL.Green, CircleShape)
        )
        Spacer(Modifier.size(9.dp))
        Text(text, color = LL.Muted, fontSize = 11.5.sp, lineHeight = 17.sp)
    }
}
