package com.leanlog.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.leanlog.app.ai.AiService
import com.leanlog.app.ai.LocalEstimator
import com.leanlog.app.core.CalendarStatus
import com.leanlog.app.core.CardioType
import com.leanlog.app.core.FoodEstimate
import com.leanlog.app.core.Gender
import com.leanlog.app.core.LeanLogMath
import com.leanlog.app.core.MealType
import com.leanlog.app.core.SeedData
import com.leanlog.app.data.CardioRecordEntity
import com.leanlog.app.data.ExerciseEntity
import com.leanlog.app.data.FoodEntity
import com.leanlog.app.data.LeanLogDatabase
import com.leanlog.app.data.LeanLogRepository
import com.leanlog.app.data.MealEntryEntity
import com.leanlog.app.data.ProfileEntity
import com.leanlog.app.data.SettingsStore
import com.leanlog.app.data.StrengthRecordEntity
import com.leanlog.app.data.WeightLogEntity
import com.leanlog.app.domain.CardioParams
import com.leanlog.app.domain.Figures
import com.leanlog.app.export.ExportService
import com.leanlog.app.ui.theme.LL
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

// ------------------------------------------------------------------ UI 模型

enum class Tab { TRAIN, DIET, ME }

data class ExerciseUi(
    val id: String,
    val name: String,
    val met: Double,
    val bodyweightDefault: Boolean,
    val groups: List<String>,
    val custom: Boolean = false,
)

data class DayCell(
    val day: Int,
    val inMonth: Boolean,
    val status: CalendarStatus,
    val hasStrength: Boolean,
    val hasCardio: Boolean,
    val deficit: Int,
)

data class MacroTotals(
    val kcal: Double = 0.0,
    val protein: Double = 0.0,
    val carb: Double = 0.0,
    val fat: Double = 0.0,
)

data class DayDetail(
    val day: Int,
    val strength: List<StrengthRecordEntity> = emptyList(),
    val cardio: List<CardioRecordEntity> = emptyList(),
    val meals: List<MealEntryEntity> = emptyList(),
    val intake: MacroTotals = MacroTotals(),
    val workoutNet: Double = 0.0,
    val deficit: Int = 0,
) {
    val hasFood: Boolean get() = meals.isNotEmpty()
    val hasWorkout: Boolean get() = strength.isNotEmpty() || cardio.isNotEmpty()
}

data class WeekSummary(
    val avgDeficit: Int? = null,
    val hitDays: Int = 0,
    val recordedDays: Int = 0,
    val target: Int = 0,
)

data class ToastMsg(val id: Long, val text: String, val undo: (() -> Unit)? = null)

/** 弹出的浮层。参数页是全屏页，其余是底部弹窗。 */
sealed interface Overlay {
    data object ExercisePick : Overlay
    data class StrengthParam(val exerciseId: String, val editId: Long? = null) : Overlay
    data object CardioPick : Overlay
    data class CardioParam(val typeId: String, val editId: Long? = null) : Overlay
    data class FoodSearch(val meal: MealType) : Overlay
    data class Portion(val draft: PortionDraft) : Overlay
    data class AiResult(val estimate: FoodEstimate, val query: String, val meal: MealType) : Overlay
    data object ProteinTarget : Overlay
    data class ProfileEdit(val field: ProfileField) : Overlay
    data object AiConfig : Overlay
    data object Export : Overlay
    data object Theme : Overlay
}

enum class ProfileField { NAME, GENDER, AGE, HEIGHT, ACTIVITY, TARGET_WEIGHT, RATE, WEIGHT }

data class PortionDraft(
    val foodId: String,
    val name: String,
    val emoji: String?,
    val kcalPer100g: Double,
    val proteinPer100g: Double,
    val carbPer100g: Double?,
    val fatPer100g: Double?,
    val servingUnit: String?,
    val servingGrams: Double?,
    val isAi: Boolean,
    val editId: Long? = null,
    val meal: MealType,
    val initialGrams: Double,
)

