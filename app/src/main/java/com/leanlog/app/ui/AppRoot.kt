package com.leanlog.app.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.core.view.WindowCompat
import com.leanlog.app.t
import com.leanlog.app.ui.components.Dot
import com.leanlog.app.ui.screens.DietScreen
import com.leanlog.app.ui.screens.MeScreen
import com.leanlog.app.ui.screens.OnboardingPage
import com.leanlog.app.ui.screens.TrainScreen
import com.leanlog.app.ui.sheets.ActivitySheet
import com.leanlog.app.ui.sheets.AiConfigSheet
import com.leanlog.app.ui.sheets.AiResultPage
import com.leanlog.app.ui.sheets.CardioParamPage
import com.leanlog.app.ui.sheets.CardioPickSheet
import com.leanlog.app.ui.sheets.ExercisePickSheet
import com.leanlog.app.ui.sheets.ExportSheet
import com.leanlog.app.ui.sheets.FoodSearchSheet
import com.leanlog.app.ui.sheets.GenderSheet
import com.leanlog.app.ui.sheets.PortionSheet
import com.leanlog.app.ui.sheets.ProfileEditSheet
import com.leanlog.app.ui.sheets.ProteinSheet
import com.leanlog.app.ui.sheets.RateSheet
import com.leanlog.app.ui.sheets.StrengthParamPage
import com.leanlog.app.ui.sheets.ThemeSheet
import com.leanlog.app.ui.theme.LL
import kotlinx.coroutines.delay

