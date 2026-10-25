package com.curlygarden.app.ui.screens.memories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class MemoriesViewModelFactory : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = MemoriesViewModel() as T
}
