package ru.netology.nmedia.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import retrofit2.Response
import ru.netology.nmedia.api.PostsApi
import ru.netology.nmedia.auth.AppAuth
import ru.netology.nmedia.auth.RegisterInfo
import ru.netology.nmedia.dto.Token
import ru.netology.nmedia.util.ConsolePrinter
import ru.netology.nmedia.util.SingleLiveEvent
import ru.netology.nmedia.R

class RegisterViewModel : ViewModel() {

    // Результат попытки регистрации:
    // Успешный статус будем использовать для автоматического возврата в предыдущий фрагмент
    val isAuthorized: Boolean
        get() = AppAuth.getInstance().data.value != null    // Берем StateFlow и проверяем

    private val _registerSuccessEvent = SingleLiveEvent<Unit>()
    val registerSuccessEvent: LiveData<Unit>
        get() = _registerSuccessEvent

    private val _registerError = MutableLiveData<String?>(null)
    val registerError: LiveData<String?>
        get() = _registerError

    // Информация для регистрации и состояние готовности к регистрации:
    // Информация для регистрации должна проверяться на полноту до того, как будет попытка регистрации
    private val _registerInfo =
        MutableLiveData(RegisterInfo()) // TODO - отследить нажатие клавиатуры
    val registerInfo: LiveData<RegisterInfo>
        get() = _registerInfo

    private val _completionWarningSet = MutableLiveData(emptySet<Int>())
    val completionWarningSet: LiveData<Set<Int>>
        get() = _completionWarningSet


    // - - - - - - - - - - - - - - - - -
    //Попытка регистрации

    fun doRegister() {

        if (!completed()) {
            // Если мы все правильно сделали, то эта ошибка не должна возникать
            _registerError.value = "Register with uncompleted status error!"
            return
        }

        // Сброс ошибки регистрации перед новой попыткой (возможно, осталась от предыдущей попытки)
        _registerError.value = null

        // Отправить запрос регистрации на сервер
        viewModelScope.launch {
            try {
                registerUser()
                if (isAuthorized)
                    _registerSuccessEvent.value = Unit  // Однократное событие
                else {
                    // Этого быть не должно, т.к. registerUser в случае неуспеха выдает ошибку
                    _registerError.value = "Unexpected register error!"
                }
            } catch (e: Exception) {
                ConsolePrinter.printText("CATCH OF REGISTER USER - ${e.message.toString()}")
                // Установка ошибки регистрации
                val errText = if (e.message.toString() == "") "Unknown register error!"
                else e.message.toString()
                _registerError.value = errText
            }
        } // end of launch

    }

    private suspend fun registerUser() {
        var response: Response<Token>? = null
        try {
            response = PostsApi.retrofitService.registerUser(
                registerInfo.value?.login ?: "",
                registerInfo.value?.password ?: "",
                registerInfo.value?.username ?: "",
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
        AppAuth.getInstance().setToken(responseToken)

    }

    // - - - - - - - - - - - - - - - - -
    // Обработка информации для регистрации

    fun resetRegisterInfo(newRegisterInfo: RegisterInfo) {
        // Сброс ошибок регистрации (мы еще не ошибались с новой информацией для регистрации)
        _registerError.value = null

        // Новая информация для регистрации
        _registerInfo.value = newRegisterInfo

        // Проверка полноты информации для регистрации, установка набора предупреждений
        val warnIdSet = mutableSetOf<Int>()

        registerInfo.value?.let {
            if (it.login.length == 0) {
                warnIdSet.add(R.string.warning_no_login)
            }
            if (it.password.length == 0) {
                warnIdSet.add(R.string.warning_no_password)
            }
            if (it.password.length != it.password2.length) {
                warnIdSet.add(R.string.warning_no_matching_password)
            }
            if (it.username.length == 0) {
                warnIdSet.add(R.string.warning_no_username)
            }
            //Позже добавим проверку наличия аватара
        }
        _completionWarningSet.value = warnIdSet

        // TODO - Подумать, как еще можно обеспечить синхронизацию обработки недостаточних данных
        viewModelScope.launch { delay(300) } // Строго после установки WarningSet!!!
    }

    fun completed(): Boolean {
        return _completionWarningSet.value?.count() == 0
    }

}
