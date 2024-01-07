package ru.netology.nmedia.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import retrofit2.Response
import ru.netology.nmedia.R
import ru.netology.nmedia.api.ApiService
import ru.netology.nmedia.auth.AppAuth
import ru.netology.nmedia.auth.LoginInfo
import ru.netology.nmedia.dto.Token
import ru.netology.nmedia.repository.PostRepository
import ru.netology.nmedia.util.ConsolePrinter
import ru.netology.nmedia.util.SingleLiveEvent
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val apiService: ApiService,
    private val appAuth: AppAuth,
) : ViewModel() {

    // Результат попытки логина:
    // Успешный статус будем использовать для автоматического возврата в предыдущий фрагмент
    val isAuthorized: Boolean
        get() = appAuth.data.value != null    // Берем StateFlow и проверяем

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

        if (!completed()) {
            // Если мы все правильно сделали, то эта ошибка не должна возникать
            _loginError.value = "Login with uncompleted status error!"
            return
        }

        // Сброс ошибки логина перед новой попыткой (возможно, осталась от предыдущей попытки)
        _loginError.value = null

        // Отправить запрос авторизации на сервер
        viewModelScope.launch {
            try {
                updateUser()
                if (isAuthorized)
                    _loginSuccessEvent.value = Unit  // Однократное событие
                else {
                    // Этого быть не должно, т.к. updateUser в случае неуспеха выдает ошибку
                    _loginError.value = "Unexpected login error!"
                }
            } catch (e: Exception) {
                ConsolePrinter.printText("CATCH OF UPDATE USER - ${e.message.toString()}")
                // Установка ошибки логина
                val errText = if (e.message.toString() == "") "Unknown login error!"
                else e.message.toString()
                _loginError.value = errText
            }
        } // end of launch

    }

    private suspend fun updateUser() {
        var response: Response<Token>? = null
        try {
            response = apiService.updateUser(
                loginInfo.value?.login ?: "",
                loginInfo.value?.password ?: ""
            )

        } catch (e: Exception) {
            // Обычно сюда попадаем, если нет ответа сервера
            throw RuntimeException("Server response failed: ${e.message.toString()}")
        }

        if (!(response?.isSuccessful ?: false)) {
            // А сюда попадаем, потому что сервер вернул isSuccessful == false
            val errText = if ((response?.message() == null) || (response?.message() == ""))
                "No server response" else response.message()
            throw RuntimeException("Request declined: $errText")
        }
        val responseToken = response?.body() ?: throw RuntimeException("body is null")

        // Надо прогрузить токен в AppAuth
//        DependencyContainer.getInstance().appAuth.setToken(responseToken)
        appAuth.setTokenAndLogin(
            responseToken,
            loginInfo.value?.login ?: ""
        )

    }

    // - - - - - - - - - - - - - - - - -
    // Обработка информации для входа

    fun resetLoginInfo(newLoginInfo: LoginInfo) {
        // Сброс ошибок логина (мы еще не ошибались с новой информацией для входа)
        _loginError.value = null

        // Новая информация для входа
        _loginInfo.value = newLoginInfo

        // Проверка полноты информации для входа, установка набора предупреждений
        val warnIdSet = mutableSetOf<Int>()

        loginInfo.value?.let {
            if (it.login.length == 0) {
                warnIdSet.add(R.string.warning_no_login)

            }
            if (it.password.length == 0) {
                warnIdSet.add(R.string.warning_no_password)
            }
        }
        _completionWarningSet.value = warnIdSet

        // TODO - Подумать, как еще можно обеспечить синхронизацию обработки недостаточних данных
        viewModelScope.launch { delay(300) } // Строго после установки WarningSet!!!
    }

    fun completed(): Boolean {
        return _completionWarningSet.value?.count() == 0
    }

}
