package com.curlygarden.app.auth

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.curlygarden.app.R

class AuthViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val repository = AuthRepository(
            context = context,
            webClientId = context.getString(R.string.default_web_client_id)
        )
        @Suppress("UNCHECKED_CAST")
        return AuthViewModel(repository) as T
    }
}