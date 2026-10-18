package com.curlygarden.app.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class HomeViewModelFactory : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val repository = HomeRepository()
        @Suppress("UNCHECKED_CAST")
        return HomeViewModel(repository) as T
    }
}