@Composable
fun LeanLogApp(vm: AppViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()
    val overlay by vm.overlay.collectAsStateWithLifecycle()
    val strengthDraft by vm.currentStrengthDraft.collectAsStateWithLifecycle()
    val cardioDraft by vm.currentCardioDraft.collectAsStateWithLifecycle()
    val toast by vm.toast.collectAsStateWithLifecycle()

    // 白天模式下状态栏图标要变深，否则白底配白字全是看不见的。
    val view = LocalView.current
    SideEffect {
        val window = view.context.findActivity()?.window ?: return@SideEffect
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = !LL.isDark
            isAppearanceLightNavigationBars = !LL.isDark
        }
    }

    // 数据还没就位时先留一块纯底：onboarded 的默认值是 false，
    // 不加这道闸的话老用户每次启动都会闪一下引导页。
    if (state.loading) {
        Box(Modifier.fillMaxSize().background(LL.Bg))
        return
    }
    if (!state.onboarded) {
        OnboardingPage { name, gender, age, height, weight ->
            vm.completeOnboarding(name, gender, age, height, weight)
        }
        return
    }

    var collapsed by remember { mutableStateOf(false) }

    BackHandler(enabled = overlay != null) { vm.closeOverlay() }

    Box(Modifier.fillMaxSize().background(LL.Bg)) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            TopBar(state)

            Box(Modifier.weight(1f)) {
                when (state.tab) {
                    Tab.TRAIN -> TrainScreen(
                        state = state,
                        collapsed = collapsed,
                        onToggleCollapse = { collapsed = !collapsed },
                        onSelectDay = vm::selectDay,
                        onShiftMonth = vm::shiftMonth,
                        onAddStrength = { vm.openExercisePick() },
                        onAddCardio = { vm.openCardioPick() },
                        onEditStrength = { vm.openStrengthParam(idOfStrength(state, it), it) },
                        onEditCardio = { vm.openCardioParam(idOfCardio(state, it), it) },
                        onDeleteStrength = vm::deleteStrength,
                        onDeleteCardio = vm::deleteCardio,
                    )

                    Tab.DIET -> DietScreen(
                        state = state,
                        collapsed = collapsed,
                        onToggleCollapse = { collapsed = !collapsed },
                        onSelectDay = vm::selectDay,
                        onShiftMonth = vm::shiftMonth,
                        onAddMeal = { vm.openFoodSearch(it) },
                        onEditEntry = { entry, meal ->
                            vm.openPortion(
                                com.leanlog.app.data.FoodEntity(
                                    id = entry.foodId,
                                    name = entry.foodName,
                                    kcalPer100g = if (entry.grams > 0) entry.kcal * 100.0 / entry.grams else 0.0,
                                    proteinPer100g = if (entry.grams > 0) entry.protein * 100.0 / entry.grams else 0.0,
                                    carbPer100g = entry.carb?.let { c -> if (entry.grams > 0) c * 100.0 / entry.grams else null },
                                    fatPer100g = entry.fat?.let { f -> if (entry.grams > 0) f * 100.0 / entry.grams else null },
                                    servingUnit = entry.servingUnit,
                                    servingGrams = entry.grams,
                                    source = if (entry.isAi) "AI" else "CUSTOM",
                                ),
                                meal,
                                edit = entry,
                            )
                        },
                        onDeleteEntry = vm::deleteMeal,
                        onProteinSetting = vm::openProteinSetting,
                    )

                    Tab.ME -> MeScreen(
                        state = state,
                        onEditProfile = { field ->
                            if (field == ProfileField.GENDER) vm.openProfileEdit(ProfileField.GENDER)
                            else vm.openProfileEdit(field)
                        },
                        onProteinSetting = vm::openProteinSetting,
                        onToggleHeatWarn = vm::toggleHeatWarn,
                        onTheme = vm::openTheme,
                        onAiConfig = vm::openAiConfig,
                        onExport = vm::openExport,
                        onToast = { vm.showToast(it) },
                    )
                }
            }

            BottomBar(state.tab, vm::setTab)
        }

        // ---------------- 浮层
        when (val o = overlay) {
            null -> Unit

            Overlay.ExercisePick -> ExercisePickSheet(
                vm = vm,
                onDismiss = vm::closeOverlay,
                onPick = { exId -> vm.openStrengthParam(exId) },
            )

            is Overlay.StrengthParam -> strengthDraft?.let {
                StrengthParamPage(vm, it, vm::closeOverlay)
            }

            Overlay.CardioPick -> CardioPickSheet(
                onDismiss = vm::closeOverlay,
                onPick = { typeId -> vm.openCardioParam(typeId) },
            )

            is Overlay.CardioParam -> cardioDraft?.let {
                CardioParamPage(vm, it, vm::closeOverlay)
            }

            is Overlay.FoodSearch -> FoodSearchSheet(
                vm = vm,
                meal = o.meal,
                query = state.aiQuery,
                results = state.searchResults,
                recent = state.recentFoods,
                onDismiss = vm::closeOverlay,
            )

            is Overlay.Portion -> PortionSheet(vm, o.draft, vm::closeOverlay)

            is Overlay.AiResult -> AiResultPage(vm, o.estimate, o.query, o.meal, vm::closeOverlay)

            Overlay.ProteinTarget -> ProteinSheet(vm, vm::closeOverlay)

            is Overlay.ProfileEdit -> when (o.field) {
                ProfileField.GENDER -> GenderSheet(vm, vm::closeOverlay)
                ProfileField.ACTIVITY -> ActivitySheet(vm, vm::closeOverlay)
                ProfileField.RATE -> RateSheet(vm, vm::closeOverlay)
                else -> ProfileEditSheet(vm, o.field, vm::closeOverlay)
            }

            Overlay.AiConfig -> AiConfigSheet(vm, vm::closeOverlay)
            Overlay.Export -> ExportSheet(vm, vm::closeOverlay)
            Overlay.Theme -> ThemeSheet(vm, vm::closeOverlay)
        }

        // AI 请求中的遮罩
        if (state.aiBusy) {
            Box(
                Modifier.fillMaxSize().background(LL.Scrim.copy(alpha = 0.55f)),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    Modifier.background(LL.Surface, RoundedCornerShape(16.dp)).padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Dot(LL.Purple, 10.dp)
                    Spacer(Modifier.height(10.dp))
                    Text(t("AI 正在估算…"), color = LL.Text, fontSize = 12.5.sp)
                    // 实测 glm-4-flash 免费档一次要 15–20 秒，高峰期更久。
                    // 写「1–3 秒」会让人以为卡死了，直接点重试。
                    Text(t("通常 10–20 秒，免费模型高峰期更久"), color = LL.Dim, fontSize = 9.5.sp)
                }
            }
        }

        // ---------------- Toast
        toast?.let { msg ->
            LaunchedEffect(msg.id) {
                delay(2400)
                vm.dismissToast()
            }
            Box(
                Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 76.dp, start = 20.dp, end = 20.dp),
            ) {
                Row(
                    Modifier.background(LL.Surface3, RoundedCornerShape(12.dp)).padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(msg.text, color = LL.Text, fontSize = 11.5.sp)
                    if (msg.undo != null) {
                        Spacer(Modifier.size(12.dp))
                        Text(
                            t("撤销"),
                            color = LL.Green, fontSize = 11.5.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable {
                                msg.undo.invoke()
                                vm.dismissToast()
                            },
                        )
                    }
                }
            }
        }
    }
}

