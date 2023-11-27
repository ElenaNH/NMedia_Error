package ru.netology.nmedia.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import retrofit2.Response
import ru.netology.nmedia.api.PostsApi
import ru.netology.nmedia.auth.LoginInfo
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.dto.Token
import ru.netology.nmedia.util.ConsolePrinter

class LoginViewModel : ViewModel() {

    private val _loginInfo = MutableLiveData(LoginInfo())
    val loginInfo: LiveData<LoginInfo>
        get() = _loginInfo

    fun doLogin(): Boolean {

        if (!completed()) return false

        // Отправить запрос авторизации на сервер
        viewModelScope.launch {
            try {
                updateUser()
            } catch (e: Exception) {
                ConsolePrinter.printText("CATCH OF UPDATE USER - ${e.message.toString()}")
            }


        } // end of launch
        return true  //TODO - вернуть реальный результат
    }

    fun resetLoginInfo(newLoginInfo: LoginInfo) {
        _loginInfo.value = newLoginInfo
    }

    fun completed(): Boolean {
        return loginInfo.value?.let {
            (it.login.length > 0) && (it.password.length > 0)
        } ?: false
    }

    suspend fun updateUser(): Boolean {
        var response: Response<Token>? = null
        try {
            response = PostsApi.retrofitService.updateUser(
                loginInfo.value?.login ?: "",
                loginInfo.value?.password ?: ""
            )

        } catch (e: Exception) {
            throw RuntimeException(e.message.toString())
        }

        if (!(response?.isSuccessful ?: false)) {
            throw RuntimeException(response?.message() ?: "No server response (=no token)")
        }
        val responseToken = response?.body() ?: throw RuntimeException("body is null (=no token)")

        // Как-то надо прогрузить токен в AppAuth???

        return true // Если сюда добрались, то все сделано
    }

}
