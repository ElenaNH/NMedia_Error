package ru.netology.nmedia.util

import ru.netology.nmedia.BuildConfig
import ru.netology.nmedia.dto.User

val BASE_URL = BuildConfig.BASE_URL  //"http://10.0.2.2:9999"
//val BASE_URL = "http://192.168.0.107:9999" // - ТАК НЕ ХОЧЕТ РАБОТАТЬ


fun currentUser(): User {  // Надо вычислять текущего пользователя
    return User(
        id = 5L,
        login = "student",
        name = "Студент",
        avatar = ""
    )
}


