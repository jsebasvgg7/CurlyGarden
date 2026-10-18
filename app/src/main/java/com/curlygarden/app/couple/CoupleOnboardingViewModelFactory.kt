package com.curlygarden.app.couple

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class CoupleOnboardingViewModelFactory : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val repository = CoupleRepository()
        @Suppress("UNCHECKED_CAST")
        return CoupleOnboardingViewModel(repository) as T
    }
}
