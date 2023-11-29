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

    // Результат попытки логина:
    // Успешный статус будем использовать для автоматического возврата в предыдущий фрагмент
    val isAuthorized: Boolean
        get() = AppAuth.getInstance().data.value != null    // Берем StateFlow и проверяем

    private val _loginSuccessEvent = SingleLiveEvent<Unit>()
    val loginSuccessEvent: LiveData<Unit>
        get() = _loginSuccessEvent

    private val _loginError = MutableLiveData<String?>(null)
    val loginError: LiveData<String?>
        get() = _loginError


    // Информация для входа и состояние готовности ко входу:
    // Информация для входа должна проверяться на полноту до того, как будет попытка входа
    private val _loginInfo = MutableLiveData(LoginInfo()) // TODO - отследить нажатие клавиатуры
    val loginInfo: LiveData<LoginInfo>
        get() = _loginInfo

    private val _completionWarningSet = MutableLiveData(emptySet<Int>())
    val completionWarningSet: LiveData<Set<Int>>
        get() = _completionWarningSet


    // - - - - - - - - - - - - - - - - -
    //Попытка входа

    fun doLogin() {

        if (!completed()) return //false

        // Сброс ошибки логина перед новой попыткой (возможно, осталась от предыдущей попытки)
        _loginError.value = null

        // Отправить запрос авторизации на сервер
        viewModelScope.launch {
            try {
                updateUser()
                //delay(500)
                if (isAuthorized)
                    _loginSuccessEvent.value = Unit  // Однократное событие
                else
                    _loginError.value = "Unknown login error!"

            } catch (e: Exception) {
                ConsolePrinter.printText("CATCH OF UPDATE USER - ${e.message.toString()}")
                // Установка ошибки логина
                _loginError.value = e.message.toString()
            }
        } // end of launch

        //return isAuthorized
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
            throw RuntimeException(response?.message() ?: "No server response")
        }
        val responseToken = response?.body() ?: throw RuntimeException("body is null")

        // Надо прогрузить токен в AppAuth
        AppAuth.getInstance().setToken(responseToken)

    }

    // - - - - - - - - - - - - - - - - -
    // Обработка информации для входа

    fun resetLoginInfo(newLoginInfo: LoginInfo) {
        // Новая информация для входа
        _loginInfo.value = newLoginInfo

        // Сброс ошибок логина (мы еще не ошибались с новой информацией для входа)
        _loginError.value = null

        // Проверка полноты информации для входа, установка набора предупреждений
        var warnIdSet = emptySet<Int>()

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