data class UiState(
    val loading: Boolean = true,
    val tab: Tab = Tab.TRAIN,
    val selectedDay: Int = 0,
    val month: YearMonth = YearMonth.now(),
    val cells: List<DayCell> = emptyList(),
    val detail: DayDetail = DayDetail(0),
    val profile: ProfileEntity = ProfileEntity(),
    val proteinTarget: Int = 140,
    val tdeeFormula: Double = 0.0,
    val tdeeAdaptive: Double? = null,
    val tdeeUsed: Double = 0.0,
    val adaptiveNote: String? = null,
    val week: WeekSummary = WeekSummary(),
    val weights: List<WeightLogEntity> = emptyList(),
    val heatWarn: Boolean = true,
    /** 皮肤：true = 黑夜（默认）。 */
    val darkTheme: Boolean = true,
    /** 首次引导是否完成。false 时整个 App 只显示引导页。 */
    val onboarded: Boolean = false,
    val exercises: List<ExerciseUi> = emptyList(),
    val muscleGroups: List<Pair<String, String>> = emptyList(),
    val recentExerciseIds: List<String> = emptyList(),
    val searchResults: List<FoodEntity> = emptyList(),
    val recentFoods: List<FoodEntity> = emptyList(),
    val aiBusy: Boolean = false,
    val aiQuery: String = "",
) {
    val today: Int get() = LocalDate.now().toEpochDay().toInt()
}

// ------------------------------------------------------------------ ViewModel

