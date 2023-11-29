package ru.netology.nmedia.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import retrofit2.Response
import ru.netology.nmedia.api.PostsApi
import ru.netology.nmedia.auth.AppAuth
import ru.netology.nmedia.auth.LoginInfo
import ru.netology.nmedia.dto.Token
import ru.netology.nmedia.util.ConsolePrinter
import ru.netology.nmedia.util.SingleLiveEvent
import ru.netology.nmedia.R

class LoginViewModel : ViewModel() {

    // Успешный статус будем использовать для автоматического возврата в предыдущий фрагмент
    val isAuthorized: Boolean
        get() = AppAuth.getInstance().data.value != null    // Берем StateFlow и проверяем

    private val _loginSuccess = SingleLiveEvent<Unit>()
    val loginSuccess: LiveData<Unit>
        get() = _loginSuccess

    // TODO Еще надо сделать набор ошибок логина:
    //  это был плохой логин или сервер не отвечает?


    // Информация для входа должна проверяться на полноту до того, как будет попытка входа
    private val _loginInfo = MutableLiveData(LoginInfo()) // TODO - отследить нажатие клавиатуры
    val loginInfo: LiveData<LoginInfo>
        get() = _loginInfo

    private val _completionWarningSet = MutableLiveData(emptySet<Int>())
    val completionWarningSet: LiveData<Set<Int>>
        get() = _completionWarningSet


    // - - - - - - - - - - - - - - - - -
    //Попытка входа

    fun doLogin(): Boolean {

        if (!completed()) return false // Хорошо бы возвращать SingleLiveEvent - текст предупреждения

        // Отправить запрос авторизации на сервер
        viewModelScope.launch {
            try {
                updateUser()
                if (isAuthorized) _loginSuccess.value = Unit  // Однократное событие
            } catch (e: Exception) {
                ConsolePrinter.printText("CATCH OF UPDATE USER - ${e.message.toString()}")
            }
        } // end of launch
        return isAuthorized
    }

    suspend fun updateUser() {
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

        // Надо прогрузить токен в AppAuth
        AppAuth.getInstance().setToken(responseToken)

    }

    // - - - - - - - - - - - - - - - - -
    // Обработка информации для входа

    fun resetLoginInfo(newLoginInfo: LoginInfo) {
        // Новая информация для входа
        _loginInfo.value = newLoginInfo

        // Проверка полноты информации для входа
        var warnIdSet = emptySet<Int>()

        //Почему-то не заходим в этот блок
        // ВОЗМОЖНО, ЕСТЬ КОНФЛИКТ ЧТЕНИЯ? ПОДПИСКА?

        loginInfo.value?.let {
            if (it.login.length == 0) warnIdSet.plus(R.string.warning_no_login)
            if (it.password.length == 0) warnIdSet.plus(R.string.warning_no_password)
        }
        _completionWarningSet.value = warnIdSet

    }

    fun completed(): Boolean {
        return _completionWarningSet.value?.count() == 0
    }

}
