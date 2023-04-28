package ru.netology.nmedia.viewmodel

import android.app.Application
import androidx.lifecycle.*
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.repository.*
import ru.netology.nmedia.model.FeedModel
import ru.netology.nmedia.util.SingleLiveEvent
import java.io.IOException
import kotlin.concurrent.thread


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
    // упрощённый вариант
    private val repository: PostRepository = PostRepositoryImpl()
    private val _data = MutableLiveData(FeedModel())
    val data: LiveData<FeedModel>
        get() = _data
    val edited = MutableLiveData(emptyPost)
    val draft = MutableLiveData(emptyPost)  // И будем сохранять это только "in memory"
    private val _postCreated = SingleLiveEvent<Unit>()
    val postCreated: LiveData<Unit>
        get() = _postCreated

    val _postAdded = SingleLiveEvent<Unit>()    // Добавлен новый пост
    val postAdded: LiveData<Unit>
        get() = _postAdded
    init {
        loadPosts()
    }

    fun loadPosts() {
        thread {
            // Начинаем загрузку
            _data.postValue(FeedModel(loading = true))
            try {
                // Данные успешно получены
                val posts = repository.getAll()
                FeedModel(posts = posts, empty = posts.isEmpty())
            } catch (e: IOException) {
                // Получена ошибка
                FeedModel(error = true)
            }.also(_data::postValue)    // Аналог _data.postValue(Рассчитанная_в_блоке_FeedModel)
        }
    }

    fun save() {
        edited.value?.let {
            val newPostCreated = (edited.value?.id == 0L)
            thread {
                repository.save(it)
                _postCreated.postValue(Unit) // Смысл выводить сообщение 1 раз, если сохранение возможно повторное?
                if (newPostCreated) {
                    _data.postValue(data.value?.copy(newPostWaiting = true)) //ждем акцепта нового поста на сервере
                    _postAdded.postValue(Unit)
                }
                // Если сохранились, то уже нет смысла в черновике (даже если сохранили другой пост)
                postDraftContent("") // Вообще-то, лучше чистить черновик при положительном результате, а не тут
            }
        }
        quitEditing()
    }


    /*  // Замена функции edit на startEditing
        fun edit(post: Post) {
            edited.value = post
        }*/

    fun changeContent(content: String) {
        val text = content.trim()
        if (edited.value?.content == text) {
            return
        }
        edited.value =
            edited.value?.copy(content = text) // Тут мы действуем в главном потоке, поэтому присвоение
    }

    fun likeById(id: Long) {
        thread {
            //repository.likeById(id)

            // Оптимистичная модель
            val old = _data.value?.posts.orEmpty()
            _data.postValue(
                _data.value?.copy(posts = _data.value?.posts.orEmpty()
                    .map { post ->
                        if (post.id == id) post.copy(
                            likedByMe = !post.likedByMe,
                            countLikes = post.countLikes + if (post.likedByMe) -1 else 1
                        )
                        else post
                    }
                )
            )
            try {
                repository.likeById(id)
            } catch (e: IOException) {
                _data.postValue(_data.value?.copy(posts = old))
            }
            // завершение обработки лайка
        }
    }

    fun shareById(id: Long) {
        // TODO()

    }

    fun removeById(id: Long) {
        thread {
            // Оптимистичная модель
            val old = _data.value?.posts.orEmpty()
            _data.postValue(
                _data.value?.copy(posts = _data.value?.posts.orEmpty()
                    .filter { it.id != id }
                )
            )
            try {
                repository.removeById(id)
            } catch (e: IOException) {
                _data.postValue(_data.value?.copy(posts = old))
            }
        }
    }


    fun startEditing(post: Post) {
        edited.value = post
    }

    fun quitEditing() {
        edited.value = emptyPost
    }

    fun setDraftContent(draftContent: String) {
        draft.value = draft.value?.copy(content = draftContent.trim())
//        draft.postValue(draft.value?.copy(content = draftContent.trim()))
    }

    fun postDraftContent(draftContent: String) {
//        draft.value = draft.value?.copy(content = draftContent.trim())
        draft.postValue(draft.value?.copy(content = draftContent.trim()))
    }

    fun getDraftContent(): String {
        return draft.value?.content ?: ""
    }
}
