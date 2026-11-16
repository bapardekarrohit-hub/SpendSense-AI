package com.rohit.spendsense

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import java.util.Locale

object LocaleHelper {
    private const val KEY = "app_language"

    /** code "" means "follow the phone's language". */
    val options = listOf(
        "" to "",
        "en" to "English",
        "hi" to "हिन्दी",
        "mr" to "मराठी"
    )

    fun saved(context: Context): String =
        context.getSharedPreferences("spendsense_prefs", Context.MODE_PRIVATE)
            .getString(KEY, "") ?: ""

    fun nameOf(code: String, systemLabel: String): String =
        if (code.isEmpty()) systemLabel else options.firstOrNull { it.first == code }?.second ?: code

    /** Wraps the base context so the whole app uses the chosen language. */
    fun wrap(base: Context): Context {
        val code = saved(base)
        if (code.isEmpty()) return base
        val locale = Locale.forLanguageTag(code)
        Locale.setDefault(locale)
        val config = Configuration(base.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        return base.createConfigurationContext(config)
    }

    fun apply(context: Context, code: String) {
        if (code == saved(context)) return
        context.getSharedPreferences("spendsense_prefs", Context.MODE_PRIVATE)
            .edit().putString(KEY, code).apply()
        var c: Context? = context
        while (c is ContextWrapper) {
            if (c is Activity) {
                c.recreate()
                return
            }
            c = c.baseContext
        }
    }
}