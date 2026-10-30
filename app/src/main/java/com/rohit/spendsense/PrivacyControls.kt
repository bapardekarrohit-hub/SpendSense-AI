package com.rohit.spendsense

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.WindowManager

object PrivacyControls {
    private const val KEY_BLOCK = "block_screenshots"

    private fun prefs(context: Context) =
        context.getSharedPreferences("spendsense_prefs", Context.MODE_PRIVATE)

    private fun activityOf(context: Context): Activity? {
        var c: Context? = context
        while (c is ContextWrapper) {
            if (c is Activity) return c
            c = c.baseContext
        }
        return null
    }

    fun isBlocked(context: Context): Boolean = prefs(context).getBoolean(KEY_BLOCK, false)

    fun setBlocked(context: Context, on: Boolean) {
        prefs(context).edit().putBoolean(KEY_BLOCK, on).apply()
        apply(context)
    }

    /** Applies the saved choice to the current window. Call it when the app starts. */
    fun apply(context: Context) {
        val window = activityOf(context)?.window ?: return
        if (isBlocked(context)) {
            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    fun clearAiData(context: Context) {
        prefs(context).edit()
            .remove("ai_server_url")
            .remove("dismissed_anomalies")
            .apply()
    }
}