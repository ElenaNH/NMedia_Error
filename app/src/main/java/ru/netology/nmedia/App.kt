package ru.netology.nmedia

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

// Чтобы у этого класса была связь с процессом, его нужно зарегистрировать в манифесте
// Здесь необходима аннотация для запуска Dagger Hilt

@HiltAndroidApp
class App : Application()

// Все удалили, т.к. не нужно переопределять методы родителя

