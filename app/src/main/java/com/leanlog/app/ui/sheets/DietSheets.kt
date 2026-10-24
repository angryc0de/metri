package com.leanlog.app.ui.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.leanlog.app.core.FoodEstimate
import com.leanlog.app.core.MealType
import com.leanlog.app.data.FoodEntity
import com.leanlog.app.ui.AppViewModel
import com.leanlog.app.ui.PortionDraft
import com.leanlog.app.ui.components.FullScreenPage
import com.leanlog.app.ui.components.GhostButton
import com.leanlog.app.ui.components.PrimaryButton
import com.leanlog.app.ui.components.Pill
import com.leanlog.app.ui.components.SearchField
import com.leanlog.app.ui.components.SheetShell
import com.leanlog.app.ui.components.ToggleSwitch
import com.leanlog.app.ui.components.WheelPicker
import com.leanlog.app.ui.fmtNum
import com.leanlog.app.ui.theme.LL

// ------------------------------------------------------------------ 食物搜索

@Composable
fun FoodSearchSheet(
    vm: AppViewModel,
    meal: MealType,
    query: String,
    results: List<FoodEntity>,
    recent: List<FoodEntity>,
    onDismiss: () -> Unit,
) {
    SheetShell(
        title = "添加到${meal.label}",
        subtitle = "输入食物名，或直接描述「牛肉减脂便当」",
        onDismiss = onDismiss,
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp)) {
            SearchField(query, "搜索食物 / 描述这一餐", onValueChange = { vm.searchFoods(it) })
            if (query.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                Box(
                    Modifier.background(LL.Purple.copy(alpha = 0.16f), RoundedCornerShape(10.dp))
                        .clickable { vm.runAiSearch(query, meal) }
                        .padding(horizontal = 11.dp, vertical = 7.dp),
                ) {
                    Text("✨ 让 AI 拆解「$query」", color = LL.Purple, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 14.dp, end = 14.dp, bottom = 24.dp),
        ) {
            if (query.isBlank() && recent.isNotEmpty()) {
                item {
                    Text("最近吃过 · 点一下直接记", color = LL.Dim, fontSize = 10.sp)
                    Spacer(Modifier.height(7.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        items(recent.size) { i ->
                            val food = recent[i]
                            Box(
                                Modifier.background(LL.Surface2, RoundedCornerShape(10.dp))
                                    .clickable { vm.quickAddFood(food, meal) }
                                    .padding(horizontal = 11.dp, vertical = 8.dp),
                            ) {
                                Text(
                                    "${food.emoji ?: "🍽"} ${food.name}",
                                    color = LL.Text, fontSize = 11.5.sp,
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }
            }

            if (results.isEmpty() && query.isNotBlank()) {
                item {
                    Column(
                        Modifier.fillMaxWidth().background(LL.Surface2, RoundedCornerShape(12.dp)).padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text("本地库没有匹配", color = LL.Muted, fontSize = 12.sp)
                        Spacer(Modifier.height(4.dp))
                        Text("点上面的「让 AI 拆解」估算这一餐", color = LL.Dim, fontSize = 10.sp)
                    }
                }
            }

            items(results.size) { index ->
                val food = results[index]
                Row(
                    Modifier.fillMaxWidth().clickable { vm.openPortion(food, meal) }
                        .padding(vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier.size(32.dp).background(LL.Surface2, RoundedCornerShape(9.dp)),
                        contentAlignment = Alignment.Center,
                    ) { Text(food.emoji ?: "🍽", fontSize = 15.sp) }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(food.name, color = LL.Text, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            if (food.source != "BUILTIN") {
                                Spacer(Modifier.width(6.dp))
                                Pill(if (food.source == "AI") "AI" else "自建", LL.Purple)
                            }
                        }
                        Text(
                            "${food.kcalPer100g.toInt()} kcal / 100g" +
                                (food.servingUnit?.let { " · 1$it ≈ ${fmtNum(food.servingGrams ?: 0.0)}g" } ?: ""),
                            color = LL.Dim, fontSize = 9.5.sp,
                        )
                    }
                    Text("›", color = LL.Faint, fontSize = 14.sp)
                }
            }
        }
    }
}

// ------------------------------------------------------------------ 份量

@Composable
fun PortionSheet(
    vm: AppViewModel,
    draft: PortionDraft,
    onDismiss: () -> Unit,
) {
    var byUnit by remember { mutableStateOf(draft.servingUnit != null) }
    val unit = draft.servingUnit ?: "份"
    val unitGrams = draft.servingGrams ?: 100.0

    var qtyText by remember { mutableStateOf(if (byUnit) "1" else fmtNum(draft.initialGrams)) }
    val qty = qtyText.toDoubleOrNull() ?: 0.0
    val grams = if (byUnit) qty * unitGrams else qty
    val kcal = draft.kcalPer100g * grams / 100.0
    val protein = draft.proteinPer100g * grams / 100.0

    SheetShell(
        title = draft.name,
        subtitle = "${draft.kcalPer100g.toInt()} kcal / 100g",
        onDismiss = onDismiss,
        heightFraction = 0.62f,
    ) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("按${if (byUnit) "份" else "克"}记录", color = LL.Text, fontSize = 12.sp)
                Spacer(Modifier.weight(1f))
                ToggleSwitch(checked = byUnit) { toUnit ->
                    // 切换单位时保持「同一份食物」的克数不变，否则 1 份会突然变成 1 克
                    val currentGrams = if (byUnit) qty * unitGrams else qty
                    byUnit = toUnit
                    qtyText = if (toUnit) fmtNum(currentGrams / unitGrams) else fmtNum(currentGrams)
                }
            }

            Spacer(Modifier.height(12.dp))
            Row(
                Modifier.fillMaxWidth().background(LL.Surface2, RoundedCornerShape(13.dp)).padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(if (byUnit) "数量（$unit）" else "克数（g）", color = LL.Muted, fontSize = 9.5.sp)
                    Row(verticalAlignment = Alignment.Bottom) {
                        BasicTextField(
                            value = qtyText,
                            onValueChange = { qtyText = it.filter { c -> c.isDigit() || c == '.' } },
                            singleLine = true,
                            textStyle = TextStyle(color = LL.Text, fontSize = 24.sp, fontWeight = FontWeight.Bold),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.width(96.dp),
                        )
                        Text(if (byUnit) unit else "g", color = LL.Dim, fontSize = 12.sp)
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("${kcal.toInt()} kcal", color = LL.Green, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Text("蛋白 ${fmtNum(protein)} g", color = LL.Muted, fontSize = 10.sp)
                }
            }

            Spacer(Modifier.height(10.dp))
            // 快捷份量
            val quick = if (byUnit) listOf(0.5, 1.0, 1.5, 2.0) else listOf(50.0, 100.0, 150.0, 200.0)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                quick.forEach { q ->
                    Box(
                        Modifier.background(LL.Surface2, RoundedCornerShape(9.dp))
                            .clickable { qtyText = fmtNum(q) }
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                    ) {
                        Text(
                            if (byUnit) "${fmtNum(q)}$unit" else "${q.toInt()}g",
                            color = LL.Muted, fontSize = 11.sp,
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            // 滚轮微调
            if (byUnit) {
                val options = remember { (1..40).map { it * 0.5 } }
                val idx = options.indexOfFirst { it >= qty }.let { if (it < 0) 1 else it }
                WheelPicker(
                    items = options.map { fmtNum(it) + unit },
                    index = idx,
                    modifier = Modifier.fillMaxWidth(),
                    onIndexChange = { i -> qtyText = fmtNum(options[i]) },
                )
            }

            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GhostButton("取消", Modifier.weight(1f)) { onDismiss() }
                PrimaryButton(
                    if (draft.editId == null) "添加" else "保存",
                    Modifier.weight(1.4f),
                    enabled = grams > 0,
                ) {
                    vm.addFood(
                        draft = draft,
                        grams = grams,
                        servings = if (byUnit) qty else null,
                        servingUnit = if (byUnit) unit else null,
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

// ------------------------------------------------------------------ AI 拆解结果

@Composable
fun AiResultPage(
    vm: AppViewModel,
    estimate: FoodEstimate,
    query: String,
    meal: MealType,
    onBack: () -> Unit,
) {
    var multiplier by remember { mutableStateOf(1.0) }
    val totalKcal = (estimate.totalKcal * multiplier).toInt()
    val totalProtein = estimate.totalProtein * multiplier
    val totalCarb = estimate.totalCarb * multiplier
    val totalFat = estimate.totalFat * multiplier

    FullScreenPage(
        title = query.ifBlank { "AI 估算" },
        subtitle = if (estimate.fromAi) "AI 拆解 · 可逐项核对" else "本地规则估算 · 未连 AI",
        onBack = onBack,
        footer = {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GhostButton("改份量", Modifier.weight(1f)) {
                    vm.openPortion(
                        FoodEntity(
                            id = "ai_tmp_" + System.currentTimeMillis(),
                            name = query.ifBlank { "AI 菜品" },
                            kcalPer100g = if (estimate.totalGrams > 0)
                                estimate.totalKcal * 100.0 / estimate.totalGrams else 100.0,
                            proteinPer100g = if (estimate.totalGrams > 0)
                                estimate.totalProtein * 100.0 / estimate.totalGrams else 5.0,
                            carbPer100g = if (estimate.totalGrams > 0)
                                estimate.totalCarb * 100.0 / estimate.totalGrams else null,
                            fatPer100g = if (estimate.totalGrams > 0)
                                estimate.totalFat * 100.0 / estimate.totalGrams else null,
                            servingUnit = estimate.servingUnit,
                            servingGrams = estimate.totalGrams,
                            emoji = "🍱",
                            source = if (estimate.fromAi) "AI" else "CUSTOM",
                        ),
                        meal = meal,
                    )
                }
                PrimaryButton("添加 $totalKcal kcal", Modifier.weight(1.5f)) {
                    vm.confirmAiResult(estimate, meal, multiplier)
                }
            }
        },
    ) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            // 总览
            Column(Modifier.fillMaxWidth().background(LL.Surface, RoundedCornerShape(14.dp)).padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🍱", fontSize = 22.sp)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(query.ifBlank { "AI 估算" }, color = LL.Text, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text(
                            "${fmtNum(estimate.servingQty * multiplier)} ${estimate.servingUnit} · 约 ${(estimate.totalGrams * multiplier).toInt()} g",
                            color = LL.Muted, fontSize = 10.5.sp,
                        )
                    }
                    if (estimate.fromAi) Pill("AI 估算", LL.Purple) else Pill("本地估算", LL.Amber)
                }
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Macro("热量", totalKcal.toDouble(), "kcal", LL.Text)
                    Macro("蛋白", totalProtein, "g", LL.Blue)
                    Macro("碳水", totalCarb, "g", LL.Amber)
                    Macro("脂肪", totalFat, "g", LL.Purple)
                }
            }

            Spacer(Modifier.height(12.dp))
            Text("拆解（可核对，不必全信）", color = LL.Muted, fontSize = 10.5.sp)
            Spacer(Modifier.height(6.dp))

            estimate.items.forEach { item ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(5.dp).background(LL.Faint, CircleShape))
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(item.name, color = LL.Text, fontSize = 12.5.sp)
                        Text(
                            "${fmtNum(item.grams * multiplier)} g · 蛋白 ${fmtNum(item.protein * multiplier)} g",
                            color = LL.Dim, fontSize = 9.5.sp,
                        )
                    }
                    Text("${(item.kcal * multiplier).toInt()}", color = LL.Muted, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
                Box(Modifier.fillMaxWidth().height(1.dp).background(LL.Line.copy(alpha = 0.5f)))
            }

            if (estimate.assumptions.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Column(Modifier.fillMaxWidth().background(LL.Surface, RoundedCornerShape(12.dp)).padding(12.dp)) {
                    estimate.assumptions.forEach { note ->
                        Row(Modifier.padding(vertical = 2.dp)) {
                            Text("·", color = LL.Dim, fontSize = 10.sp)
                            Spacer(Modifier.width(5.dp))
                            Text(note, color = LL.Dim, fontSize = 9.5.sp)
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            Text("份量倍数：${fmtNum(multiplier)} 份", color = LL.Muted, fontSize = 10.5.sp)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(0.5, 0.75, 1.0, 1.25, 1.5).forEach { m ->
                    val on = m == multiplier
                    Box(
                        Modifier.background(
                            if (on) LL.Purple.copy(alpha = 0.18f) else LL.Surface2,
                            RoundedCornerShape(9.dp),
                        ).clickable { multiplier = m }.padding(horizontal = 11.dp, vertical = 7.dp),
                    ) {
                        Text(
                            fmtNum(m),
                            color = if (on) LL.Purple else LL.Muted,
                            fontSize = 11.5.sp,
                            fontWeight = if (on) FontWeight.Bold else FontWeight.Normal,
                        )
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun Macro(label: String, value: Double, unit: String, color: androidx.compose.ui.graphics.Color) {
    Column(horizontalAlignment = Alignment.Start) {
        Text(label, color = LL.Dim, fontSize = 9.sp)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value.toInt().toString(), color = color, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text(unit, color = LL.Dim, fontSize = 8.5.sp)
        }
    }
}
