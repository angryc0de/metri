package com.leanlog.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.leanlog.app.domain.CardioParams
import com.leanlog.app.ui.UiState
import com.leanlog.app.ui.components.Dot
import com.leanlog.app.ui.components.EmptyHint
import com.leanlog.app.ui.components.LCard
import com.leanlog.app.ui.components.calendarSection
import com.leanlog.app.ui.components.Pill
import com.leanlog.app.ui.components.SectionTitle
import com.leanlog.app.ui.fmtNum
import com.leanlog.app.ui.icons.ExerciseIcon
import com.leanlog.app.ui.theme.LL
import com.leanlog.app.core.SeedData

@Composable
fun TrainScreen(
    state: UiState,
    collapsed: Boolean,
    onToggleCollapse: () -> Unit,
    onSelectDay: (Int) -> Unit,
    onShiftMonth: (Long) -> Unit,
    onAddStrength: () -> Unit,
    onAddCardio: () -> Unit,
    onEditStrength: (Long) -> Unit,
    onEditCardio: (Long) -> Unit,
    onDeleteStrength: (Long) -> Unit,
    onDeleteCardio: (Long) -> Unit,
) {
    // 日历和记录共用同一个滚动容器：上滑时日历整体让位，只有本周那一行吸住。
    // 之前日历是页面顶部的固定区块，6 行月份能吃掉半屏，当天记录根本看不全。
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    LazyColumn(
        Modifier.fillMaxSize(),
        state = listState,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 18.dp),
    ) {
        calendarSection(
            month = state.month,
            cells = state.cells,
            selectedDay = state.selectedDay,
            heatWarn = state.heatWarn,
            collapsed = collapsed,
            listState = listState,
            onSelect = onSelectDay,
            onShiftMonth = onShiftMonth,
            onToggleCollapse = onToggleCollapse,
        )

        item(key = "content") {
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                DayStatCard(state)

                SectionWithAdd(
                    title = "力量训练",
                    count = state.detail.strength.size,
                    tint = LL.StrengthDot,
                    onAdd = onAddStrength,
                )
                if (state.detail.strength.isEmpty()) {
                    EmptyHint("还没有力量记录，点上面的 + 添加")
                } else {
                    state.detail.strength.forEach { record ->
                        StrengthRow(
                            name = record.exerciseName,
                            exerciseId = record.exerciseId,
                            weight = if (record.bodyweight) null else record.weightKg,
                            reps = record.reps,
                            sets = record.sets,
                            netKcal = record.netKcal,
                            onClick = { onEditStrength(record.id) },
                            onDelete = { onDeleteStrength(record.id) },
                        )
                    }
                }

                SectionWithAdd(
                    title = "有氧训练",
                    count = state.detail.cardio.size,
                    tint = LL.CardioDot,
                    onAdd = onAddCardio,
                )
                if (state.detail.cardio.isEmpty()) {
                    EmptyHint("还没有有氧记录")
                } else {
                    state.detail.cardio.forEach { record ->
                        val type = SeedData.cardioById(record.typeId)
                        CardioRow(
                            typeId = record.typeId,
                            name = record.typeName,
                            summary = type?.let {
                                CardioParams.summary(it, CardioParams.decode(record.paramsJson), record.enumValue)
                            } ?: "—",
                            netKcal = record.netKcal,
                            onClick = { onEditCardio(record.id) },
                            onDelete = { onDeleteCardio(record.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DayStatCard(state: UiState) {
    val detail = state.detail
    val tdee = state.tdeeUsed
    LCard {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(com.leanlog.app.ui.formatDayTitle(detail.day), color = LL.Text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text(
                    "基础消耗 ${tdee.toInt()} kcal" + if (detail.hasWorkout) " · 训练净消耗 ${detail.workoutNet.toInt()} kcal" else "",
                    color = LL.Muted, fontSize = 10.5.sp,
                )
            }
            Spacer(Modifier.weight(1f))
            Column(horizontalAlignment = Alignment.End) {
                val deficit = detail.deficit
                // 没记饮食时不能拿"摄入 0"去算缺口，否则每天一睁眼就是 2000 大卡绿色缺口。
                // 只有真正记过东西的日子才评级 —— 这是 PRD 里那条「有记录」判定剔除训练的落地。
                val recorded = detail.hasFood
                val color = when {
                    !recorded -> LL.Dim
                    !state.heatWarn -> LL.Text
                    deficit > 0 -> LL.Green
                    deficit < 0 -> LL.Red
                    else -> LL.Muted
                }
                // 「缺口」这个词本身已经含了负号的意思，数字再带 "-" 等于说了两遍。
                // 超出时反过来换成「超出」，避免出现「热量缺口 -300」这种自相矛盾的写法。
                Text(if (deficit >= 0) "热量缺口" else "热量超出", color = LL.Muted, fontSize = 10.sp)
                Text(
                    if (recorded) "${kotlin.math.abs(deficit)} kcal" else "未记录",
                    color = color,
                    fontSize = if (recorded) 17.sp else 14.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        if (state.week.recordedDays > 0) {
            Spacer(Modifier.height(10.dp))
            Row(
                Modifier.fillMaxWidth().background(LL.Surface2, RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("本周", color = LL.Dim, fontSize = 10.sp)
                Spacer(Modifier.size(8.dp))
                Text(
                    "平均缺口 ${state.week.avgDeficit ?: 0}",
                    color = LL.Muted, fontSize = 11.sp, fontWeight = FontWeight.Medium,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    "达标 ${state.week.hitDays}/${state.week.recordedDays} 天",
                    // 关掉缺口警示后整页都不该再出现绿色，否则"关了还在提醒"
                    color = if (state.heatWarn) LL.Green else LL.Muted,
                    fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.size(4.dp))
                Text("（目标 ${state.week.target}）", color = LL.Dim, fontSize = 9.sp)
            }
        }
    }
}

@Composable
private fun SectionWithAdd(title: String, count: Int, tint: Color, onAdd: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Dot(tint, 6.dp)
        Spacer(Modifier.size(6.dp))
        Text(title, color = LL.Text, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        if (count > 0) {
            Spacer(Modifier.size(6.dp))
            Text("$count 项", color = LL.Dim, fontSize = 10.sp)
        }
        Spacer(Modifier.weight(1f))
        Box(
            Modifier.size(26.dp).background(tint.copy(alpha = 0.16f), CircleShape).clickable { onAdd() },
            contentAlignment = Alignment.Center,
        ) {
            Text("+", color = tint, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun StrengthRow(
    name: String,
    exerciseId: String,
    weight: Double?,
    reps: Int,
    sets: Int,
    netKcal: Double,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().background(LL.Surface, RoundedCornerShape(14.dp))
            .clickable { onClick() }.padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(38.dp).background(LL.Surface2, RoundedCornerShape(11.dp)),
            contentAlignment = Alignment.Center,
        ) { ExerciseIcon(exerciseId, size = 26.dp, tint = LL.Muted) }
        Spacer(Modifier.size(11.dp))
        Column(Modifier.weight(1f)) {
            Text(name, color = LL.Text, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(2.dp))
            Text(
                buildString {
                    append(if (weight == null) "自重" else "${fmtNum(weight)} kg")
                    append(" × $reps 次 × $sets 组")
                },
                color = LL.Muted, fontSize = 11.sp,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            // 数字 + 单位分两段：单位跟饮食页一样用小一号的灰字（9sp / LL.Dim），
            // 别写成同一串 —— 否则数字和单位一样重，读起来像一串编号。
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${netKcal.toInt()}", color = LL.Blue, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(" kcal", color = LL.Dim, fontSize = 9.sp)
            }
            Text("净消耗", color = LL.Dim, fontSize = 9.sp)
        }
        Spacer(Modifier.size(10.dp))
        DeleteDot(onDelete)
    }
}

@Composable
private fun CardioRow(
    typeId: String,
    name: String,
    summary: String,
    netKcal: Double,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().background(LL.Surface, RoundedCornerShape(14.dp))
            .clickable { onClick() }.padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(38.dp).background(LL.Surface2, RoundedCornerShape(11.dp)),
            contentAlignment = Alignment.Center,
        ) { ExerciseIcon(typeId, size = 26.dp, tint = LL.Muted) }
        Spacer(Modifier.size(11.dp))
        Column(Modifier.weight(1f)) {
            Text(name, color = LL.Text, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(2.dp))
            Text(summary, color = LL.Muted, fontSize = 11.sp)
        }
        Column(horizontalAlignment = Alignment.End) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${netKcal.toInt()}", color = LL.Amber, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(" kcal", color = LL.Dim, fontSize = 9.sp)
            }
            Text("净消耗", color = LL.Dim, fontSize = 9.sp)
        }
        Spacer(Modifier.size(10.dp))
        DeleteDot(onDelete)
    }
}

@Composable
private fun DeleteDot(onDelete: () -> Unit) {
    Box(
        Modifier.size(24.dp).clickable { onDelete() },
        contentAlignment = Alignment.Center,
    ) {
        Text("×", color = LL.Dim, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}
