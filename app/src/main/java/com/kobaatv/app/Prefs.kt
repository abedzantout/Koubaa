package com.kobaatv.app

import android.content.Context
import androidx.core.content.edit

object Prefs {
    private const val FILE = "kobaa_prefs"
    private const val KEY_HOST = "xt_host"
    private const val KEY_USER = "xt_user"
    private const val KEY_PASS = "xt_pass"
    private const val KEY_LANG = "app_lang" // "ar" | "en" | "system"

    fun get(ctx: Context) = ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    // Legacy single-account getters, kept only so Accounts can migrate the old
    // xt_host/xt_user/xt_pass values into the account list on first run.
    fun host(ctx: Context) = get(ctx).getString(KEY_HOST, "") ?: ""
    fun user(ctx: Context) = get(ctx).getString(KEY_USER, "") ?: ""
    fun pass(ctx: Context) = get(ctx).getString(KEY_PASS, "") ?: ""

    fun lang(ctx: Context) = get(ctx).getString(KEY_LANG, "system") ?: "system"
    fun setLang(ctx: Context, lang: String) = get(ctx).edit { putString(KEY_LANG, lang) }
}
