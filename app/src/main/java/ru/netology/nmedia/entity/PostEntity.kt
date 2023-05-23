package ru.netology.nmedia.entity

import androidx.room.*
import ru.netology.nmedia.dto.Attachment
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.enumeration.AttachmentType

@Entity
data class PostEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long,
    val author: String,
    val authorAvatar: String,
    val content: String,
    val published: String,
    val likedByMe: Boolean,
    val likes: Int = 0,
    @Embedded
    var attachment: AttachmentEmbeddable?,
//    val attachment: Attachment? = null,
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

        return Post(
            id,
            author,
            authorAvatar,
            content,
            match?.value,
            published,
            likedByMe,
            likes,
            0,
            0,
            attachment?.toDto(),
        )
    }


    companion object {
        fun fromDto(dto: Post) =
            PostEntity(
                dto.id,
                dto.author,
                dto.authorAvatar,
                dto.content,
                dto.published,
                dto.likedByMe,
                dto.likes,
                AttachmentEmbeddable.fromDto(dto.attachment),
            )

    }
}

//@Embeddable
data class AttachmentEmbeddable(
    var url: String,
    //@Column(columnDefinition = "TEXT")
    var description: String?,
    //@Enumerated(EnumType.STRING)
    var type: String,
) {
    fun toDto() = Attachment(url, description, AttachmentType.valueOf(type))

    companion object {
        fun fromDto(dto: Attachment?) = dto?.let {
            AttachmentEmbeddable(it.url, it.description, it.type.toString())
        }
    }
}


