package com.kobaatv.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LoginViewModel : ViewModel() {

    sealed interface State {
        data object Idle : State
        data object Submitting : State
        data class Success(val host: String, val user: String, val pass: String) : State
        data class Error(val message: String?) : State
    }

    private val _state = MutableStateFlow<State>(State.Idle)
    val state: StateFlow<State> = _state.asStateFlow()

    fun login(host: String, user: String, pass: String) {
        // Ignore re-taps while a login is already in flight.
        if (_state.value is State.Submitting) return
        _state.value = State.Submitting
        viewModelScope.launch {
            XtreamRepository(host, user, pass).login()
                .onSuccess { _state.value = State.Success(host, user, pass) }
                .onFailure { _state.value = State.Error(it.message) }
        }
    }

    /** Lets the activity reset to Idle after consuming a one-off Error. */
    fun clearError() {
        if (_state.value is State.Error) _state.value = State.Idle
    }
}
