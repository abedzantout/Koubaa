package com.kobaatv.app

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/**
 * Builds ChannelsViewModel with a FailoverManager bound to the application
 * context, so the ViewModel can pick a working account (with failover) before
 * loading channels.
 */
class ChannelsViewModelFactory(context: Context) : ViewModelProvider.Factory {
    private val failover = FailoverManager(context.applicationContext)

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(ChannelsViewModel::class.java)) {
            "Unknown ViewModel class: ${modelClass.name}"
        }
        return ChannelsViewModel(failover) as T
    }
}
