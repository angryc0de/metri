package com.leanlog.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.leanlog.app.core.SeedData
import com.leanlog.app.data.WeightLogEntity
import com.leanlog.app.ui.ProfileField
import com.leanlog.app.ui.UiState
import com.leanlog.app.ui.components.LCard
import com.leanlog.app.ui.components.ToggleSwitch
import com.leanlog.app.ui.fmtNum
import com.leanlog.app.ui.theme.LL

@Composable
fun MeScreen(
    state: UiState,
    onEditProfile: (ProfileField) -> Unit,
    onProteinSetting: () -> Unit,
    onToggleHeatWarn: () -> Unit,
    onTheme: () -> Unit,
    onAiConfig: () -> Unit,
    onExport: () -> Unit,
    onToast: (String) -> Unit,
) {
    val profile = state.profile
    val latest = state.weights.lastOrNull()
    val prev = state.weights.dropLast(1).lastOrNull()
    val delta = if (latest != null && prev != null) latest.kg - prev.kg else 0.0

    LazyColumn(
        Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 14.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            LCard {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                    Column(Modifier.weight(1f)) {
                        Text("当前体重", color = LL.Muted, fontSize = 10.sp)
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                fmtNum(profile.weightKg),
                                color = LL.Text, fontSize = 28.sp, fontWeight = FontWeight.Bold,
                            )
                            Text(" kg", color = LL.Dim, fontSize = 12.sp)
                            if (latest != null && prev != null) {
                                Spacer(Modifier.size(8.dp))
                                Text(
                                    (if (delta > 0) "+" else "") + String.format("%.1f", delta),
                                    color = if (delta <= 0) LL.Green else LL.Red,
                                    fontSize = 12.sp, fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                        Text(
                            "目标 ${fmtNum(profile.targetWeightKg)} kg · 每周 ${fmtNum(profile.weeklyRateKg)} kg",
                            color = LL.Muted, fontSize = 10.5.sp,
                        )
                    }
                    Box(
                        Modifier.background(LL.Green.copy(alpha = 0.16f), RoundedCornerShape(10.dp))
                            .clickable { onEditProfile(ProfileField.WEIGHT) }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    ) {
                        Text("记录体重", color = LL.Green, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                if (state.weights.size >= 2) {
                    Spacer(Modifier.height(14.dp))
                    WeightChart(state.weights)
                    Spacer(Modifier.height(6.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        ChartLegend(LL.Green, "日体重")
                        ChartLegend(LL.Blue, "7 日平均")
                        Spacer(Modifier.weight(1f))
                        Text("${state.weights.size} 条记录", color = LL.Dim, fontSize = 9.sp)
                    }
                } else {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "再记录几天体重，这里会出现趋势曲线。单日体重波动 ±1kg 多半是水，看趋势才有意义。",
                        color = LL.Dim, fontSize = 10.sp,
                    )
                }
            }
        }

        item {
            LCard {
                Text("能量消耗估算", color = LL.Text, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    EnergyStat("基础代谢", state.tdeeFormula / profile.activityFactor, LL.Text)
                    EnergyStat("公式消耗", state.tdeeFormula, LL.Muted)
                    EnergyStat(
                        "实测消耗",
                        state.tdeeAdaptive ?: 0.0,
                        if (state.tdeeAdaptive != null) LL.Green else LL.Dim,
                        showUnknown = state.tdeeAdaptive == null,
                    )
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    state.adaptiveNote
                        ?: "实测消耗由最近 30 天的「摄入 − 体重变化」反推，比公式更贴近你自己。日历颜色用的是这个值。",
                    color = if (state.adaptiveNote != null) LL.Amber else LL.Dim,
                    fontSize = 10.sp,
                )
            }
        }

        item {
            ProfileGroup("个人信息") {
                MeRow("姓名", profile.name.ifBlank { "未填写" }) { onEditProfile(ProfileField.NAME) }
                MeRow("性别", if (profile.gender == "FEMALE") "女" else "男") { onEditProfile(ProfileField.GENDER) }
                MeRow("年龄", "${profile.age} 岁") { onEditProfile(ProfileField.AGE) }
                MeRow("身高", "${fmtNum(profile.heightCm)} cm") { onEditProfile(ProfileField.HEIGHT) }
                MeRow("日常活动量", activityLabel(profile.activityFactor)) {
                    onEditProfile(ProfileField.ACTIVITY)
                }
                MeRow("目标体重", "${fmtNum(profile.targetWeightKg)} kg") {
                    onEditProfile(ProfileField.TARGET_WEIGHT)
                }
                MeRow("每周减重", "${fmtNum(profile.weeklyRateKg)} kg") { onEditProfile(ProfileField.RATE) }
                MeRow(
                    "每日蛋白目标",
                    "${state.proteinTarget} g · " + if (profile.proteinAuto) "自动" else "自定义",
                ) { onProteinSetting() }
            }
        }

        item {
            ProfileGroup("设置") {
                MeRow("外观", if (state.darkTheme) "黑夜模式" else "白天模式") { onTheme() }
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("热量缺口警示", color = LL.Text, fontSize = 12.5.sp, fontWeight = FontWeight.Medium)
                        Text(
                            "关闭后日历不再按缺口标绿/红，只保留中性数字",
                            color = LL.Dim, fontSize = 9.5.sp,
                        )
                    }
                    ToggleSwitch(checked = state.heatWarn) { onToggleHeatWarn() }
                }
                MeRow("AI 模型", "OpenAI 兼容接口") { onAiConfig() }
                MeRow("数据导出", "JSON 备份 + CSV") { onExport() }
                MeRow("关于", "Metri v1.0 · 本地记录 · 无账号") {
                    onToast("所有数据只存在这台设备上，没有账号也没有云端")
                }
            }
        }
    }
}

