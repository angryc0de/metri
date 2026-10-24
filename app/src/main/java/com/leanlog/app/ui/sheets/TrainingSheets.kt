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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.leanlog.app.core.CardioType
import com.leanlog.app.core.SeedData
import com.leanlog.app.t
import com.leanlog.app.ui.AppViewModel
import com.leanlog.app.ui.CardioDraft
import com.leanlog.app.ui.ExerciseUi
import com.leanlog.app.ui.StrengthDraft
import com.leanlog.app.ui.components.FullScreenPage
import com.leanlog.app.ui.components.GhostButton
import com.leanlog.app.ui.components.PrimaryButton
import com.leanlog.app.ui.components.SearchField
import com.leanlog.app.ui.components.SheetShell
import com.leanlog.app.ui.components.ToggleSwitch
import com.leanlog.app.ui.components.WheelPicker
import com.leanlog.app.ui.fmtNum
import com.leanlog.app.ui.icons.ExerciseIcon
import com.leanlog.app.ui.theme.LL
import kotlin.math.roundToInt

// ------------------------------------------------------------------ 器械选择

@Composable
fun ExercisePickSheet(
    vm: AppViewModel,
    onDismiss: () -> Unit,
    onPick: (String) -> Unit,
) {
    val groups = vm.state.value.muscleGroups
    val recentExercises = remember { vm.recentExercises() }
    var query by remember { mutableStateOf("") }
    // 没有任何训练记录时不显示「最近」，直接落在第一个部位上。
    // 原先是回落到胸的器械列表、但左栏仍高亮着「最近」，看着像最近练过 9 次。
    var group by remember {
        mutableStateOf(if (recentExercises.isEmpty()) (groups.firstOrNull()?.first ?: "chest") else "recent")
    }
    val exercises: List<ExerciseUi> = remember(query, group) {
        if (query.isNotBlank()) vm.exercisesInGroup(group, query)
        else if (group == "recent") recentExercises
        else vm.exercisesInGroup(group)
    }

    SheetShell(
        title = t("选择器械"),
        subtitle = if (exercises.isEmpty()) null else "${exercises.size} ${t("个")}",
        onDismiss = onDismiss,
    ) {
        Box(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp)) {
            SearchField(query, t("搜索器械，例如：划船"), onValueChange = { query = it })
        }
        Row(Modifier.fillMaxSize()) {
            // 左侧部位导航
            Column(
                Modifier.width(74.dp).fillMaxSize().padding(start = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (recentExercises.isNotEmpty()) {
                    RailButton(t("最近"), group == "recent") { group = "recent" }
                }
                groups.forEach { (id, name) ->
                    RailButton(t(name), group == id) { group = id }
                }
            }
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(start = 10.dp, end = 12.dp, bottom = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(exercises.size) { index ->
                    val ex = exercises[index]
                    Column(
                        Modifier
                            .background(LL.Surface2, RoundedCornerShape(12.dp))
                            .clickable { onPick(ex.id) }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        ExerciseIcon(ex.id, size = 34.dp, tint = LL.Muted)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            t(ex.name),
                            color = LL.Text, fontSize = 11.sp, fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RailButton(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .background(if (selected) LL.Surface3 else androidx.compose.ui.graphics.Color.Transparent, RoundedCornerShape(9.dp))
            .clickable { onClick() }
            .padding(vertical = 9.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = if (selected) LL.Text else LL.Muted,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
        )
    }
}

// ------------------------------------------------------------------ 有氧项目选择

@Composable
fun CardioPickSheet(onDismiss: () -> Unit, onPick: (String) -> Unit) {
    SheetShell(title = t("选择有氧项目"), onDismiss = onDismiss, heightFraction = 0.72f) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(14.dp),
            horizontalArrangement = Arrangement.spacedBy(9.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            val types = SeedData.cardioTypes
            items(types.size) { index ->
                val type = types[index]
                Row(
                    Modifier.background(LL.Surface2, RoundedCornerShape(12.dp))
                        .clickable { onPick(type.id) }
                        .padding(horizontal = 10.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ExerciseIcon(type.id, size = 28.dp, tint = LL.Amber)
                    Spacer(Modifier.width(9.dp))
                    Column {
                        Text(t(type.name), color = LL.Text, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                        Text(
                            type.params.joinToString(" · ") { t(it.label) },
                            color = LL.Dim, fontSize = 8.5.sp,
                        )
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------------------ 力量参数页

@Composable
fun StrengthParamPage(
    vm: AppViewModel,
    draft: StrengthDraft,
    onBack: () -> Unit,
) {
    val weightOptions = remember { (0..1200).map { it * 2.5 } } // 0–300kg，步长 2.5
    val repOptions = remember { (1..20).toList() }
    val setOptions = remember { (1..20).toList() }

    val weightIndex = remember(draft.weightKg) {
        weightOptions.indexOfFirst { it >= draft.weightKg }.let { if (it < 0) 0 else it }
    }

    FullScreenPage(
        title = draft.exerciseName,
        subtitle = if (draft.editId == null) t("记录这一组训练") else t("编辑记录"),
        onBack = onBack,
        trailing = {
            if (draft.editId != null) {
                Text(
                    t("删除"), color = LL.Red, fontSize = 12.sp, fontWeight = FontWeight.Medium,
                    modifier = Modifier.clickable { vm.deleteStrength(draft.editId) },
                )
            }
        },
        footer = {
            PrimaryButton(
                text = if (draft.editId == null) t("添加记录") else t("保存修改"),
                modifier = Modifier.fillMaxWidth(),
            ) { vm.saveStrength() }
        },
    ) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            // 头图
            Row(
                Modifier.fillMaxWidth().background(LL.Surface, RoundedCornerShape(14.dp)).padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier.size(46.dp).background(LL.Surface2, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center,
                ) { ExerciseIcon(draft.exerciseId, size = 32.dp, tint = LL.Text) }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(t(draft.exerciseName), color = LL.Text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    draft.lastSummary?.let {
                        Text(it, color = LL.Muted, fontSize = 10.5.sp)
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // 自重开关
            Row(
                Modifier.fillMaxWidth().background(LL.Surface, RoundedCornerShape(14.dp)).padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(t("自重动作"), color = LL.Text, fontSize = 12.5.sp, fontWeight = FontWeight.Medium)
                    Text(t("开启后不记重量，只记次数与组数"), color = LL.Dim, fontSize = 9.5.sp)
                }
                ToggleSwitch(checked = draft.bodyweight) { on ->
                    vm.updateStrengthDraft { it.copy(bodyweight = on) }
                }
            }

            Spacer(Modifier.height(12.dp))

            // 三个滚轮
            Row(
                Modifier.fillMaxWidth().background(LL.Surface, RoundedCornerShape(14.dp)).padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (!draft.bodyweight) {
                    WheelColumn(
                        label = t("重量 kg"),
                        value = fmtNum(draft.weightKg),
                        modifier = Modifier.weight(1.15f),
                        items = weightOptions.map { fmtNum(it) },
                        index = weightIndex,
                    ) { idx ->
                        vm.updateStrengthDraft { it.copy(weightKg = weightOptions[idx].toDouble()) }
                    }
                }
                WheelColumn(
                    label = t("次数"),
                    value = draft.reps.toString(),
                    modifier = Modifier.weight(1f),
                    items = repOptions.map { it.toString() },
                    index = (draft.reps - 1).coerceIn(0, repOptions.lastIndex),
                ) { idx ->
                    vm.updateStrengthDraft { it.copy(reps = repOptions[idx]) }
                }
                WheelColumn(
                    label = t("组数"),
                    value = draft.sets.toString(),
                    modifier = Modifier.weight(1f),
                    items = setOptions.map { it.toString() },
                    index = (draft.sets - 1).coerceIn(0, setOptions.lastIndex),
                ) { idx ->
                    vm.updateStrengthDraft { it.copy(sets = setOptions[idx]) }
                }
            }

            Spacer(Modifier.height(12.dp))
            Text(
                "${t("预计训练时长 ")}${estimatedMinutes(draft)}${t(" 分钟（组数 ×（每组约 ")}${draft.reps * 3}${t(" 秒 + 组间休息 90 秒））")}",
                color = LL.Dim, fontSize = 9.5.sp,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
            Spacer(Modifier.height(20.dp))
        }
    }
}

private fun estimatedMinutes(draft: StrengthDraft): Int {
    val sec = draft.sets * (draft.reps * 3 + 90) - 90
    // 截断会把 6.9 分钟显示成 6，和实际算进去的净消耗对不上，用四舍五入
    return (sec.coerceAtLeast(60) / 60.0).roundToInt()
}

@Composable
private fun WheelColumn(
    label: String,
    value: String,
    modifier: Modifier,
    items: List<String>,
    index: Int,
    onChange: (Int) -> Unit,
) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = LL.Muted, fontSize = 9.5.sp)
        Spacer(Modifier.height(2.dp))
        Text(value, color = LL.Text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        WheelPicker(
            items = items,
            index = index.coerceIn(0, items.lastIndex.coerceAtLeast(0)),
            modifier = Modifier.fillMaxWidth(),
            onIndexChange = onChange,
        )
    }
}

// ------------------------------------------------------------------ 有氧参数页

@Composable
fun CardioParamPage(vm: AppViewModel, draft: CardioDraft, onBack: () -> Unit) {
    val type: CardioType = draft.type
    FullScreenPage(
        title = type.name,
        subtitle = if (draft.editId == null) t("记录这一次有氧") else t("编辑记录"),
        onBack = onBack,
        trailing = {
            if (draft.editId != null) {
                Text(
                    t("删除"), color = LL.Red, fontSize = 12.sp, fontWeight = FontWeight.Medium,
                    modifier = Modifier.clickable { vm.deleteCardio(draft.editId) },
                )
            }
        },
        footer = {
            PrimaryButton(
                text = if (draft.editId == null) t("添加记录") else t("保存修改"),
                modifier = Modifier.fillMaxWidth(),
                color = LL.Amber,
            ) { vm.saveCardio() }
        },
    ) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            Row(
                Modifier.fillMaxWidth().background(LL.Surface, RoundedCornerShape(14.dp)).padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier.size(46.dp).background(LL.Surface2, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center,
                ) { ExerciseIcon(type.id, size = 32.dp, tint = LL.Amber) }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(t(type.name), color = LL.Text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Text(
                        type.params.joinToString(" · ") { t(it.label) },
                        color = LL.Muted, fontSize = 10.5.sp,
                    )
                }
            }

            // 枚举型参数（泳姿、强度）
            type.enumSpec?.let { spec ->
                Spacer(Modifier.height(12.dp))
                Column(Modifier.fillMaxWidth().background(LL.Surface, RoundedCornerShape(14.dp)).padding(14.dp)) {
                    Text(t(spec.label), color = LL.Muted, fontSize = 10.sp)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        spec.options.forEach { option ->
                            val on = draft.enumValue == option
                            Box(
                                Modifier
                                    .background(if (on) LL.Amber.copy(alpha = 0.18f) else LL.Surface2, RoundedCornerShape(9.dp))
                                    .clickable { vm.updateCardioDraft { it.copy(enumValue = option) } }
                                    .padding(horizontal = 12.dp, vertical = 7.dp),
                            ) {
                                Text(
                                    t(option),
                                    color = if (on) LL.Amber else LL.Muted,
                                    fontSize = 11.5.sp,
                                    fontWeight = if (on) FontWeight.Bold else FontWeight.Normal,
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            Column(Modifier.fillMaxWidth().background(LL.Surface, RoundedCornerShape(14.dp)).padding(12.dp)) {
                type.params.chunked(3).forEach { rowSpecs ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        rowSpecs.forEach { spec ->
                            val current = draft.params[spec.key] ?: spec.default
                            val values = remember(spec.key) { buildRange(spec.min, spec.max, spec.step) }
                            val idx = values.indexOfFirst { it >= current }.let { if (it < 0) 0 else it }
                            WheelColumn(
                                label = "${t(spec.label)} ${t(spec.unit)}",
                                value = fmtNum(current),
                                modifier = Modifier.weight(1f),
                                items = values.map { fmtNum(it) },
                                index = idx,
                            ) { i ->
                                vm.updateCardioDraft { it.copy(params = it.params + (spec.key to values[i])) }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            Text(
                t("消耗按「MET × 体重 × 时长」估算，已扣除这段时间的静息消耗。实际误差约 ±15%。"),
                color = LL.Dim, fontSize = 9.5.sp, modifier = Modifier.padding(horizontal = 4.dp),
            )
            Spacer(Modifier.height(20.dp))
        }
    }
}

private fun buildRange(min: Double, max: Double, step: Double): List<Double> {
    val out = mutableListOf<Double>()
    var v = min
    while (v <= max + 1e-6 && out.size < 400) {
        out.add(Math.round(v * 10) / 10.0)
        v += step
    }
    return out
}
