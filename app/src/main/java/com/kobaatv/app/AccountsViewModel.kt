package com.kobaatv.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Backs the Accounts screen. On open it probes every account in parallel
 * (login) so the row can show live expiry and connection counts. Holds the
 * account list and the active account id for display.
 */
class AccountsViewModel(app: Application) : AndroidViewModel(app) {

    /** Per-account probe result shown in a row. */
    sealed interface Probe {
        data object Checking : Probe
        data class Ok(val info: XtreamClient.AccountInfo) : Probe
        data object Unreachable : Probe
    }

    data class Row(
        val account: Account,
        val isActive: Boolean,
        val probe: Probe,
    )

    private val _rows = MutableStateFlow<List<Row>>(emptyList())
    val rows: StateFlow<List<Row>> = _rows.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        val ctx = getApplication<Application>()
        val accounts = Accounts.list(ctx)
        val activeId = Accounts.active(ctx)?.id
        // Seed rows as "checking" immediately, then fill in each probe result.
        _rows.value = accounts.map { Row(it, it.id == activeId, Probe.Checking) }

        viewModelScope.launch {
            val probed = accounts.map { account ->
                async {
                    account to XtreamRepository(account.host, account.username, account.password)
                        .login()
                        .fold(
                            onSuccess = { Probe.Ok(it) },
                            onFailure = { Probe.Unreachable },
                        )
                }
            }.awaitAll()

            val probeById = probed.associate { (a, p) -> a.id to p }
            _rows.value = accounts.map {
                Row(it, it.id == activeId, probeById[it.id] ?: Probe.Unreachable)
            }
        }
    }

    fun remove(id: String) {
        Accounts.remove(getApplication(), id)
        refresh()
    }
}
