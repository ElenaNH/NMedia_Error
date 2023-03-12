package ru.netology.nmedia.viewmodel

import android.app.Application
import androidx.lifecycle.*
import ru.netology.nmedia.db.AppDb
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.repository.*

private val emptyPost = Post(
    id = 0,
    author = "",
    content = "",
    published = "",
    likedByMe = false,
    countLikes = 0,
    countShare = 0,
    countViews = 0
)

//class PostViewModel : ViewModel()
class PostViewModel(application: Application) : AndroidViewModel(application) {
    // упрощённый вариант // Пока сохраним упрощенный код, хоть так обычно и не делается
//    private val repository: PostRepository = PostRepositoryInMemoryImpl()
//    private val repository: PostRepository = PostRepositorySharedPrefsImpl(application)
//    private val repository: PostRepository = PostRepositoryFileImpl(application)
//    private val repository: PostRepository = PostRepositorySQLiteImpl(AppDb.getInstance(application).postDao)
    private val repository: PostRepository = PostRepositoryImpl(
        AppDb.getInstance(context = application).postDao()
    )

    val data = repository.getAll()
    val edited = MutableLiveData(emptyPost)
    val draft =
        MutableLiveData(emptyPost)  // И не будем сохранять это в файле, ни в БД - только "in memory"

    fun save() {
        edited.value?.let {
            repository.save(it)
            // Если сохранились, то уже нет смысла в черновике (даже если сохранили другой пост)
            setDraftContent("")
        }
        quitEditing()
    }

    fun startEditing(post: Post) {
        edited.value = post
    }

    fun quitEditing() {
        edited.value = emptyPost
    }

    fun changeContent(content: String) {
        val text = content.trim()
        if (edited.value?.content == text) {
            return
        }
        // Сначала проверим наличие ссылки внутри поста (возьмем первую подходящую)
        val regex = "(https?://)?([\\w-]{1,32})(\\.[\\w-]{1,32})+[^\\s@]*".toRegex()
        val match = regex.find(content)
        // Если ссылка есть в тексте, то поместим ее в отдельное поле
        // Если нет ссылки, то поле ссылки должно стать пустым (даже если раньше там ссылка была)
        // Теперь скопируем пост с нашими изменениями
        edited.value = edited.value?.copy(content = text, videoLink = (match?.value ?: ""))
    }

    fun setDraftContent(draftContent: String) {
        draft.value = draft.value?.copy(content = draftContent.trim())
    }

    fun getDraftContent(): String {
        return draft.value?.content ?: ""
    }

    fun likeById(id: Long) = repository.likeById(id)
    fun shareById(id: Long) = repository.shareById(id)
    fun removeById(id: Long) = repository.removeById(id)

}
