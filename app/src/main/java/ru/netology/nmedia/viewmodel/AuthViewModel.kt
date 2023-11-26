package ru.netology.nmedia.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import ru.netology.nmedia.auth.AppAuth
import ru.netology.nmedia.dto.Token

class AuthViewModel: ViewModel() {
    val data: LiveData<Token?> = AppAuth.getInstance().data
        .asLiveData()

    val isAuthorized: Boolean
        get() = AppAuth.getInstance().data.value != null
}
