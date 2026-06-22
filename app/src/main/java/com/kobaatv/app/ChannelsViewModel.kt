package com.kobaatv.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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

    private val _state = MutableStateFlow<State>(State.Loading)
    val state: StateFlow<State> = _state.asStateFlow()

    init {
        load()
    }

    /** Reloads the channel list. Safe to call again, e.g. for a retry. */
    fun load() {
        _state.value = State.Loading
        viewModelScope.launch {
            repository.liveStreams()
                .onSuccess { _state.value = State.Loaded(it) }
                .onFailure { _state.value = State.Error(it.message) }
        }
    }

    /**
     * Marks an emitted Error as handled by collapsing to an empty Loaded state,
     * so re-collecting the flow (e.g. after a rotation) does not replay the
     * error and show the toast again.
     */
    fun consumeError() {
        if (_state.value is State.Error) _state.value = State.Loaded(emptyList())
    }

    fun hlsUrl(streamId: Int): String = repository.hlsUrl(streamId)
}
