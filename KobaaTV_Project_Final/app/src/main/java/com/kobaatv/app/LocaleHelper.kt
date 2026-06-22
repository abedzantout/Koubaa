package com.kobaatv.app

import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import java.util.Locale

object LocaleHelper {
    fun wrap(ctx: Context): ContextWrapper {
        val lang = Prefs.lang(ctx)
        if (lang == "system") return ContextWrapper(ctx)
        val locale = Locale(lang)
        Locale.setDefault(locale)
        val config = Configuration(ctx.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        return ContextWrapper(ctx.createConfigurationContext(config))
    }
}
