package ru.netology.nmedia.viewmodel

import android.app.Application
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.findFragment
import androidx.lifecycle.*
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.switchMap
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import ru.netology.nmedia.R
import ru.netology.nmedia.db.AppDb
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.enumeration.PostActionType
import ru.netology.nmedia.model.FeedModel
import ru.netology.nmedia.model.FeedModelState
import ru.netology.nmedia.repository.*
import ru.netology.nmedia.util.ConsolePrinter
import ru.netology.nmedia.util.SingleLiveEvent

//import java.io.IOException
//import kotlin.concurrent.thread


private val emptyPost = Post.getEmptyPost()

class PostViewModel(application: Application) : AndroidViewModel(application) {
    // упрощённый вариант
    private val repository: PostRepository =
        PostRepositoryImpl(AppDb.getInstance(application).postDao())

    // в репозитории - все неудаленное; в данных - все нескрытое

    // val data: LiveData<FeedModel> =
    //        repository.data
    //            .map {
    //                val visiblePosts = it.filter { it.hidden == 0 }
    //                // В конце возвращаем новые данные для списка постов
    //                FeedModel(posts = visiblePosts, empty = visiblePosts.isEmpty())
    //            }
    //            .asLiveData(Dispatchers.Default)
    val repositoryData: LiveData<FeedModel> =
        repository.data
            .map {
                FeedModel(posts = it, empty = it.isEmpty())
            }
            .asLiveData(Dispatchers.Default)
    val data: LiveData<FeedModel> = repositoryData
        .map {
            val visiblePosts = it.posts.filter { it.hidden == 0 }
            // В конце возвращаем новые данные для списка постов
            FeedModel(posts = visiblePosts, empty = visiblePosts.isEmpty())
        }

    val edited = MutableLiveData(emptyPost)
    val draft = MutableLiveData(emptyPost)  // И будем сохранять это только "in memory"

    val newerCount: LiveData<Int> = data.switchMap {
        repository.getNewerCount(it.posts.firstOrNull()?.id ?: 0L)
            .asLiveData(Dispatchers.Default)
    }

    private val _dataState = MutableLiveData(FeedModelState())
    val dataState: LiveData<FeedModelState>
        get() = _dataState
    private val _postCreated = SingleLiveEvent<Unit>()
    val postCreated: LiveData<Unit>
        get() = _postCreated
    private val _postSavingStarted = SingleLiveEvent<Unit>()
    val postSavingStarted: LiveData<Unit>
        get() = _postSavingStarted

    private val _postActionFailed = SingleLiveEvent<PostActionType>()  // Однократная ошибка
    val postActionFailed: LiveData<PostActionType>
        get() = _postActionFailed


// 1) раньше выводили однократную ошибку, но теперь нас устраивает многократный вывод ошибки
// с кнопкой refresh для всех постов - либо однократный для создания/редактирования поста
// Сообщение об успехе не выводим - просто обновляемся
// 2) посты при ошибке не перезапрашиваем - только по кнопке или по свайпу или при открытии
// (избегаем бесконечного цикла при отказе сервера)
// 3) репозиторий
// при ошибке лайка самовосстанавливается
// при иной ошибке устанавливает различные состояния ожидания, которые синхронизируются при обновлении
// 4) viewModel подписана на данные репозитория, поэтому сама перерисуется при его восстановлении


// С какой периодичностью перезапрашивать посты?
// А то все обновится на сервере, а мы не увидим
// Вариант - при каждом успешном действии (т.е., когда точно был контакт с сервером)
// Решение: Пока сделаем только при загрузке и принудительном обновлении


    init {
        loadPosts()
    }

    fun loadPosts() = refreshOrLoadPosts(refreshingState = false)

    fun refresh() = refreshOrLoadPosts(refreshingState = true)

