package com.kobaatv.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ChannelsViewModel(
    private val failover: FailoverManager,
    // Builds the repository for the account failover settled on.
    private val repositoryFor: (Account) -> XtreamRepository = {
        XtreamRepository(it.host, it.username, it.password)
    },
) : ViewModel() {

    /** Why a connection attempt failed; the UI maps this to a localized string. */
    enum class FailReason { NO_NETWORK, ALL_FAILED, NO_ACCOUNTS, LOAD_ERROR }

    sealed interface State {
        data object Loading : State
        data class Loaded(val channels: List<XtreamClient.Channel>) : State
        data class Error(val reason: FailReason) : State
    }

    // Persistent screen state — drives the list and the account-health dot. An
    // Error stays Error so the dot stays red until the next (re)load.
    private val _state = MutableStateFlow<State>(State.Loading)
    val state: StateFlow<State> = _state.asStateFlow()

    // One-shot failure reasons for transient toasts, kept separate from `state`
    // so a re-collect does not re-toast.
    private val _events = MutableSharedFlow<FailReason>(extraBufferCapacity = 1)
    val events: SharedFlow<FailReason> = _events.asSharedFlow()

    // Repository for the account currently connected, used for stream URLs.
    private var repository: XtreamRepository? = null

    init {
        load()
    }

    /** Connects (with failover) then loads that account's channel list. */
    fun load() {
        _state.value = State.Loading
        viewModelScope.launch {
            when (val outcome = failover.connect()) {
                is FailoverManager.Outcome.Connected -> {
                    val repo = repositoryFor(outcome.account).also { repository = it }
                    repo.liveStreams()
                        .onSuccess { _state.value = State.Loaded(it) }
                        .onFailure { fail(FailReason.LOAD_ERROR) }
                }
                is FailoverManager.Outcome.NoNetwork -> fail(FailReason.NO_NETWORK)
                is FailoverManager.Outcome.AllFailed -> fail(FailReason.ALL_FAILED)
                is FailoverManager.Outcome.NoAccounts -> fail(FailReason.NO_ACCOUNTS)
            }
        }
    }

    private fun fail(reason: FailReason) {
        _state.value = State.Error(reason)
        _events.tryEmit(reason)
    }

    fun hlsUrl(streamId: Int): String = repository?.hlsUrl(streamId).orEmpty()
}
