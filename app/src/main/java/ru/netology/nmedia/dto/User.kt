package ru.netology.nmedia.dto

data class User(
    val id: Long,
    val login: String,
    val name: String,  // в посте это author
    val avatar: String, // в посте это authorAvatar
//    val authorities: List<String>,
)

val AnonymousUser = User(
    id = 0L,
    login = "anonymous",
    name = "Anonymous",
    avatar = "",
//    authorities = listOf("ROLE_ANONYMOUS")
)