private fun activityLabel(factor: Double): String {
    val level = SeedData.activityLevels.minByOrNull { kotlin.math.abs(it.factor - factor) }
    return level?.label ?: "久坐"
}

@Composable
private fun ChartLegend(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(5.dp).background(color, CircleShape))
        Spacer(Modifier.size(4.dp))
        Text(label, color = LL.Dim, fontSize = 9.sp)
    }
}

@Composable
private fun EnergyStat(label: String, value: Double, color: Color, showUnknown: Boolean = false) {
    Column {
        Text(label, color = LL.Dim, fontSize = 9.5.sp)
        Spacer(Modifier.height(3.dp))
        Text(
            if (showUnknown) "待积累" else "${value.toInt()}",
            color = color,
            fontSize = if (showUnknown) 11.sp else 15.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

/**
 * 体重曲线 + 7 日移动平均。
 * 单点波动全是水，所以必须有一条平滑线 —— 否则用户会为 0.8kg 的日常波动焦虑。
 */
@Composable
private fun WeightChart(weights: List<WeightLogEntity>) {
    val points = weights.takeLast(30)
    val values = points.map { it.kg }
    val min = values.min() - 0.4
    val max = values.max() + 0.4
    val span = (max - min).coerceAtLeast(0.1)

    val ma = movingAverage(values, 7)

    Canvas(Modifier.fillMaxWidth().height(96.dp)) {
        val w = size.width
        val h = size.height
        fun x(i: Int) = if (values.size <= 1) 0f else w * i / (values.size - 1).toFloat()
        fun y(v: Double) = h - ((v - min) / span * h).toFloat()

        // 网格
        for (i in 0..2) {
            val gy = h * i / 2f
            drawLine(LL.Line.copy(alpha = 0.7f), Offset(0f, gy), Offset(w, gy), strokeWidth = 1f)
        }

        // 日体重
        val raw = Path()
        values.forEachIndexed { i, v ->
            if (i == 0) raw.moveTo(x(i), y(v)) else raw.lineTo(x(i), y(v))
        }
        drawPath(raw, LL.Green.copy(alpha = 0.55f), style = Stroke(width = 1.6f, cap = StrokeCap.Round))

        // 7 日均线
        val smooth = Path()
        var started = false
        ma.forEachIndexed { i, v ->
            if (v == null) return@forEachIndexed
            if (!started) {
                smooth.moveTo(x(i), y(v)); started = true
            } else {
                smooth.lineTo(x(i), y(v))
            }
        }
        drawPath(smooth, LL.Blue, style = Stroke(width = 2.2f, cap = StrokeCap.Round))

        // 末点
        if (values.isNotEmpty()) {
            drawCircle(LL.Blue, radius = 3.2f, center = Offset(x(values.lastIndex), y(values.last())))
        }
    }
}

private fun movingAverage(values: List<Double>, window: Int): List<Double?> = values.indices.map { i ->
    if (i < window - 1) null else values.subList(i - window + 1, i + 1).average()
}

@Composable
private fun ProfileGroup(title: String, content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    LCard {
        Text(title, color = LL.Muted, fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(4.dp))
        Column(content = content)
    }
}

@Composable
private fun MeRow(label: String, value: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = LL.Text, fontSize = 12.5.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.weight(1f))
        Text(value, color = LL.Muted, fontSize = 11.5.sp)
        Spacer(Modifier.size(6.dp))
        Text("›", color = LL.Faint, fontSize = 13.sp)
    }
}
