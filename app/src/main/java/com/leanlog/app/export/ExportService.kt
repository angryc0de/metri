package com.leanlog.app.export

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.leanlog.app.core.SeedData
import com.leanlog.app.data.ExportBundle
import com.leanlog.app.data.LeanLogRepository
import com.leanlog.app.domain.CardioParams
import com.leanlog.app.domain.Figures
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * 导出。两种格式各有用途，都要有：
 * - JSON 是**全量备份**，能原样恢复（换手机靠它）；
 * - CSV 是**给人看的**，丢进 Excel 做透视。
 *
 * 只有出口没有入口的备份等于没有，所以 JSON 里必须带 schema 版本号，
 * 以后表结构变了才知道怎么迁移。
 */
class ExportService(
    private val context: Context,
    private val repository: LeanLogRepository,
) {

    data class ExportResult(val files: List<File>, val uris: List<Uri>, val dailySummary: String)

    suspend fun exportAll(): ExportResult = withContext(Dispatchers.IO) {
        val bundle = repository.exportBundle()
        val dir = File(context.filesDir, "exports").apply { mkdirs() }
        val stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmm"))

        val files = mutableListOf<File>()

        val jsonFile = File(dir, "leanlog-backup-$stamp.json")
        jsonFile.writeText(buildJson(bundle))
        files += jsonFile

        val csvs = mapOf(
            "meals-$stamp.csv" to mealsCsv(bundle),
            "strength-$stamp.csv" to strengthCsv(bundle),
            "cardio-$stamp.csv" to cardioCsv(bundle),
            "weight-$stamp.csv" to weightCsv(bundle),
            "daily-$stamp.csv" to dailyCsv(bundle),
        )
        csvs.forEach { (name, content) ->
            val f = File(dir, name)
            // Excel 打开中文 CSV 必须有 BOM，否则全是乱码
            f.writeText("\uFEFF" + content)
            files += f
        }

        // 同时往系统「下载」目录放一份，用户拿去电脑用
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            files.forEach { pushToDownloads(it) }
        }

        val uris = files.map {
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", it)
        }
        ExportResult(files, uris, dailySummary(bundle))
    }

    fun shareIntent(uris: List<Uri>): Intent {
        val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "application/octet-stream"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return Intent.createChooser(intent, "导出 LeanLog 数据")
    }

    private fun pushToDownloads(file: File) {
        runCatching {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, file.name)
                put(MediaStore.Downloads.MIME_TYPE, if (file.name.endsWith(".csv")) "text/csv" else "application/json")
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/LeanLog")
            }
            val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: return
            context.contentResolver.openOutputStream(uri)?.use { out ->
                file.inputStream().use { it.copyTo(out) }
            }
        }
    }

    // ---------------------------------------------------------------- JSON

    private fun buildJson(b: ExportBundle): String {
        val root = JSONObject()
        root.put("schemaVersion", 1)
        root.put("app", "LeanLog")
        root.put("exportedAt", LocalDateTime.now().toString())

        root.put("profile", JSONObject().apply {
            put("name", b.profile.name)
            put("gender", b.profile.gender)
            put("age", b.profile.age)
            put("heightCm", b.profile.heightCm)
            put("weightKg", b.profile.weightKg)
            put("activityFactor", b.profile.activityFactor)
            put("targetWeightKg", b.profile.targetWeightKg)
            put("weeklyRateKg", b.profile.weeklyRateKg)
            put("proteinAuto", b.profile.proteinAuto)
            put("proteinCustomG", b.profile.proteinCustomG)
        })

        root.put("weights", JSONArray().apply {
            b.weights.forEach {
                put(JSONObject().put("date", date(it.day)).put("kg", it.kg))
            }
        })

        root.put("meals", JSONArray().apply {
            b.meals.forEach {
                put(JSONObject().apply {
                    put("date", date(it.day))
                    put("meal", it.meal)
                    put("foodId", it.foodId)
                    put("foodName", it.foodName)
                    put("grams", it.grams)
                    put("servings", it.servings ?: JSONObject.NULL)
                    put("servingUnit", it.servingUnit ?: JSONObject.NULL)
                    put("kcal", it.kcal)
                    put("protein", it.protein)
                    put("carb", it.carb ?: JSONObject.NULL)
                    put("fat", it.fat ?: JSONObject.NULL)
                    put("isAi", it.isAi)
                })
            }
        })

        root.put("strength", JSONArray().apply {
            b.strength.forEach {
                put(JSONObject().apply {
                    put("date", date(it.day))
                    put("exerciseId", it.exerciseId)
                    put("exerciseName", it.exerciseName)
                    put("weightKg", it.weightKg)
                    put("reps", it.reps)
                    put("sets", it.sets)
                    put("bodyweight", it.bodyweight)
                    put("minutes", it.minutes)
                    put("netKcal", it.netKcal)
                })
            }
        })

        root.put("cardio", JSONArray().apply {
            b.cardio.forEach {
                put(JSONObject().apply {
                    put("date", date(it.day))
                    put("typeId", it.typeId)
                    put("typeName", it.typeName)
                    put("paramsJson", it.paramsJson)
                    put("enumValue", it.enumValue ?: JSONObject.NULL)
                    put("minutes", it.minutes)
                    put("met", it.met)
                    put("netKcal", it.netKcal)
                })
            }
        })

        // 内置库不导出（换设备会重新灌种子），只导用户自己造的
        root.put("customFoods", JSONArray().apply {
            b.foods.filter { it.source != "BUILTIN" }.forEach {
                put(JSONObject().apply {
                    put("id", it.id)
                    put("name", it.name)
                    put("kcalPer100g", it.kcalPer100g)
                    put("proteinPer100g", it.proteinPer100g)
                    put("servingUnit", it.servingUnit ?: JSONObject.NULL)
                    put("servingGrams", it.servingGrams ?: JSONObject.NULL)
                    put("source", it.source)
                })
            }
        })
        root.put("customExercises", JSONArray().apply {
            b.exercises.filter { it.custom }.forEach {
                put(JSONObject().put("id", it.id).put("name", it.name).put("met", it.met))
            }
        })

        return root.toString(2)
    }

    // ---------------------------------------------------------------- CSV

    private data class DayRow(
        val day: Int,
        val kcal: Double,
        val protein: Double,
        val burn: Double,
        val deficit: Double,
    )

    /** 每天：摄入、蛋白、训练净消耗、缺口。缺口 = 基础消耗 + 训练净消耗 − 摄入。 */
    private fun dayRows(b: ExportBundle): List<DayRow> {
        val intake = b.meals.groupBy { it.day }
            .mapValues { (_, rows) -> rows.sumOf { it.kcal } to rows.sumOf { it.protein } }
        val burn = b.strength.groupBy { it.day }
            .mapValues { (_, rows) -> rows.sumOf { it.netKcal } }
            .toMutableMap()
        b.cardio.groupBy { it.day }.forEach { (day, rows) ->
            burn[day] = (burn[day] ?: 0.0) + rows.sumOf { it.netKcal }
        }
        val tdee = Figures.tdeeBase(b.profile)
        return intake.keys.union(burn.keys).sorted().map { day ->
            val kcal = intake[day]?.first ?: 0.0
            val protein = intake[day]?.second ?: 0.0
            val netBurn = burn[day] ?: 0.0
            // 训练净消耗必须单独带出来。之前这里只放了三个值，dailyCsv 就把
            // 缺口当成训练消耗写了出去，CSV 里那一列全是缺口数。
            DayRow(day, kcal, protein, netBurn, tdee + netBurn - kcal)
        }
    }

    private fun mealsCsv(b: ExportBundle): String = buildString {
        appendLine("日期,餐次,食物,克数,份数,份量单位,热量,蛋白,碳水,脂肪,来源")
        b.meals.forEach {
            appendLine(
                listOf(
                    date(it.day), mealLabel(it.meal), it.foodName,
                    num(it.grams), it.servings?.let { s -> num(s) } ?: "", it.servingUnit ?: "",
                    num(it.kcal), num(it.protein), it.carb?.let { c -> num(c) } ?: "",
                    it.fat?.let { f -> num(f) } ?: "", if (it.isAi) "AI估算" else "手动",
                ).joinToString(",")
            )
        }
    }

    private fun strengthCsv(b: ExportBundle): String = buildString {
        appendLine("日期,器械,重量kg,次数,组数,自重,时长分钟,净消耗")
        b.strength.forEach {
            appendLine(
                listOf(
                    date(it.day), it.exerciseName, num(it.weightKg), it.reps.toString(), it.sets.toString(),
                    if (it.bodyweight) "是" else "否", num(it.minutes), num(it.netKcal),
                ).joinToString(",")
            )
        }
    }

    private fun cardioCsv(b: ExportBundle): String = buildString {
        appendLine("日期,项目,参数,强度MET,时长分钟,净消耗")
        b.cardio.forEach {
            // 只导 enumValue 的话，坡度/速度/时间这些数值参数整列都是空的 ——
            // 而这些恰恰是事后唯一能解释"为什么这天消耗 367 大卡"的依据。
            val summary = SeedData.cardioById(it.typeId)?.let { type ->
                CardioParams.summary(type, CardioParams.decode(it.paramsJson), it.enumValue)
            }.orEmpty().ifEmpty { it.enumValue ?: "" }
            appendLine(
                listOf(
                    date(it.day), it.typeName, summary,
                    num(it.met), num(it.minutes), num(it.netKcal),
                ).joinToString(",")
            )
        }
    }

    private fun weightCsv(b: ExportBundle): String = buildString {
        appendLine("日期,体重kg")
        b.weights.forEach { appendLine("${date(it.day)},${num(it.kg)}") }
    }

    private fun dailyCsv(b: ExportBundle): String = buildString {
        appendLine("日期,摄入热量,蛋白,训练净消耗,基础消耗,热量缺口")
        val tdee = Math.round(Figures.tdeeBase(b.profile)).toInt()
        dayRows(b).forEach { r ->
            appendLine(
                "${date(r.day)},${num(r.kcal)},${num(r.protein)},${num(r.burn)},$tdee,${Math.round(r.deficit).toInt()}"
            )
        }
    }

    private fun dailySummary(b: ExportBundle): String {
        val rows = dayRows(b)
        if (rows.isEmpty()) return "还没有可统计的记录"
        val avg = Math.round(rows.map { it.deficit }.average()).toInt()
        return "共 ${rows.size} 天有记录，平均缺口 $avg kcal"
    }

    // ---------------------------------------------------------------- 工具

    private fun date(epochDay: Int) = LocalDate.ofEpochDay(epochDay.toLong()).toString()

    private fun mealLabel(meal: String) = when (meal) {
        "BREAKFAST" -> "早餐"
        "LUNCH" -> "午餐"
        "DINNER" -> "晚餐"
        else -> "其他"
    }

    private fun num(v: Double): String =
        if (v == v.toLong().toDouble()) v.toLong().toString() else String.format("%.1f", v)
}
