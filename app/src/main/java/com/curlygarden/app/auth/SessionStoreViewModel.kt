package com.curlygarden.app.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner

class SessionStoreViewModel : ViewModel(), ViewModelStoreOwner {

    override var viewModelStore: ViewModelStore = ViewModelStore()
        private set

    fun reset() {
        viewModelStore.clear()
        viewModelStore = ViewModelStore()
    }

    override fun onCleared() {
        viewModelStore.clear()
    }
}
