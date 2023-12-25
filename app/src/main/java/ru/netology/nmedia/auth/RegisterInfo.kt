package ru.netology.nmedia.auth

data class RegisterInfo(
    val username: String = "",
    val login: String = "",
    val password: String = "",
    val password2: String = "",
    val server: String? = null,
    val port: String? = null,
    val avatar: String? = null,
)
