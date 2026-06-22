package com.kobaatv.app

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * Drives [Failover] against the real device: checks validated connectivity and
 * tries each candidate account's login until one works. On success it records
 * the account as last-good so the next connection starts there.
 */
class FailoverManager(
    private val appContext: Context,
    private val io: CoroutineDispatcher = Dispatchers.IO,
    // Injectable so tests can stand in for the real login.
    private val probe: suspend (Account) -> Result<XtreamClient.AccountInfo> = { account ->
        XtreamRepository(account.host, account.username, account.password, io).login()
    },
) {

    sealed interface Outcome {
        data class Connected(val account: Account, val info: XtreamClient.AccountInfo) : Outcome
        data object NoNetwork : Outcome
        data object AllFailed : Outcome
        data object NoAccounts : Outcome
    }

    /**
     * Finds a working account, trying the last-good one first. Stops without
     * cycling when there is no validated internet.
     */
    suspend fun connect(): Outcome {
        val accounts = Accounts.list(appContext)
        val activeId = Accounts.active(appContext)?.id
        val tried = mutableSetOf<String>()

        while (true) {
            when (val decision = Failover.next(accounts, activeId, tried, hasValidatedInternet())) {
                is Failover.Decision.NoAccounts -> return Outcome.NoAccounts
                is Failover.Decision.NoNetwork -> return Outcome.NoNetwork
                is Failover.Decision.AllFailed -> return Outcome.AllFailed
                is Failover.Decision.Try -> {
                    val account = decision.account
                    tried += account.id
                    probe(account).onSuccess { info ->
                        Accounts.setLastGood(appContext, account.id)
                        return Outcome.Connected(account, info)
                    }
                    // On failure, loop: next() picks the next untried account, or
                    // re-checks connectivity (which may have dropped meanwhile).
                }
            }
        }
    }

    private fun hasValidatedInternet(): Boolean {
        val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}
