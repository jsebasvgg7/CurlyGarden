package com.curlygarden.app.ui.screens.imagine

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class ImagineViewModelFactory : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = ImagineViewModel() as T
}
