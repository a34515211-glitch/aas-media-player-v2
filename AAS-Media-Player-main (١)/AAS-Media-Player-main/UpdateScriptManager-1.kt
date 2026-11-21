package com.aas.mediaplayer

import android.content.Context
import org.json.JSONObject

/**
 * Safe in-app update script handler.
 *
 * IMPORTANT:
 * This is intentionally NOT a Kotlin/Java interpreter. It never executes pasted source code,
 * downloads dex/jar files, loads plugins, or rewrites the installed APK.
 *
 * It accepts a small JSON document containing only approved preference changes.
 * Full application code updates must be delivered as an APK signed with the same
 * signing certificate as the installed application and installed through Android's
 * package installer with the user's confirmation.
 */
class UpdateScriptManager(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("aas_user_settings", Context.MODE_PRIVATE)

    data class Result(
        val success: Boolean,
        val message: String,
        val changedKeys: List<String> = emptyList()
    )

    /**
     * Applies only known settings. Unknown keys or invalid values reject the whole update.
     *
     * Example accepted JSON:
     * {
     *   "format": "aas-settings-v1",
     *   "changes": {
     *     "theme": "dark",
     *     "language": "ar",
     *     "seek_seconds": 15,
     *     "default_speed": 1.0
     *   }
     * }
     */
    fun applySettingsScript(script: String): Result {
        val root = try {
            JSONObject(script)
        } catch (_: Exception) {
            return Result(false, "صيغة التحديث غير صحيحة. استخدم JSON صالحاً.")
        }

        if (root.optString("format") != "aas-settings-v1") {
            return Result(false, "إصدار صيغة التحديث غير مدعوم.")
        }

        val changes = root.optJSONObject("changes")
            ?: return Result(false, "لم يتم العثور على قسم changes.")

        val allowed = setOf(
            "theme",
            "language",
            "seek_seconds",
            "default_speed",
            "sleep_timer_enabled"
        )

        val keys = changes.keys().asSequence().toList()
        if (keys.isEmpty()) return Result(false, "لا توجد تغييرات لتطبيقها.")

        val unknown = keys.filterNot { it in allowed }
        if (unknown.isNotEmpty()) {
            return Result(false, "رفض التحديث: توجد إعدادات غير مسموح بها: ${unknown.joinToString()}")
        }

        val editor = prefs.edit()

        for (key in keys) {
            when (key) {
                "theme" -> {
                    val value = changes.optString(key)
                    if (value !in setOf("dark", "light", "system")) {
                        return Result(false, "قيمة theme يجب أن تكون dark أو light أو system.")
                    }
                    editor.putString(key, value)
                }

                "language" -> {
                    val value = changes.optString(key)
                    if (value !in setOf("ar", "en")) {
                        return Result(false, "اللغة المدعومة حالياً هي ar أو en.")
                    }
                    editor.putString(key, value)
                }

                "seek_seconds" -> {
                    val value = changes.optInt(key, -1)
                    if (value !in setOf(5, 10, 15, 20, 30)) {
                        return Result(false, "قيمة seek_seconds يجب أن تكون 5 أو 10 أو 15 أو 20 أو 30.")
                    }
                    editor.putInt(key, value)
                }

                "default_speed" -> {
                    val value = changes.optDouble(key, -1.0)
                    if (value !in listOf(0.5, 0.75, 1.0, 1.25, 1.5, 2.0)) {
                        return Result(false, "سرعة التشغيل الافتراضية غير مدعومة.")
                    }
                    editor.putFloat(key, value.toFloat())
                }

                "sleep_timer_enabled" -> {
                    if (!changes.has(key) || changes.isNull(key)) {
                        return Result(false, "قيمة sleep_timer_enabled غير صالحة.")
                    }
                    editor.putBoolean(key, changes.getBoolean(key))
                }
            }
        }

        if (!editor.commit()) {
            return Result(false, "تعذر حفظ التغييرات.")
        }

        return Result(true, "تم حفظ إعدادات التحديث. قد تحتاج بعض التغييرات إلى إعادة فتح الشاشة.", keys)
    }

    fun currentSettings(): JSONObject {
        return JSONObject().apply {
            put("theme", prefs.getString("theme", "system"))
            put("language", prefs.getString("language", "ar"))
            put("seek_seconds", prefs.getInt("seek_seconds", 10))
            put("default_speed", prefs.getFloat("default_speed", 1.0f).toDouble())
            put("sleep_timer_enabled", prefs.getBoolean("sleep_timer_enabled", true))
        }
    }
}
