package ru.netology.nmedia.dto

data class Token(
    val id: Long,   // в посте это поле authorId
    val token: String,
    //val avatar: String? = null,  // Пока что без него
)
