package com.curlygarden.app.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/**
 * A diferencia de AuthViewModelFactory, este no necesita contexto: Home
 * solo lee Firestore, no lanza ningun selector de sistema que requiera
 * una Activity.
 */
class HomeViewModelFactory : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val repository = HomeRepository()
        @Suppress("UNCHECKED_CAST")
        return HomeViewModel(repository) as T
    }
}
