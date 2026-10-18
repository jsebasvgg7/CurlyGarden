package com.curlygarden.app.ui.screens.draw

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class DrawViewModelFactory : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return DrawViewModel() as T
    }
}
