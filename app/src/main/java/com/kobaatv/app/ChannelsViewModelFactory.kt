package com.kobaatv.app

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/**
 * Builds ChannelsViewModel with a repository created from the stored Xtream
 * credentials. Reads Prefs once at construction so the ViewModel itself stays
 * free of Android dependencies.
 */
class ChannelsViewModelFactory(context: Context) : ViewModelProvider.Factory {
    private val repository = XtreamRepository(
        host = Prefs.host(context),
        username = Prefs.user(context),
        password = Prefs.pass(context),
    )

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(ChannelsViewModel::class.java)) {
            "Unknown ViewModel class: ${modelClass.name}"
        }
        return ChannelsViewModel(repository) as T
    }
}