class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = LeanLogRepository(LeanLogDatabase.get(app).dao())
    private val settings = SettingsStore(app)
    private val ai = AiService(settings)
    private val exportService = ExportService(app, repository)

    private val _state = MutableStateFlow(
        UiState(
            selectedDay = LocalDate.now().toEpochDay().toInt(),
            heatWarn = settings.heatWarn,
            darkTheme = settings.darkTheme,
        )
    )
    val state: StateFlow<UiState> = _state.asStateFlow()

    private val _overlay = MutableStateFlow<Overlay?>(null)
    val overlay: StateFlow<Overlay?> = _overlay.asStateFlow()

    private val _toast = MutableStateFlow<ToastMsg?>(null)
    val toast: StateFlow<ToastMsg?> = _toast.asStateFlow()


    private val _strengthDraft = MutableStateFlow<StrengthDraft?>(null)
    val currentStrengthDraft: StateFlow<StrengthDraft?> = _strengthDraft.asStateFlow()

    private val _cardioDraft = MutableStateFlow<CardioDraft?>(null)
    val currentCardioDraft: StateFlow<CardioDraft?> = _cardioDraft.asStateFlow()

    init {
        // 必须在第一帧组合之前把皮肤定下来，否则会先黑一帧再变白。
        LL.use(settings.darkTheme)
        viewModelScope.launch {
            repository.ensureSeeded()
            loadLibrary()
            // 直接 await，不要走 refresh() —— 那样 loading 会在数据就位之前就置 false，
            // 首帧会读到空 cells，引导页也会误判成"未引导"再闪一下。
            reloadAll()
            _state.update { it.copy(loading = false) }
        }
    }

    // ---------------------------------------------------------------- 加载

    private suspend fun loadLibrary() {
        val groups = repository.muscleGroups().map { it.id to it.name }
        val rows = repository.exercisesWithGroups()
        val grouped = rows.groupBy { it.id }.map { (id, list) ->
            val first = list.first()
            ExerciseUi(
                id = id,
                name = first.name,
                met = first.met,
                bodyweightDefault = first.bodyweightDefault,
                groups = list.map { it.groupId },
                custom = false,
            )
        }.sortedBy { it.name }
        _state.update { it.copy(exercises = grouped, muscleGroups = groups) }
    }

    fun refresh() {
        viewModelScope.launch { reloadAll() }
    }

    private suspend fun reloadAll() {
        val snapshot = _state.value
        val profile = repository.profile()
        val day = snapshot.selectedDay

        val intakeMap = repository.intakeByDay()
        val strengthMap = repository.strengthBurnByDay()
        val cardioMap = repository.cardioBurnByDay()

        val tdeeBase = Figures.tdeeBase(profile)
        val cells = buildCells(snapshot.month, day, intakeMap, strengthMap, cardioMap, tdeeBase)

        val strength = repository.strengthOn(day)
        val cardio = repository.cardioOn(day)
        val meals = repository.mealsOn(day)
        val intake = MacroTotals(
            kcal = meals.sumOf { it.kcal },
            protein = meals.sumOf { it.protein },
            carb = meals.sumOf { it.carb ?: 0.0 },
            fat = meals.sumOf { it.fat ?: 0.0 },
        )
        val workoutNet = strength.sumOf { it.netKcal } + cardio.sumOf { it.netKcal }
        val detail = DayDetail(day, strength, cardio, meals, intake, workoutNet, (tdeeBase + workoutNet - intake.kcal).toInt())

        // 周汇总：只统计有饮食记录的日子，没记录的那天不是「0 缺口」
        val weekStart = weekStartOf(day)
        val weekDays = (0..6).map { weekStart + it }
        val weekDeficits = weekDays.mapNotNull { d ->
            val row = intakeMap[d] ?: return@mapNotNull null
            val burn = (strengthMap[d]?.netKcal ?: 0.0) + (cardioMap[d]?.netKcal ?: 0.0)
            (tdeeBase + burn - row.kcal).toInt()
        }
        val target = Figures.targetDeficit(profile)

        val weights = repository.weights(day - 60)
        val adaptive = Figures.adaptive(
            weights = weights,
            intakeByDay = intakeMap.mapValues { it.value.kcal },
        )
        val compare = Figures.compareTdee(profile, adaptive)

        _state.update {
            it.copy(
                profile = profile,
                proteinTarget = Figures.proteinTarget(profile),
                cells = cells,
                detail = detail,
                week = WeekSummary(
                    avgDeficit = LeanLogMath.averageDeficit(weekDeficits),
                    hitDays = LeanLogMath.targetHitDays(weekDeficits, target),
                    recordedDays = weekDeficits.size,
                    target = target,
                ),
                weights = weights,
                tdeeFormula = compare.formula,
                tdeeAdaptive = compare.adaptive,
                tdeeUsed = compare.used,
                adaptiveNote = compare.note ?: adaptive.reason,
                recentExerciseIds = repository.recentExerciseIds(),
                recentFoods = repository.recentFoods(),
                onboarded = settings.onboarded,
            )
        }
    }

    private fun buildCells(
        month: YearMonth,
        selected: Int,
        intake: Map<Int, com.leanlog.app.data.DayIntakeRow>,
        strength: Map<Int, com.leanlog.app.data.DayBurnRow>,
        cardio: Map<Int, com.leanlog.app.data.DayBurnRow>,
        tdeeBase: Double,
    ): List<DayCell> {
        val first = month.atDay(1)
        val offset = first.dayOfWeek.value - 1 // 周一开头
        val start = first.minusDays(offset.toLong())
        val today = LocalDate.now().toEpochDay().toInt()
        val heatWarn = settings.heatWarn

        // 只生成这个月真正需要的行数。固定 42 格会在只有 5 行的月份多出一整行空框，
        // 视觉上像是"这个月还有几天没渲染"，很干扰。
        val rowCount = (offset + month.lengthOfMonth() + 6) / 7
        return (0 until rowCount * 7).map { i ->
            val date = start.plusDays(i.toLong())
            val day = date.toEpochDay().toInt()
            val hasFood = intake.containsKey(day)
            val hasStrength = (strength[day]?.entries ?: 0) > 0
            val hasCardio = (cardio[day]?.entries ?: 0) > 0
            val hasWorkout = hasStrength || hasCardio
            val burn = (strength[day]?.netKcal ?: 0.0) + (cardio[day]?.netKcal ?: 0.0)
            val deficit = (tdeeBase + burn - (intake[day]?.kcal ?: 0.0)).toInt()

            val status = if (heatWarn) {
                LeanLogMath.calendarStatus(day > today, hasFood, hasWorkout, deficit)
            } else {
                LeanLogMath.plainStatus(day > today, hasFood, hasWorkout)
            }
            DayCell(
                day = day,
                inMonth = date.month == month.month,
                status = status,
                hasStrength = hasStrength,
                hasCardio = hasCardio,
                deficit = deficit,
            )
        }
    }

    private fun weekStartOf(day: Int): Int {
        val date = LocalDate.ofEpochDay(day.toLong())
        return date.minusDays((date.dayOfWeek.value - 1).toLong()).toEpochDay().toInt()
    }

    // ---------------------------------------------------------------- 导航

    fun setTab(tab: Tab) = _state.update { it.copy(tab = tab) }

    fun selectDay(day: Int) {
        _state.update { it.copy(selectedDay = day) }
        viewModelScope.launch { reloadAll() }
    }

    fun shiftMonth(delta: Long) {
        _state.update { st ->
            val next = st.month.plusMonths(delta)
            // 选中日期要跟着翻过去，否则换月后日历里一格都不高亮，
            // 而「吸顶那一行」正是按选中日期算的，会对不上。
            val dayOfMonth = LocalDate.ofEpochDay(st.selectedDay.toLong()).dayOfMonth
                .coerceAtMost(next.lengthOfMonth())
            st.copy(month = next, selectedDay = next.atDay(dayOfMonth).toEpochDay().toInt())
        }
        viewModelScope.launch { reloadAll() }
    }

    fun showToast(text: String, undo: (() -> Unit)? = null) {
        _toast.value = ToastMsg(System.currentTimeMillis(), text, undo)
    }

    fun dismissToast() {
        _toast.value = null
    }

    // ---------------------------------------------------------------- 浮层

    fun closeOverlay() {
        _overlay.value = null
        _strengthDraft.value = null
        _cardioDraft.value = null
    }

    fun openExercisePick() {
        _overlay.value = Overlay.ExercisePick
    }

    fun openCardioPick() {
        _overlay.value = Overlay.CardioPick
    }

    fun openStrengthParam(exerciseId: String, editId: Long? = null) {
        viewModelScope.launch {
            val exercise = repository.exercise(exerciseId)
            val existing = editId?.let { repository.strengthById(it) }
            val last = repository.lastStrengthFor(exerciseId)
            val draft = StrengthDraft(
                exerciseId = exerciseId,
                exerciseName = exercise?.name ?: "未知器械",
                met = exercise?.met ?: 4.0,
                editId = editId,
                weightKg = existing?.weightKg ?: (last?.weightKg ?: 40.0),
                reps = existing?.reps ?: (last?.reps ?: 12),
                sets = existing?.sets ?: (last?.sets ?: 4),
                bodyweight = existing?.bodyweight
                    ?: (last?.bodyweight ?: (exercise?.bodyweightDefault ?: false)),
                lastSummary = last?.let { "上次 ${fmt(it.weightKg)}kg × ${it.reps}次 × ${it.sets}组" },
            )
            _strengthDraft.value = draft
            _overlay.value = Overlay.StrengthParam(exerciseId, editId)
        }
    }

    fun updateStrengthDraft(transform: (StrengthDraft) -> StrengthDraft) {
        val next = _strengthDraft.value?.let(transform) ?: return
        _strengthDraft.value = next
    }

    fun openCardioParam(typeId: String, editId: Long? = null) {
        val type = SeedData.cardioById(typeId) ?: return
        viewModelScope.launch {
            val existing = editId?.let { repository.cardioById(it) }
            val params = existing?.let { CardioParams.decode(it.paramsJson) } ?: type.defaultParams()
            val draft = CardioDraft(
                typeId = typeId,
                type = type,
                editId = editId,
                params = params,
                enumValue = existing?.enumValue ?: type.enumSpec?.default,
            )
            _cardioDraft.value = draft
            _overlay.value = Overlay.CardioParam(typeId, editId)
        }
    }

    fun updateCardioDraft(transform: (CardioDraft) -> CardioDraft) {
        val next = _cardioDraft.value?.let(transform) ?: return
        _cardioDraft.value = next
    }

    fun openFoodSearch(meal: MealType) {
        viewModelScope.launch {
            val all = repository.allFoods()
            _state.update { it.copy(searchResults = all, aiQuery = "") }
            _overlay.value = Overlay.FoodSearch(meal)
        }
    }

    fun searchFoods(query: String) {
        _state.update { it.copy(aiQuery = query) }
        viewModelScope.launch {
            val results = if (query.isBlank()) repository.allFoods() else repository.searchFoods(query.trim())
            _state.update { it.copy(searchResults = results) }
        }
    }

    fun openPortion(food: FoodEntity, meal: MealType, edit: MealEntryEntity? = null) {
        val grams = edit?.grams ?: food.servingGrams ?: 100.0
        _overlay.value = Overlay.Portion(
            PortionDraft(
                foodId = food.id,
                name = food.name,
                emoji = food.emoji,
                kcalPer100g = food.kcalPer100g,
                proteinPer100g = food.proteinPer100g,
                carbPer100g = food.carbPer100g,
                fatPer100g = food.fatPer100g,
                servingUnit = food.servingUnit,
                servingGrams = food.servingGrams,
                isAi = food.source == "AI",
                editId = edit?.id,
                meal = meal,
                initialGrams = grams,
            )
        )
    }

    fun openProteinSetting() {
        _overlay.value = Overlay.ProteinTarget
    }

    fun openProfileEdit(field: ProfileField) {
        _overlay.value = Overlay.ProfileEdit(field)
    }

    fun openAiConfig() {
        _overlay.value = Overlay.AiConfig
    }

    fun openExport() {
        _overlay.value = Overlay.Export
    }

    // ---------------------------------------------------------------- 写操作

    fun saveStrength() {
        val draft = _strengthDraft.value ?: return
        viewModelScope.launch {
            val profile = repository.profile()
            val bmr = Figures.bmr(profile)
            val calc = Figures.strengthCalc(
                exerciseMet = draft.met,
                weightKg = profile.weightKg,
                weightValue = draft.weightKg,
                reps = draft.reps,
                sets = draft.sets,
                bodyweight = draft.bodyweight,
                bmr = bmr,
            )
            val day = _state.value.selectedDay
            val record = StrengthRecordEntity(
                id = draft.editId ?: 0,
                day = day,
                exerciseId = draft.exerciseId,
                exerciseName = draft.exerciseName,
                weightKg = if (draft.bodyweight) 0.0 else draft.weightKg,
                reps = draft.reps,
                sets = draft.sets,
                bodyweight = draft.bodyweight,
                minutes = calc.minutes,
                netKcal = calc.net,
                createdAt = draft.editId?.let { repository.strengthById(it)?.createdAt }
                    ?: System.currentTimeMillis(),
            )
            if (draft.editId == null) repository.addStrength(record) else repository.updateStrength(record)

            closeOverlay()
            reloadAll()
            showToast(
                if (draft.editId == null) "已记录 ${draft.exerciseName} · 净消耗 ${calc.net.toInt()} kcal"
                else "已保存修改"
            )
        }
    }

    fun deleteStrength(id: Long) {
        viewModelScope.launch {
            val record = repository.strengthById(id)
            repository.deleteStrength(id)
            closeOverlay()
            reloadAll()
            if (record != null) {
                showToast("已删除 ${record.exerciseName}") {
                    viewModelScope.launch {
                        repository.addStrength(record.copy(id = 0))
                        reloadAll()
                    }
                }
            }
        }
    }

    fun saveCardio() {
        val draft = _cardioDraft.value ?: return
        viewModelScope.launch {
            val profile = repository.profile()
            val calc = Figures.cardioCalc(
                type = draft.type,
                params = draft.params,
                enumValue = draft.enumValue,
                bmr = Figures.bmr(profile),
                weightKg = profile.weightKg,
            )
            val day = _state.value.selectedDay
            val record = CardioRecordEntity(
                id = draft.editId ?: 0,
                day = day,
                typeId = draft.typeId,
                typeName = draft.type.name,
                paramsJson = CardioParams.encode(draft.params),
                enumValue = draft.enumValue,
                minutes = calc.minutes,
                met = calc.met,
                netKcal = calc.net,
                createdAt = draft.editId?.let { repository.cardioById(it)?.createdAt }
                    ?: System.currentTimeMillis(),
            )
            if (draft.editId == null) repository.addCardio(record) else repository.updateCardio(record)

            closeOverlay()
            reloadAll()
            showToast(
                if (draft.editId == null) "已记录 ${draft.type.name} · 净消耗 ${calc.net.toInt()} kcal"
                else "已保存修改"
            )
        }
    }

    fun deleteCardio(id: Long) {
        viewModelScope.launch {
            val record = repository.cardioById(id)
            repository.deleteCardio(id)
            closeOverlay()
            reloadAll()
            if (record != null) {
                showToast("已删除 ${record.typeName}") {
                    viewModelScope.launch {
                        repository.addCardio(record.copy(id = 0))
                        reloadAll()
                    }
                }
            }
        }
    }

    fun addFood(draft: PortionDraft, grams: Double, servings: Double?, servingUnit: String?) {
        viewModelScope.launch {
            val factor = grams / 100.0
            val entry = MealEntryEntity(
                id = draft.editId ?: 0,
                day = _state.value.selectedDay,
                meal = draft.meal.name,
                foodId = draft.foodId,
                foodName = draft.name,
                grams = grams,
                servings = servings,
                servingUnit = servingUnit,
                kcal = draft.kcalPer100g * factor,
                protein = draft.proteinPer100g * factor,
                carb = draft.carbPer100g?.times(factor),
                fat = draft.fatPer100g?.times(factor),
                isAi = draft.isAi,
                createdAt = System.currentTimeMillis(),
            )
            if (draft.editId == null) repository.addMeal(entry) else repository.updateMeal(entry)

            closeOverlay()
            reloadAll()
            showToast(
                if (draft.editId == null) "已加入${draft.meal.label} · ${(draft.kcalPer100g * factor).toInt()} kcal"
                else "已保存修改"
            )
        }
    }

    fun deleteMeal(entry: MealEntryEntity) {
        viewModelScope.launch {
            repository.deleteMeal(entry.id)
            reloadAll()
            showToast("已删除 ${entry.foodName}") {
                viewModelScope.launch {
                    repository.addMeal(entry.copy(id = 0))
                    reloadAll()
                }
            }
        }
    }

    /** 「最近吃过」一键入账：按上次吃的克数直接记，不再进份量页。 */
    fun quickAddFood(food: FoodEntity, meal: MealType) {
        viewModelScope.launch {
            val grams = food.servingGrams ?: 100.0
            val factor = grams / 100.0
            val entry = MealEntryEntity(
                day = _state.value.selectedDay,
                meal = meal.name,
                foodId = food.id,
                foodName = food.name,
                grams = grams,
                servings = 1.0,
                servingUnit = food.servingUnit,
                kcal = food.kcalPer100g * factor,
                protein = food.proteinPer100g * factor,
                carb = food.carbPer100g?.times(factor),
                fat = food.fatPer100g?.times(factor),
                isAi = food.source == "AI",
            )
            repository.addMeal(entry)
            closeOverlay()
            reloadAll()
            showToast("已加入${meal.label} · ${entry.kcal.toInt()} kcal") {
                viewModelScope.launch {
                    repository.deleteMeal(entry.id)
                    reloadAll()
                }
            }
        }
    }

    // ---------------------------------------------------------------- AI

    fun runAiSearch(query: String, meal: MealType) {
        if (query.isBlank()) return
        viewModelScope.launch {
            _state.update { it.copy(aiBusy = true) }
            val estimate = ai.estimateFood(query)
            _state.update { it.copy(aiBusy = false) }
            // 配了 Key 却仍走本地规则 = 请求失败了。必须说出来，
            // 否则用户会以为 AI 在用，实际拿到的是误差 ±25% 的兜底数字。
            if (!estimate.fromAi && settings.aiConfigured) {
                showToast(ai.lastError ?: "AI 请求失败，已用本地规则估算")
            }
            _overlay.value = Overlay.AiResult(estimate, query, meal)
        }
    }

    /** AI 结果一键入账：拆解项写入本周的当天记录，并把这份「菜品」写回本地食物库。 */
    fun confirmAiResult(estimate: FoodEstimate, meal: MealType, multiplier: Double) {
        viewModelScope.launch {
            val day = _state.value.selectedDay
            val totalKcal = estimate.totalKcal * multiplier
            val totalProtein = estimate.totalProtein * multiplier
            val totalCarb = estimate.totalCarb * multiplier
            val totalFat = estimate.totalFat * multiplier
            val totalGrams = estimate.totalGrams * multiplier

            val foodId = "ai_" + System.currentTimeMillis()
            val per100 = if (totalGrams > 0) 100.0 / totalGrams else 1.0
            repository.upsertFood(
                FoodEntity(
                    id = foodId,
                    name = estimate.items.firstOrNull()?.name?.let { "AI 菜品" } ?: "AI 菜品",
                    alias = null,
                    kcalPer100g = totalKcal * per100,
                    proteinPer100g = totalProtein * per100,
                    carbPer100g = totalCarb * per100,
                    fatPer100g = totalFat * per100,
                    servingUnit = estimate.servingUnit,
                    servingGrams = totalGrams,
                    emoji = "🍱",
                    source = "AI",
                )
            )

            repository.addMeal(
                MealEntryEntity(
                    day = day,
                    meal = meal.name,
                    foodId = foodId,
                    foodName = estimateTitle(estimate),
                    grams = totalGrams,
                    servings = estimate.servingQty * multiplier,
                    servingUnit = estimate.servingUnit,
                    kcal = totalKcal,
                    protein = totalProtein,
                    carb = totalCarb,
                    fat = totalFat,
                    isAi = estimate.fromAi,
                )
            )
            closeOverlay()
            reloadAll()
            showToast("已加入${meal.label} · ${totalKcal.toInt()} kcal")
        }
    }

    private fun estimateTitle(estimate: FoodEstimate): String {
        val main = estimate.items.firstOrNull { !it.name.contains("油") && !it.name.contains("饭") }
        return main?.name?.let { "$it 等 ${estimate.items.size} 项" } ?: "AI 估算菜品"
    }

    /** 没有 API Key 时也要能走完流程：用本地规则给个初值。 */
    fun localEstimate(query: String, meal: MealType) {
        _overlay.value = Overlay.AiResult(LocalEstimator.estimate(query), query, meal)
    }

    // ---------------------------------------------------------------- 设置

    /**
     * 首次引导完成。
     *
     * 引导页只在 [UiState.onboarded] 为 false 时显示，所以这里不需要再做"是否已有资料"的判断 ——
     * 用户之后在「我」页改的任何值都不会被它覆盖。
     * 顺手记一条今天的体重：体重曲线从第一天就有起点，自适应 TDEE 也才有可用的回归样本。
     */
    fun completeOnboarding(name: String, gender: Gender, age: Int, heightCm: Double, weightKg: Double) {
        viewModelScope.launch {
            val p = repository.profile()
            repository.saveProfile(
                p.copy(
                    name = name.trim().take(12),
                    gender = gender.name,
                    age = age.coerceIn(10, 100),
                    heightCm = heightCm.coerceIn(120.0, 230.0),
                    weightKg = weightKg.coerceIn(30.0, 250.0),
                    targetWeightKg = (weightKg - 5.0).coerceAtLeast(40.0),
                )
            )
            repository.recordWeight(LocalDate.now().toEpochDay().toInt(), weightKg)
            settings.onboarded = true
            _state.update { it.copy(onboarded = true) }
            reloadAll()
            showToast("资料已保存，开始记录吧")
        }
    }

    fun toggleHeatWarn() {
        val next = !settings.heatWarn
        settings.heatWarn = next
        _state.update { it.copy(heatWarn = next) }
        viewModelScope.launch { reloadAll() }
    }

    fun openTheme() {
        _overlay.value = Overlay.Theme
    }

    /**
     * 换肤。注意不需要 reloadAll：颜色是全局 state，改完所有界面自己就重组了，
     * 数据跟着重查一遍纯属浪费。
     */
    fun setTheme(dark: Boolean) {
        settings.darkTheme = dark
        LL.use(dark)
        _state.update { it.copy(darkTheme = dark) }
        closeOverlay()
    }

    fun setProteinTarget(customG: Int, auto: Boolean) {
        viewModelScope.launch {
            val profile = repository.profile()
            repository.saveProfile(profile.copy(proteinAuto = auto, proteinCustomG = customG))
            closeOverlay()
            reloadAll()
            showToast(if (auto) "蛋白目标已恢复自动" else "蛋白目标已设为 $customG g")
        }
    }

    fun saveProfile(field: ProfileField, value: String) {
        viewModelScope.launch {
            val p = repository.profile()
            val updated = when (field) {
                ProfileField.NAME -> p.copy(name = value)
                ProfileField.GENDER -> p.copy(gender = if (value == "女" || value == "FEMALE") "FEMALE" else "MALE")
                ProfileField.AGE -> p.copy(age = value.toIntOrNull()?.coerceIn(10, 100) ?: p.age)
                ProfileField.HEIGHT -> p.copy(heightCm = value.toDoubleOrNull()?.coerceIn(120.0, 230.0) ?: p.heightCm)
                ProfileField.ACTIVITY -> p.copy(activityFactor = value.toDoubleOrNull() ?: p.activityFactor)
                ProfileField.TARGET_WEIGHT -> p.copy(targetWeightKg = value.toDoubleOrNull() ?: p.targetWeightKg)
                ProfileField.RATE -> p.copy(weeklyRateKg = value.toDoubleOrNull() ?: p.weeklyRateKg)
                ProfileField.WEIGHT -> p.copy(weightKg = value.toDoubleOrNull() ?: p.weightKg)
            }
            repository.saveProfile(updated)
            if (field == ProfileField.WEIGHT) {
                updated.weightKg.let { kg ->
                    repository.recordWeight(LocalDate.now().toEpochDay().toInt(), kg)
                }
            }
            closeOverlay()
            reloadAll()
        }
    }

    fun saveAiConfig(baseUrl: String, apiKey: String, model: String, visionModel: String) {
        settings.aiBaseUrl = baseUrl.trim().ifBlank { SettingsStore.DEFAULT_BASE_URL }
        settings.aiApiKey = apiKey.trim()
        settings.aiModel = model.trim().ifBlank { SettingsStore.DEFAULT_MODEL }
        settings.aiVisionModel = visionModel.trim()
        closeOverlay()
        showToast(if (settings.aiConfigured) "AI 已配置" else "已清除 API Key")
    }

    fun aiConfigured(): Boolean = settings.aiConfigured

    fun aiSettingsSnapshot(): List<String> = listOf(
        settings.aiBaseUrl, settings.aiApiKey, settings.aiModel, settings.aiVisionModel,
    )

    fun exportData() {
        viewModelScope.launch {
            runCatching { exportService.exportAll() }
                .onSuccess { result ->
                    settings.lastBackupAt = System.currentTimeMillis()
                    closeOverlay()
                    showToast("已导出 ${result.files.size} 个文件 · ${result.dailySummary}")
                }
                .onFailure { showToast("导出失败：${it.message}") }
        }
    }

    fun exportShareIntent(uris: List<android.net.Uri>) = exportService.shareIntent(uris)

    // ---------------------------------------------------------------- 辅助

    fun cardioType(id: String): CardioType? = SeedData.cardioById(id)

    fun exercisesInGroup(groupId: String, query: String = ""): List<ExerciseUi> {
        val all = _state.value.exercises
        val filtered = if (query.isBlank()) all.filter { groupId in it.groups }
        else all.filter { it.name.contains(query.trim()) }
        return filtered.sortedBy { it.name }
    }

    fun recentExercises(): List<ExerciseUi> {
        val map = _state.value.exercises.associateBy { it.id }
        return _state.value.recentExerciseIds.mapNotNull { map[it] }
    }

    fun exerciseById(id: String): ExerciseUi? = _state.value.exercises.firstOrNull { it.id == id }

    fun genderLabel(p: ProfileEntity): String = if (p.gender == "FEMALE") "女" else "男"

    fun genderOf(p: ProfileEntity): Gender = Figures.gender(p)

    private fun fmt(v: Double) = if (v == v.toLong().toDouble()) v.toLong().toString() else v.toString()
}

/** 力量参数页的草稿。滚轮与输入框共用同一份。 */
data class StrengthDraft(
    val exerciseId: String,
    val exerciseName: String,
    val met: Double,
    val editId: Long?,
    val weightKg: Double,
    val reps: Int,
    val sets: Int,
    val bodyweight: Boolean,
    val lastSummary: String?,
)

data class CardioDraft(
    val typeId: String,
    val type: CardioType,
    val editId: Long?,
    val params: Map<String, Double>,
    val enumValue: String?,
)