private fun idOfStrength(state: UiState, id: Long): String =
    state.detail.strength.firstOrNull { it.id == id }?.exerciseId ?: ""
private fun idOfCardio(state: UiState, id: Long): String =
    state.detail.cardio.firstOrNull { it.id == id }?.typeId ?: ""

@Composable
private fun TopBar(state: UiState) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(top = 2.dp, bottom = 10.dp),
    ) {
        // wordmark 独占第一行、居中：它是品牌抬头，应该比界面上任何内容都靠上。
        // 样式是用户在预览页里挑的「特粗 + 斜体」。之所以现在不糊了，
        // 是因为字距收回到 0：之前那版是斜体 + 特粗 + 1.6sp 字距三者叠加，
        // 5 个字母被拉开又被斜切，笔画边缘对不齐才显得脏。
        Text(
            "Metri",
            modifier = Modifier.align(Alignment.CenterHorizontally),
            color = LL.Text,
            fontSize = 13.sp,
            fontWeight = FontWeight.Black,
            fontStyle = FontStyle.Italic,
        )
        Spacer(Modifier.height(2.dp))
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 左：谁在用。姓名是引导页填的，没填就用一句招呼兜着，不留空。
            Column(Modifier.weight(1f)) {
                Text(
                    state.profile.name.ifBlank { t("你好") },
                    color = LL.Text, fontSize = 15.sp, fontWeight = FontWeight.Bold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(1.dp))
                Text(
                    t("今天 · ") + com.leanlog.app.ui.formatDayTitle(state.today),
                    color = LL.Muted, fontSize = 10.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.End) {
                Row(
                    Modifier.background(LL.Surface, RoundedCornerShape(10.dp)).padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        com.leanlog.app.ui.fmtNum(state.profile.weightKg),
                        color = LL.Text, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                    )
                    Text(" kg", color = LL.Dim, fontSize = 9.5.sp)
                }
            }
        }
    }
}

@Composable
private fun BottomBar(tab: Tab, onSelect: (Tab) -> Unit) {
    Column {
        Box(Modifier.fillMaxWidth().height(1.dp).background(LL.Line))
        Row(
            Modifier.fillMaxWidth().background(LL.Surface).navigationBarsPadding().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            TabItem(t("训练"), tab == Tab.TRAIN) { onSelect(Tab.TRAIN) }
            TabItem(t("饮食"), tab == Tab.DIET) { onSelect(Tab.DIET) }
            TabItem(t("我"), tab == Tab.ME) { onSelect(Tab.ME) }
        }
    }
}

@Composable
private fun TabItem(label: String, selected: Boolean, onClick: () -> Unit) {
    Column(
        Modifier.clickable { onClick() }.padding(horizontal = 22.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            label,
            color = if (selected) LL.Green else LL.Dim,
            fontSize = 12.5.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
        )
        Spacer(Modifier.height(4.dp))
        Box(
            Modifier.size(width = 14.dp, height = 2.5.dp)
                .background(if (selected) LL.Green else Color.Transparent, RoundedCornerShape(2.dp))
        )
    }
}

/** Compose 给的 context 可能是 ContextWrapper，要一层层往里剥才能拿到 Activity。 */
private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