    private fun refreshOrLoadPosts(refreshingState: Boolean) = viewModelScope.launch {
        if (refreshingState) {
            _dataState.value = FeedModelState(refreshing = true)  // Начинаем обновление
        } else {
            _dataState.value = FeedModelState(loading = true)  // Начинаем загрузку
        }
        try {
            repository.getAll()
            _dataState.value = FeedModelState()     // При успехе
        } catch (e: Exception) {
            _dataState.value = FeedModelState(error = true)  // При ошибке
        }
    }

    fun save() {
        // Тут просто вызываем метод репозитория
        // А уже в репозитории делаем так:
        // Если пост новый - создаем запись в локальной БД
        // Отправляем для неподтвержденного поста запрос на сервер с заменой id = 0
        // а если пост подтвержденный, то с его собственным id
        // Если от сервера приходит ошибка, то статусы поста не меняются
        // Если приходит успешный ответ с постом и с присвоенным id, то
        // "черновому" посту меняем id и сбрасываем признак unconfirmed
        // После этого уже пост точно подтвержден, так что обновляем его целиком

        viewModelScope.launch {
            supervisorScope {
                async {
                    // Эта корутина будет отвечать за запись - ее не ждем

                    try {
                        edited.value?.let {
                            repository.save(it)
                            _postCreated.value = Unit  // Однократное событие

                            ConsolePrinter.printText("MY SAVING TRY FINISHED")
                        }
                    } catch (e: Exception) {
                        ConsolePrinter.printText("MY SAVING CATCH STARTED: ${e.message.toString()}")
                        // Тут надо просто оставить запись в локальной БД в неподтвержденном статусе
                        _postActionFailed.value = PostActionType.ACTION_POST_SAVING
                    }
                }

                launch {
                    // Эта корутина будет отвечать за выход из режима редактирования

                    _postSavingStarted.value = Unit // Однократное событие
                    quitEditing() // сбрасываем редактирование при попытке записи (заменим на emptyPost)
                    // Черновик сбросим, т.к. у нас будет либо подтвержденный, либо неподтвержденный пост
                    postDraftContent("")

                }
            }
        }

    }


    fun changeContent(content: String) {
        val text = content.trim()
        if (edited.value?.content == text) {
            return
        }
        edited.value =
            edited.value?.copy(content = text)
    }

    fun likeById(unconfirmedStatus: Int, id: Long, setLikedOn: Boolean) {
        viewModelScope.launch {
            try {
                repository.likeById(unconfirmedStatus, id, setLikedOn)
                _dataState.value = FeedModelState()     // При успехе
                ConsolePrinter.printText("MY LIKING TRY FINISHED")
            } catch (e: Exception) {
                ConsolePrinter.printText("MY LIKING CATCH STARTED: ${e.message.toString()}")
                _postActionFailed.value =
                    PostActionType.ACTION_POST_LIKE_CHANGE // Признак ошибки
            }
        }
        // завершение обработки лайка
    }

    fun shareById(unconfirmedStatus: Int, id: Long) {
        // TODO()  //Наш сервер пока не обрабатывает шаринг, поэтому не наращиваем счетчик

    }

    fun removeById(unconfirmedStatus: Int, id: Long) {
        viewModelScope.launch {
            try {
                repository.removeById(unconfirmedStatus, id)
                _dataState.value = FeedModelState()     // При успехе
                ConsolePrinter.printText("MY TRY FINISHED")
            } catch (e: Exception) {
                ConsolePrinter.printText("MY CATCH STARTED: ${e.message.toString()}")
                _postActionFailed.value =
                    PostActionType.ACTION_POST_DELETION // Установим признак ошибки
            }
        }
    }

    fun countHidden(): Int {
//        var count = 0
//        viewModelScope.launch {
//            count = repository.countHidden()
//        }
//        return count

        return repositoryData.value?.posts?.filter { it.hidden != 0 }?.count() ?: 0
    }

    fun setAllVisible() {
        viewModelScope.launch {
            repository.setAllVisible()
        }
    }


    //-------------------------------------------------
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
