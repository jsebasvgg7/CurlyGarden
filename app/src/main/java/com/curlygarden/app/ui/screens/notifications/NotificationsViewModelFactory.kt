package com.curlygarden.app.ui.screens.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class NotificationsViewModelFactory : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = NotificationsViewModel() as T
}
