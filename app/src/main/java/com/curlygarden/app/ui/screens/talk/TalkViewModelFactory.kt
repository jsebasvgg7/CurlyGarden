package com.curlygarden.app.ui.screens.talk

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class TalkViewModelFactory : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = TalkViewModel() as T
}
