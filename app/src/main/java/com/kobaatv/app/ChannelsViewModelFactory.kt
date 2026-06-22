package com.kobaatv.app

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/**
 * Builds ChannelsViewModel with a repository created from the active Xtream
 * account. Reads the account once at construction so the ViewModel itself stays
 * free of Android dependencies.
 */
class ChannelsViewModelFactory(context: Context) : ViewModelProvider.Factory {
    private val active = Accounts.active(context)
    private val repository = XtreamRepository(
        host = active?.host.orEmpty(),
        username = active?.username.orEmpty(),
        password = active?.password.orEmpty(),
    )

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(ChannelsViewModel::class.java)) {
            "Unknown ViewModel class: ${modelClass.name}"
        }
        return ChannelsViewModel(repository) as T
    }
}
