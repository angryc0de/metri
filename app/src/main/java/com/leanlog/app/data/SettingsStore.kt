package com.leanlog.app.data

import android.content.Context
import android.content.SharedPreferences

/**
 * 轻量设置。这里只有开关和 API Key，用 SharedPreferences 足够，
 * 不值得为它引入 DataStore + 序列化。
 */
class SettingsStore(context: Context) {

    private val sp: SharedPreferences =
        context.applicationContext.getSharedPreferences("leanlog_settings", Context.MODE_PRIVATE)

    /** 关闭后日历不再按缺口标绿/红。 */
    var heatWarn: Boolean
        get() = sp.getBoolean(KEY_HEAT_WARN, true)
        set(value) = sp.edit().putBoolean(KEY_HEAT_WARN, value).apply()

    /** 首次引导是否完成。 */
    var onboarded: Boolean
        get() = sp.getBoolean(KEY_ONBOARDED, false)
        set(value) = sp.edit().putBoolean(KEY_ONBOARDED, value).apply()

    /** 皮肤：true = 黑夜（默认），false = 白天。 */
    var darkTheme: Boolean
        get() = sp.getBoolean(KEY_DARK_THEME, true)
        set(value) = sp.edit().putBoolean(KEY_DARK_THEME, value).apply()

    // ---------------------------------------------------------------- AI

    var aiBaseUrl: String
        get() = sp.getString(KEY_AI_URL, DEFAULT_BASE_URL) ?: DEFAULT_BASE_URL
        set(value) = sp.edit().putString(KEY_AI_URL, value).apply()

    var aiApiKey: String
        get() = sp.getString(KEY_AI_KEY, "") ?: ""
        set(value) = sp.edit().putString(KEY_AI_KEY, value).apply()

    var aiModel: String
        get() = sp.getString(KEY_AI_MODEL, DEFAULT_MODEL) ?: DEFAULT_MODEL
        set(value) = sp.edit().putString(KEY_AI_MODEL, value).apply()

    /** 拍照识别用的多模态模型，留空则回落到 aiModel。 */
    var aiVisionModel: String
        get() = sp.getString(KEY_AI_VISION_MODEL, "") ?: ""
        set(value) = sp.edit().putString(KEY_AI_VISION_MODEL, value).apply()

    val aiConfigured: Boolean get() = aiApiKey.isNotBlank()

    var lastBackupAt: Long
        get() = sp.getLong(KEY_LAST_BACKUP, 0L)
        set(value) = sp.edit().putLong(KEY_LAST_BACKUP, value).apply()

    companion object {
        private const val KEY_HEAT_WARN = "heat_warn"
        private const val KEY_ONBOARDED = "onboarded"
        private const val KEY_DARK_THEME = "dark_theme"
        private const val KEY_AI_URL = "ai_base_url"
        private const val KEY_AI_KEY = "ai_api_key"
        private const val KEY_AI_MODEL = "ai_model"
        private const val KEY_AI_VISION_MODEL = "ai_vision_model"
        private const val KEY_LAST_BACKUP = "last_backup_at"

        /** 任意 OpenAI 兼容端点。默认给一个国内可直连的免费模型，用户可随时改。 */
        const val DEFAULT_BASE_URL = "https://open.bigmodel.cn/api/paas/v4"
        const val DEFAULT_MODEL = "glm-4-flash"
    }
}
