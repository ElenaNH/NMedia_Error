package ru.netology.nmedia.entity

import androidx.room.*
import retrofit2.http.POST
import ru.netology.nmedia.dto.Attachment
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.enumeration.AttachmentType
//@PrimaryKey(autoGenerate = true)  // Тут нужен двупольный ключ во избежание конфликта идентификаторов
@Entity(primaryKeys = ["unconfirmed","id"])
data class PostEntity(
    val id: Long,
    val authorId: Long,
    val author: String,
    val authorAvatar: String,
    val content: String,
    val published: String,
    val likedByMe: Boolean,
    val likes: Int = 0,
    @Embedded
    var attachment: AttachmentEmbeddable?,
//    val attachment: Attachment? = null,
    val unconfirmed: Int,   // ОТСУТСТВУЕТ В СЕРВЕРНОЙ БД
    val unsaved: Int,       // ОТСУТСТВУЕТ В СЕРВЕРНОЙ БД
    val deleted: Int = 0,   // ОТСУТСТВУЕТ В СЕРВЕРНОЙ БД
    val hidden: Int,        // ОТСУТСТВУЕТ В СЕРВЕРНОЙ БД
    val unsavedAttach: Int = 0,       // ОТСУТСТВУЕТ В СЕРВЕРНОЙ БД
) {
    // С одной стороны мы можем добавлять свои поля для своих целей
    // С другой стороны - серверное поле content мы уже используем для записи данных,
    // которые только готовимся отправить
    // Так что не страшно, если мы и поля из Embedded attachment так же задействуем

    fun toDto(): Post {
        // Сначала проверим наличие ссылки внутри поста (возьмем первую подходящую)
        val regex = "(https?://)?([\\w-]{1,32})(\\.[\\w-]{1,32})+[^\\s@]*".toRegex()
        val match = regex.find(content)
        // Если ссылка есть в тексте, то поместим ее в отдельное поле
        // Если нет ссылки, то поле ссылки будет пустым

        return if (deleted == 0)
            Post(
                id = id,
                authorId = authorId,
                author = author,
                authorAvatar = authorAvatar,
                content = content,
                videoLink = match?.value,
                published = published,
                likedByMe = likedByMe,
                likes = likes,
                countShare = 0,
                countViews = 0,
                attachment = attachment?.toDto(),
                unconfirmed = unconfirmed,
                unsaved = unsaved,
                hidden = hidden,
                unsavedAttach = unsavedAttach,
            )
        else
            // Подумать - не лучше ли выбрасывать ошибку
            Post.getEmptyPost()     // Если пост удален, то вернем пустой неподтвержденный пост неопределенного автора
    }


    companion object {
        fun fromDto(dto: Post) =
            PostEntity(
                id = dto.id,
                authorId = dto.authorId,
                author = dto.author,
                authorAvatar = dto.authorAvatar,
                content = dto.content,
                published = dto.published,
                likedByMe = dto.likedByMe,
                likes = dto.likes,
                attachment = AttachmentEmbeddable.fromDto(dto.attachment),
                unconfirmed = dto.unconfirmed,
                unsaved = dto.unsaved,
                deleted = 0,
                hidden = dto.hidden,
                unsavedAttach = dto.unsavedAttach,
            ) // Мы все-таки будем следить, чтобы удаленные энтити не превращались в посты

    }
}

data class AttachmentEmbeddable(
    var url: String,
    var description: String?,
    var type: String,
) {
    fun toDto() = Attachment(url, description, AttachmentType.valueOf(type))

    companion object {
        fun fromDto(dto: Attachment?) = dto?.let {
            AttachmentEmbeddable(it.url, it.description, it.type.toString())
        }
    }
}


// Функции расширения для списков

fun List<PostEntity>.toDto(): List<Post> = map(PostEntity::toDto)
    //.filter { it.id != 0L } - пока у нас и так будут ненулевые id, ведь в базе есть автонумератор
fun List<Post>.fromDto(): List<PostEntity> = map(PostEntity::fromDto)
    .filter { it.deleted == 0 } // Берем только неудаленные записи таблицы "энтити"
