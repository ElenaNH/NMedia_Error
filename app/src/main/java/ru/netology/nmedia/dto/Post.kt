package ru.netology.nmedia.dto

import ru.netology.nmedia.enumeration.AttachmentType

fun Long.statisticsToString(): String {
    val stat = this
    return when {
        (stat >= 1_000_000L) -> {
            val mega = stat / 1_000_000L
            val megaDecimal = (stat % 1_000_000L) / 100_000L
            if (megaDecimal == 0L) "${mega}M" else "${mega}.${megaDecimal}M"
        }
        (stat >= 10_000L) -> "${stat / 1_000L}K"
        (stat >= 1_000L) -> {
            val kilo = stat / 1_000L
            val kiloDecimal = (stat % 1_000L) / 100L
            if (kiloDecimal == 0L) "${kilo}K" else "${kilo}.${kiloDecimal}K"
        }
        (stat < 0L) -> "XX"
        else -> "$stat"
    }
}

/*data class Attachment(
    val url: String,
    val description: String,
    val type: String    // изначально только IMAGE
)*/

data class Attachment(
    val url: String,
    val description: String?,
    val type: AttachmentType,
)

/*ВНИМАНИЕ! поле likes должно называться именно так,
* чтобы работало преобразование gson.fromJson в объект Post из текста, пришедшего с сервера
* НО!!!
* Правильно было использовать PostEntity, который конвертировать далее через toDto*/
data class Post(
    val id: Long,
    val author: String,
    val authorAvatar: String,
    val content: String,
    val videoLink: String? = null,
    val published: String,  // val date: Int = (System.currentTimeMillis() / 86400000).toInt()
    val likedByMe: Boolean = false,
    val likes: Int,
    val countShare: Int,
    val countViews: Int,
    val attachment: Attachment? = null
) {
    fun avatarFileName() = when (author) {
        "Нетология", "Netology" -> "netology.jpg"
        "Сбер" -> "sber.jpg"
        "Тинькофф", "Tinkoff" -> "tcs.jpg"
        else -> ""
    }
}
