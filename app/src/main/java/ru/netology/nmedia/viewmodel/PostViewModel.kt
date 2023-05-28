package ru.netology.nmedia.viewmodel

import android.app.Application
import androidx.lifecycle.*
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.enumeration.PostActionType
import ru.netology.nmedia.model.FeedModel
import ru.netology.nmedia.repository.*
import ru.netology.nmedia.util.SingleLiveEvent

//import java.io.IOException
//import kotlin.concurrent.thread


private val emptyPost = Post(
    id = 0,
    author = currentAuthor(),
    authorAvatar = "",
    content = "",
    published = "",
    likedByMe = false,
    likes = 0,
    countShare = 0,
    countViews = 0,
    attachment = null
)

private fun currentAuthor(): String = "Me"  // Надо вычислять текущего автора

class PostViewModel(application: Application) : AndroidViewModel(application) {
    // упрощённый вариант
    private val repository: PostRepository = PostRepositoryImpl()
    private val _data = MutableLiveData(FeedModel())
    val data: LiveData<FeedModel>
        get() = _data
    val edited = MutableLiveData(emptyPost)
    val draft = MutableLiveData(emptyPost)  // И будем сохранять это только "in memory"
    private val _postCreateLoading = MutableLiveData<Boolean>()
    private val _postCreated = SingleLiveEvent<Unit>()
    private val _postActionFailed = SingleLiveEvent<PostActionType>()  // Однократная ошибка
    private val _postActionSucceed = SingleLiveEvent<PostActionType>()  // Однократный успех (альтернатива ошибке)
    val postCreateLoading: LiveData<Boolean>
        get() = _postCreateLoading
    val postCreated: LiveData<Unit>
        get() = _postCreated
    val postActionFailed: LiveData<PostActionType>
        get() = _postActionFailed
    val postActionSucceed: LiveData<PostActionType>
        get() = _postActionSucceed

    init {
        loadPosts()
    }

    fun loadPosts() {

        // Начинаем загрузку
        _data.value = FeedModel(loading = true) // Аналог _data.setValue(FeedModel(loading = true))
        repository.getAllAsync(object : PostRepository.Callback<List<Post>> {
            override fun onSuccess(posts: List<Post>) {
                _data.value = FeedModel(posts = posts, empty = posts.isEmpty())
            }

            override fun onError(e: Exception) {
                _data.value = FeedModel(error = true)
            }
        })
    }

    fun save() {

        edited.value?.let {
            _postCreateLoading.value = true
            repository.save(it, object : PostRepository.Callback<Post> {
                override fun onSuccess(posts: Post) {
                    _postCreated.postValue(Unit) // Передаем сообщение, к-е обрабатывается однократно
                    // Если сохранились, то уже нет смысла в черновике (даже если сохранили другой пост)
                    _postCreateLoading.postValue(false) // Конец загрузки
                    postDraftContent("") // Чистим черновик, т.к. успешно вернулся результат и вызван CallBack
                    _postActionSucceed.postValue(PostActionType.ACTION_POST_CREATION)
                }

                override fun onError(e: Exception) {
                    // Всплывающее сообщение об ошибке записи
                    _postActionFailed.postValue(PostActionType.ACTION_POST_CREATION)

                }
            })
        }

        quitEditing()
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
        // оптимистичная модель
        val old = _data.value?.posts.orEmpty()
        var ratedPost: Post = emptyPost
        _data.value =
            _data.value?.copy(posts = _data.value?.posts.orEmpty() // Пока еще главный поток
                .map { post ->
                    if (post.id == id) {
                        ratedPost = post.copy(
                            likedByMe = !post.likedByMe,
                            likes = post.likes + if (post.likedByMe) -1 else 1
                        )
                        ratedPost   // Одновременно запомним и изменим обновленный пост в списке
                    } else post
                }
            )

        // Если даже пост не найден, и id остался нулевой (что маловероятно),
        // то все равно передадим его на сервер - пусть сервер вернет ошибку
        repository.likeById(id, object : PostRepository.Callback<Post> {
            override fun onSuccess(posts: Post) {
                _postActionSucceed.postValue(PostActionType.ACTION_POST_LIKE_CHANGE)
                // Ничего не делаем, потому что мы уже все сделали до вызова в расчете на успех
            }

            override fun onError(e: Exception) {
                _postActionFailed.postValue(PostActionType.ACTION_POST_LIKE_CHANGE)

                // Раз не ставится лайк, то вернемся к предыдущим данным
                _data.postValue(_data.value?.copy(posts = old))
            }
        })


        // завершение обработки лайка
    }

    fun shareById(id: Long) {
        // TODO()  //Наш сервер пока не обрабатывает шаринг, поэтому не наращиваем счетчик

    }

    fun removeById(id: Long) {
        // TODO
        // Оптимистичная модель - обновляемся до получения ответа от сервера
        val old = _data.value?.posts.orEmpty()
        _data.value =
            _data.value?.copy(posts = _data.value?.posts.orEmpty() // Пока еще главный поток
                .filter { it.id != id }
            )
        repository.removeById(id, object : PostRepository.Callback<Unit> {
            override fun onSuccess(posts: Unit) {
                super.onSuccess(posts)
                _postActionSucceed.postValue(PostActionType.ACTION_POST_DELETION)
                // Ничего не делаем, потому что мы уже все сделали до вызова в расчете на успех
            }

            override fun onError(e: Exception) {
                super.onError(e)
                _postActionFailed.postValue(PostActionType.ACTION_POST_DELETION)
                _data.postValue(_data.value?.copy(posts = old))
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
