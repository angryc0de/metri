package com.leanlog.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.leanlog.app.core.MealType
import com.leanlog.app.data.MealEntryEntity
import com.leanlog.app.ui.UiState
import com.leanlog.app.ui.components.LCard
import com.leanlog.app.ui.components.calendarSection
import com.leanlog.app.ui.components.Pill
import com.leanlog.app.ui.fmtNum
import com.leanlog.app.ui.theme.LL

@Composable
fun DietScreen(
    state: UiState,
    collapsed: Boolean,
    onToggleCollapse: () -> Unit,
    onSelectDay: (Int) -> Unit,
    onShiftMonth: (Long) -> Unit,
    onAddMeal: (MealType) -> Unit,
    onEditEntry: (MealEntryEntity, MealType) -> Unit,
    onDeleteEntry: (MealEntryEntity) -> Unit,
    onProteinSetting: () -> Unit,
) {
    // 与训练页同一套滚动模型：日历参与滚动，本周那一行吸顶。
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    LazyColumn(
        Modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(bottom = 18.dp),
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
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                IntakeCard(state, onProteinSetting)

                MealType.entries.forEach { meal ->
                    val entries = state.detail.meals.filter { it.meal == meal.name }
                    MealHeader(meal, entries.sumOf { it.kcal }, onAdd = { onAddMeal(meal) })
                    if (entries.isEmpty()) {
                        Box(
                            Modifier.fillMaxWidth()
                                .background(LL.Surface.copy(alpha = 0.5f), RoundedCornerShape(13.dp))
                                .padding(vertical = 14.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("还没有记录", color = LL.Dim, fontSize = 11.sp)
                        }
                    } else {
                        entries.forEach { entry ->
                            MealRow(
                                entry = entry,
                                onClick = { onEditEntry(entry, meal) },
                                onDelete = { onDeleteEntry(entry) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun IntakeCard(state: UiState, onProteinSetting: () -> Unit) {
    val intake = state.detail.intake
    val target = state.tdeeUsed
    val proteinTarget = state.proteinTarget
    val kcalPct = if (target > 0) (intake.kcal / target).coerceIn(0.0, 1.0) else 0.0
    val protPct = if (proteinTarget > 0) (intake.protein / proteinTarget).coerceIn(0.0, 1.0) else 0.0

    LCard {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
            Column(Modifier.weight(1f)) {
                Text("摄入", color = LL.Muted, fontSize = 10.sp)
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("${intake.kcal.toInt()}", color = LL.Text, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                    Text(" / ${target.toInt()} kcal", color = LL.Dim, fontSize = 12.sp)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                val deficit = state.detail.deficit
                // 和训练页同一套口径：没记饮食就不评级，否则"0 摄入"会伪装成一个大缺口
                val recorded = state.detail.hasFood
                val color = when {
                    !recorded -> LL.Dim
                    !state.heatWarn -> LL.Text
                    deficit > 0 -> LL.Green
                    deficit < 0 -> LL.Red
                    else -> LL.Muted
                }
                Text(
                    if (!recorded) "热量缺口" else if (deficit >= 0) "缺口" else "超出",
                    color = LL.Dim, fontSize = 10.sp,
                )
                Text(
                    if (recorded) "${kotlin.math.abs(deficit)} kcal" else "—",
                    color = color, fontSize = 16.sp, fontWeight = FontWeight.Bold,
                )
            }
        }

        Spacer(Modifier.height(10.dp))
        Box(Modifier.fillMaxWidth().height(5.dp).background(LL.Surface2, RoundedCornerShape(3.dp))) {
            Box(
                Modifier.fillMaxWidth(kcalPct.toFloat()).height(5.dp)
                    .background(if (intake.kcal > target) LL.Red else LL.Green, RoundedCornerShape(3.dp))
            )
        }

        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("蛋白质", color = LL.Muted, fontSize = 10.5.sp)
            Spacer(Modifier.size(6.dp))
            Text(
                "${fmtNum(intake.protein)} / $proteinTarget g",
                color = LL.Text, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.size(5.dp))
            Text(
                if (state.profile.proteinAuto) "· 自动" else "· 自定义",
                color = LL.Dim, fontSize = 9.sp,
            )
            Spacer(Modifier.weight(1f))
            // 齿轮：打开蛋白目标设置
            Box(
                Modifier.size(24.dp).background(LL.Surface2, CircleShape).clickable { onProteinSetting() },
                contentAlignment = Alignment.Center,
            ) { GearIcon() }
        }
        Spacer(Modifier.height(6.dp))
        Box(Modifier.fillMaxWidth().height(5.dp).background(LL.Surface2, RoundedCornerShape(3.dp))) {
            Box(
                Modifier.fillMaxWidth(protPct.toFloat()).height(5.dp)
                    .background(LL.Blue, RoundedCornerShape(3.dp))
            )
        }

        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            MacroStat("碳水", intake.carb, LL.Amber)
            MacroStat("脂肪", intake.fat, LL.Purple)
        }
    }
}

@Composable
private fun MacroStat(label: String, value: Double, color: androidx.compose.ui.graphics.Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(5.dp).background(color, CircleShape))
        Spacer(Modifier.size(5.dp))
        Text(label, color = LL.Dim, fontSize = 10.sp)
        Spacer(Modifier.size(4.dp))
        Text("${value.toInt()} g", color = LL.Muted, fontSize = 10.sp)
    }
}

@Composable
private fun GearIcon() {
    androidx.compose.foundation.Canvas(Modifier.size(13.dp)) {
        val c = center
        val r = size.minDimension / 2f
        val stroke = androidx.compose.ui.graphics.drawscope.Stroke(
            width = r * 0.34f,
            cap = androidx.compose.ui.graphics.StrokeCap.Round,
        )
        drawCircle(LL.Muted, radius = r * 0.42f, center = c, style = stroke)
        // 6 个齿
        for (i in 0 until 6) {
            val angle = (Math.PI / 3 * i).toFloat()
            val inner = r * 0.62f
            val outer = r * 0.98f
            drawLine(
                color = LL.Muted,
                start = androidx.compose.ui.geometry.Offset(
                    c.x + inner * kotlin.math.cos(angle),
                    c.y + inner * kotlin.math.sin(angle),
                ),
                end = androidx.compose.ui.geometry.Offset(
                    c.x + outer * kotlin.math.cos(angle),
                    c.y + outer * kotlin.math.sin(angle),
                ),
                strokeWidth = r * 0.34f,
                cap = androidx.compose.ui.graphics.StrokeCap.Round,
            )
        }
    }
}

@Composable
private fun MealHeader(meal: MealType, kcal: Double, onAdd: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(meal.label, color = LL.Text, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        if (kcal > 0) {
            Spacer(Modifier.size(6.dp))
            Text("${kcal.toInt()} kcal", color = LL.Dim, fontSize = 10.sp)
        }
        Spacer(Modifier.weight(1f))
        Box(
            Modifier.size(26.dp).background(LL.Green.copy(alpha = 0.16f), CircleShape).clickable { onAdd() },
            contentAlignment = Alignment.Center,
        ) {
            Text("+", color = LL.Green, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun MealRow(entry: MealEntryEntity, onClick: () -> Unit, onDelete: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().background(LL.Surface, RoundedCornerShape(13.dp))
            .clickable { onClick() }.padding(11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(34.dp).background(LL.Surface2, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
        ) { Text(entry.foodName.take(1), color = LL.Muted, fontSize = 14.sp, fontWeight = FontWeight.Bold) }
        Spacer(Modifier.size(10.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    entry.foodName,
                    color = LL.Text, fontSize = 13.sp, fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (entry.isAi) {
                    Spacer(Modifier.size(6.dp))
                    Pill("AI", LL.Purple)
                }
            }
            Spacer(Modifier.height(2.dp))
            Text(
                buildString {
                    if (entry.servings != null && entry.servingUnit != null) {
                        append("${fmtNum(entry.servings)}${entry.servingUnit} · ")
                    }
                    append("${fmtNum(entry.grams)} g · 蛋白 ${fmtNum(entry.protein)} g")
                },
                color = LL.Muted, fontSize = 10.5.sp,
            )
        }
        Text("${entry.kcal.toInt()}", color = LL.Text, fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
        Text(" kcal", color = LL.Dim, fontSize = 9.sp)
        Spacer(Modifier.size(8.dp))
        Box(Modifier.size(22.dp).clickable { onDelete() }, contentAlignment = Alignment.Center) {
            Text("×", color = LL.Dim, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
    }
}
