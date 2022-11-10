package ru.netology.nmedia.dto

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

data class Post(
    val id: Long,
    val author: String,
    val content: String,
    val published: String,
    val likedByMe: Boolean = false,
    val countLikes: Long,
    val countShare: Long,
    val countViews: Long
)
