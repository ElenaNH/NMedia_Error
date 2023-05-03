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
    author = currentAuthor(),
    content = "",
    published = "",
    likedByMe = false,
    likes = 0,
    countShare = 0,
    countViews = 0
)

private fun currentAuthor(): String = "Me"  // Надо вычислять текущего автора

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
    private val _postCreateLoading = MutableLiveData<Boolean>()
    val postCreateLoading: LiveData<Boolean>
        get() = _postCreateLoading

    init {
        loadPosts()
    }

    fun loadPosts() {

        // Начинаем загрузку
        _data.value = FeedModel(loading = true) // Аналог _data.setValue(FeedModel(loading = true))
        repository.getAll(object : PostsCallBack<List<Post>> {
            override fun onSuccess(data: List<Post>) {
                _data.postValue(FeedModel(posts = data, empty = data.isEmpty()))
            }

            override fun onError(e: Exception) {
                _data.postValue(FeedModel(error = true))
            }
        })
    }

    fun save() {
        edited.value?.let {
            // Начало загрузки
            _postCreateLoading.value = true // Пока еще мы в главном потоке
            repository.save(it, object : PostsCallBack<Post> {
                // Теперь мы в фоновом потоке в обоих методах
                override fun onSuccess(data: Post) {
                    _postCreated.postValue(Unit) // Передаем сообщение, которое обрабатывается однократно
                    // Если сохранились, то уже нет смысла в черновике (даже если сохранили другой пост)
                    _postCreateLoading.postValue(false) // Конец загрузки
                    postDraftContent("") // Чистим черновик, т.к. успешно вернулся результат и вызван CallBack
                }

                override fun onError(e: Exception) {
                    _data.postValue(FeedModel(error = true))
                }
            })
        }
        // Это снова главный поток (причем, значение edited уже передано в фоновый поток и больше нам не нужно)
        quitEditing()   // Поэтому используем метод главного потока
    }


    fun changeContent(content: String) {
        val text = content.trim()
        if (edited.value?.content == text) {
            return
        }
        edited.value =
            edited.value?.copy(content = text) // Тут мы действуем в главном потоке, поэтому присвоение
    }

    fun likeById(id: Long) {

        // Оптимистичная модель
        val old = _data.value?.posts.orEmpty()
        _data.value =
            _data.value?.copy(posts = _data.value?.posts.orEmpty() // Пока еще главный поток
                .map { post ->
                    if (post.id == id) {
                        post.copy(
                            likedByMe = !post.likedByMe,
                            likes = post.likes + if (post.likedByMe) -1 else 1
                        )
                    } else post
                }
            )

        repository.likeById(id, object : PostsCallBack<Unit> {
            // А тут уже все методы будут в фоновом потоке
            override fun onError(e: Exception) {
                _data.postValue(_data.value?.copy(posts = old))
            }

            override fun onSuccess(data: Unit) {
                // Ничего не делаем, потому что мы уже все сделали до вызова в расчете на успех
            }
        })
        // завершение обработки лайка
    }

    fun shareById(id: Long) {
        // TODO()  //Наш сервер пока не обрабатывает шаринг, поэтому не наращиваем счетчик

    }

    fun removeById(id: Long) {
        // Оптимистичная модель - обновляемся до получения ответа от сервера
        val old = _data.value?.posts.orEmpty()
        _data.value =
            _data.value?.copy(posts = _data.value?.posts.orEmpty() // Пока еще главный поток
                .filter { it.id != id }
            )

        repository.removeById(id, object : PostsCallBack<Unit> {
            // А тут уже все методы будут в фоновом потоке
            override fun onError(e: Exception) {
                _data.postValue(_data.value?.copy(posts = old))
            }

            override fun onSuccess(data: Unit) {
                // Ничего не делаем, потому что мы уже все сделали до вызова в расчете на успех
            }
        })

    }


    fun startEditing(post: Post) {
        edited.value = post
    }

    fun quitEditing() {
        edited.value = emptyPost
    }

    fun setDraftContent(draftContent: String) {
        draft.value = draft.value?.copy(content = draftContent.trim()) // Главный поток
    }

    fun postDraftContent(draftContent: String) {
        draft.postValue(draft.value?.copy(content = draftContent.trim())) // Фоновый поток!!!
    }

    fun getDraftContent(): String {
        return draft.value?.content ?: ""
    }
}
