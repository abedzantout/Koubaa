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
    private val repository: XtreamRepository,
) : ViewModel() {

    sealed interface State {
        data object Loading : State
        data class Loaded(val channels: List<XtreamClient.Channel>) : State
        data class Error(val message: String?) : State
    }

    // Persistent screen state — drives the list and the account-health dot. An
    // Error stays Error so the dot stays red until the next (re)load.
    private val _state = MutableStateFlow<State>(State.Loading)
    val state: StateFlow<State> = _state.asStateFlow()

    // One-shot error messages for the toast, kept separate from `state` so a
    // re-collect (rotation, return to foreground) does not re-toast.
    private val _errors = MutableSharedFlow<String?>(extraBufferCapacity = 1)
    val errors: SharedFlow<String?> = _errors.asSharedFlow()

    init {
        load()
    }

    /** Reloads the channel list. Safe to call again, e.g. for a retry. */
    fun load() {
        _state.value = State.Loading
        viewModelScope.launch {
            repository.liveStreams()
                .onSuccess { _state.value = State.Loaded(it) }
                .onFailure {
                    _state.value = State.Error(it.message)
                    _errors.tryEmit(it.message)
                }
        }
    }

    fun hlsUrl(streamId: Int): String = repository.hlsUrl(streamId)
}
