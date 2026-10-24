package com.leanlog.app.ai

import android.util.Log
import com.leanlog.app.core.DetectedFood
import com.leanlog.app.core.FoodEstimate
import com.leanlog.app.core.FoodEstimateItem
import com.leanlog.app.data.SettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * 走 OpenAI 兼容协议（/chat/completions），所以 DeepSeek、通义、豆包、智谱、
 * 甚至本地 Ollama 只要改 Base URL 和模型名就能用。
 *
 * 三条硬规则：
 * 1. Key 只存本地，不进代码、不上传。
 * 2. 一定要有本地规则兜底 —— 没网、没配 Key、被限流，记录不能中断。
 * 3. AI 结果必须标 fromAi=false 时说明是本地估算，界面要显示区别。
 */
class AiService(private val settings: SettingsStore) {

    /**
     * 最近一次失败原因。AI 出错会静默降级到本地规则，界面上只看到「本地估算」，
     * 用户无从知道是 Key 错了还是网络断了 —— 这个字段就是给界面提示用的。
     */
    @Volatile
    var lastError: String? = null
        private set

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .build()

    private val jsonMedia = "application/json; charset=utf-8".toMediaType()

    // ---------------------------------------------------------------- 文本估算

    suspend fun estimateFood(query: String): FoodEstimate = withContext(Dispatchers.IO) {
        lastError = null
        if (!settings.aiConfigured) {
            lastError = "还没填 API Key"
            return@withContext LocalEstimator.estimate(query)
        }

        val prompt = """
            你是中文食物营养估算助手。用户在减脂，请估算他吃完这份食物摄入的量。
            用户描述：「$query」

            只返回一个 JSON 对象，不要任何解释文字、不要 markdown 代码块：
            {
              "serving": {"qty": 1, "unit": "份", "grams": 420},
              "items": [
                {"name": "杂粮饭", "grams": 140, "kcal": 122, "protein": 4.2, "carb": 26.0, "fat": 1.1}
              ],
              "assumptions": ["按常见外卖便当分量估算", "含少量烹调油"]
            }

            要求：
            - items 拆成 3-6 个可见配料（主食、主料、蔬菜、蛋、油/酱汁），不要只给一个总数
            - kcal 是该克数下的总热量，不是每 100g
            - 烹调油和酱汁必须单独列一项，外卖菜品默认含油
            - 所有数值为数字，不要带单位
        """.trimIndent()

        val content = chat(
            model = settings.aiModel,
            messages = JSONArray().put(
                JSONObject().put("role", "user").put("content", prompt)
            ),
        ) ?: return@withContext LocalEstimator.estimate(query)

        val parsed = parseEstimate(content)
        if (parsed == null) lastError = "模型返回的不是预期 JSON"
        parsed ?: LocalEstimator.estimate(query)
    }

    // ---------------------------------------------------------------- 图片识别

    /**
     * 返回 null 表示无法识别（未配置 Key / 请求失败）。界面据此提示，而不是编造数据。
     */
    suspend fun detectFromPhoto(jpegBase64: String): List<DetectedFood>? = withContext(Dispatchers.IO) {
        if (!settings.aiConfigured) return@withContext null

        val visionPrompt = """
            这是用户的一餐照片。识别出盘中所有食物，估算各自重量与热量。
            只返回 JSON 数组，不要解释文字：
            [{"name":"米饭","grams":180,"kcal":209,"protein":4.7,"confidence":0.9}]
            要求：
            - 分开列出每一样食物，包括主食、肉、蔬菜、饮料
            - 油炸、红烧、酱汁类要提示热量偏高
            - confidence 是 0-1 的把握程度
        """.trimIndent()

        val contentArray = JSONArray()
            .put(JSONObject().put("type", "text").put("text", visionPrompt))
            .put(
                JSONObject().put("type", "image_url")
                    .put("image_url", JSONObject().put("url", "data:image/jpeg;base64,$jpegBase64"))
            )

        val model = settings.aiVisionModel.ifBlank { settings.aiModel }
        val content = chat(
            model = model,
            messages = JSONArray().put(JSONObject().put("role", "user").put("content", contentArray)),
        ) ?: return@withContext null

        parseDetections(content)
    }

    // ---------------------------------------------------------------- HTTP

