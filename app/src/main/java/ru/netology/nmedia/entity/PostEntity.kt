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
    val published: String,
    val likedByMe: Boolean,
    val likes: Int = 0,
) {
    // Пока мы работаем только с теми полями, которые были описаны в задании для серверной части
    // далее наверняка добавятся все нужные поля

    //    fun toDto() = Post(id, author, content, null, published, likedByMe, likes, 0, 0)
    fun toDto(): Post {
        // Сначала проверим наличие ссылки внутри поста (возьмем первую подходящую)
        val regex = "(https?://)?([\\w-]{1,32})(\\.[\\w-]{1,32})+[^\\s@]*".toRegex()
        val match = regex.find(content)
        // Если ссылка есть в тексте, то поместим ее в отдельное поле
        // Если нет ссылки, то поле ссылки будет пустым

        return Post(id, author, content, match?.value, published, likedByMe, likes, 0, 0)
    }


    companion object {
        fun fromDto(dto: Post) =
            PostEntity(
                dto.id,
                dto.author,
                dto.content,
                dto.published,
                dto.likedByMe,
                dto.likes
            )

    }
}

/*@Entity
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
}*/

