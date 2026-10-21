package com.curlygarden.app.ui.screens.play

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class PlayViewModelFactory : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = PlayViewModel() as T
}
