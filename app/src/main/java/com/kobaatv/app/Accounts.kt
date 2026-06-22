package com.kobaatv.app

import android.content.Context
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Stores the list of Xtream accounts as a JSON array in the existing
 * SharedPreferences file, plus the id of the last account that worked.
 *
 * On first read it migrates the legacy single-account keys
 * (xt_host/xt_user/xt_pass) into the array so existing users keep their
 * account as #1.
 */
object Accounts {
    private const val KEY_ACCOUNTS = "accounts_json"
    private const val KEY_LAST_GOOD = "last_good_account_id"

    fun list(ctx: Context): List<Account> {
        val prefs = Prefs.get(ctx)
        val raw = prefs.getString(KEY_ACCOUNTS, null)
        if (raw == null) {
            val migrated = migrateLegacy(ctx)
            save(ctx, migrated)
            return migrated
        }
        return parse(raw)
    }

    fun save(ctx: Context, accounts: List<Account>) {
        val arr = JSONArray()
        accounts.forEach { a ->
            arr.put(
                JSONObject()
                    .put("id", a.id)
                    .put("host", a.host)
                    .put("username", a.username)
                    .put("password", a.password),
            )
        }
        Prefs.get(ctx).edit { putString(KEY_ACCOUNTS, arr.toString()) }
    }

    fun add(ctx: Context, host: String, username: String, password: String): Account {
        val account = Account(UUID.randomUUID().toString(), host.trimEnd('/'), username, password)
        save(ctx, list(ctx) + account)
        return account
    }

    /**
     * Adds the account if new, or updates the password of a matching
     * (host + username) one, then marks it as the last-good account. Used by the
     * login screen so signing in adds to the account list and becomes active.
     */
    fun upsertAndActivate(ctx: Context, host: String, username: String, password: String): Account {
        val cleanHost = host.trimEnd('/')
        val existing = list(ctx).firstOrNull { it.host == cleanHost && it.username == username }
        val account = if (existing != null) {
            val updated = existing.copy(password = password)
            save(ctx, list(ctx).map { if (it.id == existing.id) updated else it })
            updated
        } else {
            add(ctx, cleanHost, username, password)
        }
        setLastGood(ctx, account.id)
        return account
    }

    fun remove(ctx: Context, id: String) {
        save(ctx, list(ctx).filterNot { it.id == id })
    }

    fun lastGoodId(ctx: Context): String? = Prefs.get(ctx).getString(KEY_LAST_GOOD, null)

    fun setLastGood(ctx: Context, id: String) {
        Prefs.get(ctx).edit { putString(KEY_LAST_GOOD, id) }
    }

    /**
     * The account to connect with: the last one that worked, falling back to the
     * first in the list. Null when no accounts are configured.
     */
    fun active(ctx: Context): Account? =
        // Same "last-good first, else first" ordering the failover engine uses.
        Failover.orderedCandidates(list(ctx), lastGoodId(ctx)).firstOrNull()

    private fun parse(raw: String): List<Account> {
        val arr = JSONArray(raw)
        return (0 until arr.length()).map {
            val o = arr.getJSONObject(it)
            Account(
                id = o.optString("id"),
                host = o.optString("host"),
                username = o.optString("username"),
                password = o.optString("password"),
            )
        }
    }

    private fun migrateLegacy(ctx: Context): List<Account> {
        val host = Prefs.host(ctx)
        val user = Prefs.user(ctx)
        val pass = Prefs.pass(ctx)
        return if (host.isNotEmpty() && user.isNotEmpty()) {
            listOf(Account(UUID.randomUUID().toString(), host, user, pass))
        } else {
            emptyList()
        }
    }
}
