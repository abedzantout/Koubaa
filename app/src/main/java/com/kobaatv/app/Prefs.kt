package com.kobaatv.app

import android.content.Context
import androidx.core.content.edit

object Prefs {
    private const val FILE = "kobaa_prefs"
    const val KEY_HOST = "xt_host"
    const val KEY_USER = "xt_user"
    const val KEY_PASS = "xt_pass"
    const val KEY_LANG = "app_lang" // "ar" | "en" | "system"

    fun get(ctx: Context) = ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun saveXtream(ctx: Context, host: String, user: String, pass: String) {
        get(ctx).edit {
            putString(KEY_HOST, host.trimEnd('/'))
            putString(KEY_USER, user)
            putString(KEY_PASS, pass)
        }
    }

    fun host(ctx: Context) = get(ctx).getString(KEY_HOST, "") ?: ""
    fun user(ctx: Context) = get(ctx).getString(KEY_USER, "") ?: ""
    fun pass(ctx: Context) = get(ctx).getString(KEY_PASS, "") ?: ""

    fun lang(ctx: Context) = get(ctx).getString(KEY_LANG, "system") ?: "system"
    fun setLang(ctx: Context, lang: String) = get(ctx).edit { putString(KEY_LANG, lang) }
}
