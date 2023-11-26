package ru.netology.nmedia

import android.app.Application
import ru.netology.nmedia.auth.AppAuth

class App : Application() {
// Чтобы у этого класса была связь с процессом, его нужно зарегистрировать в манифесте

    override fun onCreate() {
        super.onCreate()
        AppAuth.initApp(this)
    }
}
