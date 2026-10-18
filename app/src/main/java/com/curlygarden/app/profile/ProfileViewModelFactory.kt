package com.curlygarden.app.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class ProfileViewModelFactory : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val repository = ProfileRepository()
        @Suppress("UNCHECKED_CAST")
        return ProfileViewModel(repository) as T
    }
}
