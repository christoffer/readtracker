package com.readtracker.android.integration_test_utils

import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import com.readtracker.android.ReadTrackerApp
import java.util.*

fun getContextWithLocale(context: Context, language: String, country: String): ContextWrapper {
    val locale = Locale(language, country)
    Locale.setDefault(locale)
    val res = context.resources
    val config = res.configuration
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        config.setLocale(locale)
    } else {
        @Suppress("DEPRECATION")
        config.locale = locale
    }

    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
        @Suppress("DEPRECATION")
        res.updateConfiguration(config, res.displayMetrics)
        return ContextWrapper(context)
    }
    return ContextWrapper(context.createConfigurationContext(config))
}

fun getAppContext(): ReadTrackerApp {
    return ApplicationProvider.getApplicationContext()
}
