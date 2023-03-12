package ru.netology.nmedia.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import ru.netology.nmedia.dto.Post

@Entity
data class PostEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long,
    val author: String,
    val content: String,
    val videoLink: String? = null,
    val published: String,
    val likedByMe: Boolean,
    val countLikes: Int = 0,
    val countShare: Int = 0,
    val countViews: Int = 0
) {
    fun toDto() = Post(
        id,
        author,
        content,
        videoLink,
        published,
        likedByMe,
        countLikes,
        countShare,
        countViews
    )

    companion object {
        fun fromDto(dto: Post) =
            PostEntity(
                dto.id,
                dto.author,
                dto.content,
                dto?.videoLink ?: "",
                dto.published,
                dto.likedByMe,
                dto.countLikes,
                dto.countShare,
                dto.countViews
            )
    }
}