    private fun chat(model: String, messages: JSONArray): String? {
        val body = JSONObject()
            .put("model", model)
            .put("messages", messages)
            .put("temperature", 0.2)
            .put("stream", false)

        val url = settings.aiBaseUrl.trimEnd('/') + "/chat/completions"
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer ${settings.aiApiKey}")
            .addHeader("Content-Type", "application/json")
            .post(body.toString().toRequestBody(jsonMedia))
            .build()

        // AI 失败会静默降级到本地规则，界面上只看到「本地估算」四个字。
        // 所以失败原因必须落到 logcat，否则永远查不出是 Key 错、模型下线还是网络不通。
        Log.d(TAG, "POST $url model=$model keyLen=${settings.aiApiKey.length}")

        return runCatching {
            client.newCall(request).execute().use { resp ->
                // 服务器返回的 Content-Type 常常不带 charset，OkHttp 会按 ISO-8859-1 解，
                // 结果中文全变乱码（AI 返回的食物名、错误提示都会）。JSON 规范就是 UTF-8，自己解。
                val bytes = resp.body?.bytes()
                val raw = if (bytes == null) "" else String(bytes, Charsets.UTF_8)
                if (!resp.isSuccessful) {
                    lastError = describeError(resp.code, raw)
                    Log.e(TAG, "HTTP ${resp.code} ${resp.message} body=${raw.take(600)}")
                    return null
                }
                val content = JSONObject(raw)
                    .optJSONArray("choices")
                    ?.optJSONObject(0)
                    ?.optJSONObject("message")
                    ?.opt("content")
                val text = when (content) {
                    is String -> content
                    is JSONArray -> buildString {
                        for (i in 0 until content.length()) {
                            val part = content.optJSONObject(i) ?: continue
                            if (part.optString("type") == "text") append(part.optString("text"))
                        }
                    }
                    else -> null
                }
                if (text.isNullOrBlank()) {
                    lastError = "响应里没有内容"
                    Log.e(TAG, "响应无 content，raw=${raw.take(600)}")
                }
                text
            }
        }.onFailure {
            lastError = when (it) {
                is java.net.UnknownHostException -> "域名解析失败，检查网络"
                is java.net.SocketTimeoutException -> "请求超时"
                is javax.net.ssl.SSLException -> "TLS 握手失败"
                is java.net.ConnectException -> "连不上服务器"
                else -> it.javaClass.simpleName + ": " + it.message
            }
            Log.e(TAG, "请求异常 ${it.javaClass.simpleName}: ${it.message}")
        }.getOrNull()
    }

    /** 把 HTTP 错误翻成用户看得懂的一句话，顺带给出最可能的处置方向。 */
    private fun describeError(code: Int, raw: String): String {
        val msg = runCatching {
            JSONObject(raw).optJSONObject("error")?.optString("message")
        }.getOrNull()
        val hint = when (code) {
            401 -> "Key 无效或已过期，请到控制台重新复制"
            402 -> "余额不足"
            403 -> "该 Key 没有这个模型的权限"
            404 -> "地址或模型名不对"
            429 -> "请求过于频繁，稍后再试"
            in 500..599 -> "服务端异常，稍后再试"
            else -> null
        }
        return buildString {
            append("HTTP ").append(code)
            if (!msg.isNullOrBlank()) append(" · ").append(msg)
            if (hint != null) append("（").append(hint).append("）")
        }
    }

    private companion object {
        const val TAG = "MetriAI"
    }

    // ---------------------------------------------------------------- 解析

    private fun parseEstimate(content: String): FoodEstimate? {
        val json = extractJsonObject(content) ?: return null
        return runCatching {
            val serving = json.optJSONObject("serving")
            val itemsJson = json.optJSONArray("items") ?: return null
            val items = (0 until itemsJson.length()).mapNotNull { i ->
                val o = itemsJson.optJSONObject(i) ?: return@mapNotNull null
                FoodEstimateItem(
                    name = o.optString("name").ifBlank { "未知食材" },
                    grams = o.optDouble("grams", 0.0),
                    kcal = o.optDouble("kcal", 0.0),
                    protein = o.optDouble("protein", 0.0),
                    carb = o.optDouble("carb", 0.0),
                    fat = o.optDouble("fat", 0.0),
                    confidence = 0.8,
                )
            }
            if (items.isEmpty()) return null
            val assumptions = json.optJSONArray("assumptions")?.let { arr ->
                (0 until arr.length()).map { arr.optString(it) }
            } ?: emptyList()
            FoodEstimate(
                items = items,
                servingQty = serving?.optDouble("qty", 1.0) ?: 1.0,
                servingUnit = serving?.optString("unit")?.ifBlank { "份" } ?: "份",
                servingGrams = serving?.optDouble("grams", items.sumOf { it.grams }) ?: items.sumOf { it.grams },
                assumptions = assumptions,
                fromAi = true,
            )
        }.getOrNull()
    }

    private fun parseDetections(content: String): List<DetectedFood>? {
        val array = extractJsonArray(content) ?: return null
        return runCatching {
            (0 until array.length()).mapNotNull { i ->
                val o = array.optJSONObject(i) ?: return@mapNotNull null
                DetectedFood(
                    name = o.optString("name").ifBlank { return@mapNotNull null },
                    grams = o.optDouble("grams", 100.0),
                    kcal = o.optDouble("kcal", 0.0),
                    protein = o.optDouble("protein", 0.0),
                    confidence = o.optDouble("confidence", 0.7).coerceIn(0.0, 1.0),
                )
            }.takeIf { it.isNotEmpty() }
        }.getOrNull()
    }

    private fun extractJsonObject(text: String): JSONObject? {
        val start = text.indexOf('{')
        val end = text.lastIndexOf('}')
        if (start < 0 || end <= start) return null
        return runCatching { JSONObject(text.substring(start, end + 1)) }.getOrNull()
    }

    private fun extractJsonArray(text: String): JSONArray? {
        val start = text.indexOf('[')
        val end = text.lastIndexOf(']')
        if (start < 0 || end <= start) return null
        return runCatching { JSONArray(text.substring(start, end + 1)) }.getOrNull()
    }
}
