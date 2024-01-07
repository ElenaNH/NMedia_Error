package ru.netology.nmedia.auth

import android.content.Context
import android.util.Log
import androidx.core.content.edit
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import ru.netology.nmedia.api.ApiService
import ru.netology.nmedia.dto.PushToken
import ru.netology.nmedia.dto.Token
import ru.netology.nmedia.dto.User
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppAuth @Inject constructor(
    @ApplicationContext
    private val context: Context
) {
    private val TOKEN_KEY = "TOKEN_KEY"
    private val ID_KEY = "ID_KEY"
    private val LOGIN_KEY = "LOGIN_KEY"

    private val prefs = context.getSharedPreferences("auth", Context.MODE_PRIVATE)
    private val _data = MutableStateFlow<Token?>(null) // Всегда можно узнать текущее значение
    val data = _data.asStateFlow()

    // Костыль
    private var _currentLogin = MutableStateFlow<String?>(null)

    init {
        val token = prefs.getString(TOKEN_KEY, null)
        val id = prefs.getLong(ID_KEY, 0L)
        val login = prefs.getString(LOGIN_KEY, "User $id")    //Костыль

        if (token == null || id == 0L) {
            prefs.edit { clear() }
        } else {
            _data.value = Token(id, token)
            _currentLogin.value = login    //Костыль
        }
        sendPushToken() // поскольку данный синглтон создается при старте приложения
    }

    /*
        @Synchronized
        fun setToken(token: Token) {
            _data.value = token
            prefs.edit {
                putString(TOKEN_KEY, token.token)
                putLong(ID_KEY, token.id)
            } // ? нужен ли после putLong в блоке apply() - в одном примере есть, в другом нет
            sendPushToken()
        }*/

    fun clearAuth() {
        _currentLogin.value = null
        _data.value = null
        prefs.edit { clear() } // ? нужен ли после clear еще и commit() - в одном примере есть, в другом нет
        sendPushToken()
    }

    @InstallIn(SingletonComponent::class)
    @EntryPoint
    interface AppAuthEntryPoint {
        fun getApiService(): ApiService
    }

    fun sendPushToken(token: String? = null) {
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val pushToken = PushToken(token ?: FirebaseMessaging.getInstance().token.await())
                Log.d("pushToken", pushToken.toString())

                val entryPoint =
                    EntryPointAccessors.fromApplication(context, AppAuthEntryPoint::class.java)
                entryPoint.getApiService().sendPushToken(pushToken)
                // TODO  ПОЧЕМУ в лекции ...apiService().save(pushToken)

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }


// КОСТЫЛИ ДЛЯ ХРАНЕНИЯ ТЕКУЩЕГО ПОЛЬЗОВАТЕЛЯ

    @Synchronized
    fun setTokenAndLogin(token: Token, login: String) {
        _currentLogin.value = login
        _data.value = token
        prefs.edit {
            putString(LOGIN_KEY, login)
            putString(TOKEN_KEY, token.token)
            putLong(ID_KEY, token.id)
        } // ? нужен ли после putLong в блоке apply() - в одном примере есть, в другом нет
        sendPushToken()
    }

    fun currentUser(): User {
        val currentUserId = _data.value?.id ?: 0L
        val currentLogin = _currentLogin.value ?: "User $currentUserId"
        return User(
            id = currentUserId,
            login = currentLogin,
            name = currentLogin,  // Можно вычислять из локальной БД (позже)
            avatar = ""
        )
    }


}


