package ru.netology.nmedia.repository

import kotlinx.coroutines.flow.*
//import kotlinx.coroutines.flow.Flow
//import kotlinx.coroutines.flow.flow
//import kotlinx.coroutines.flow.flowOn
//import kotlinx.coroutines.flow.map
import kotlinx.coroutines.delay
import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.newFixedThreadPoolContext
import kotlinx.coroutines.supervisorScope
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import ru.netology.nmedia.api.PostsApi
import ru.netology.nmedia.dao.PostDao
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.entity.PostEntity
import ru.netology.nmedia.entity.fromDto
import ru.netology.nmedia.entity.toDto
import ru.netology.nmedia.util.ConsolePrinter
//import java.io.IOException
//import java.util.concurrent.TimeUnit
//import kotlin.Exception
import java.lang.RuntimeException
import ru.netology.nmedia.error.ApiError
import ru.netology.nmedia.error.AppError


class PostRepositoryImpl(private val postDao: PostDao) : PostRepository {
    override val data: Flow<List<Post>> = postDao.getAll()
        .map { it.toDto() }
        .flowOn(Dispatchers.Default)


    override suspend fun getAll() {

        // ******************************************************************************
        // К сожалению, в интерфейсе PostRepository не задан никакой аналог pushAllPostponed
        // поэтому нельзя отдельно запараллелить отправку отложенных действий из вьюмодели
        // Приходится сюда добавлять кучу действий - больше деть их некуда
        // ******************************************************************************

        // Запросим список постов с сервера
        val response = PostsApi.retrofitService.getAll()
        if (!response.isSuccessful) {
            throw RuntimeException(response.message())
        }
        val posts = response.body() ?: throw RuntimeException("body is null")
        // Если данные пришли хорошие, то

        // Исключим из обновления несохраненные изменения и запланированные удаления
        val confirmedUnsavedEntities = postDao.getAllConfirmedUnsaved()
        val waitedDeleteEntities = postDao.getAllDeleted()
        val excludeIds = confirmedUnsavedEntities.map { it.id }
            .plus(waitedDeleteEntities.map { it.id })
            .toSet()
        // .toList()

        val postEntitiesForInserting = posts.fromDto()
            .filterNot { excludeIds.contains(it.id) }
//            .map { entity ->
//                entity.copy(hidden = if (postDao.isEmpty()) 0 else 1)
//            } // Если в базе не было постов, то все покажем, а иначе новые сделаем скрытыми

        // Обновим только то, что не нужно прежде проталкивать на сервер
        if (postEntitiesForInserting.count() > 0)
            postDao.insert(postEntitiesForInserting)
        //postDao.insert(posts.map { PostEntity.fromDto(it)})

        // Сюда дошли, значит можно вкинуть на сервер кучу подвисших новых/удаляемых постов
        pushLocalDeleted()
        pushLocalUnconfirmed()  // При неуспехе мы вываливаемся отсюда в вызывающую функцию
        pushLocalConfirmedUnsaved()  // Пока отдельными функциями протолкнем
    }

    suspend fun pushLocalUnconfirmed() {
        // Не обрабатываем ошибку, а вылетаем наверх
        ConsolePrinter.printText("pushAllUnconfirmed started")
        postDao.getAllUnconfirmed()
            .toDto()
            .forEach {
                save(it)
            }
        ConsolePrinter.printText("pushAllUnconfirmed finished")
    }

    suspend fun pushLocalConfirmedUnsaved() {
        // Не обрабатываем ошибку, а вылетаем наверх
        ConsolePrinter.printText("pushAllConfirmedUnsaved started")
        postDao.getAllConfirmedUnsaved()
            .toDto()
            .forEach {
                save(it)
            }
        ConsolePrinter.printText("pushAllConfirmedUnsaved finished")
    }

    suspend fun pushLocalDeleted() {
        // Не обрабатываем ошибку, а вылетаем наверх
        ConsolePrinter.printText("pushAllDeleted started")
        postDao.getAllDeleted()
            .forEach { entity ->
                removeById(
                    entity.unconfirmed,
                    entity.id
                ) // Удаленный энтити преобразовался бы в пустой пост, а нам нужен непустой
            }
        ConsolePrinter.printText("pushAllDeleted finished")
    }


    override suspend fun save(post: Post) {
        // Сначала добавляем в локальную БД в "неподтвержденном" статусе
        val newPost = (post.id == 0L)
        val unconfirmedPost = newPost || (post.unconfirmed != 0)
        if (newPost) ConsolePrinter.printText("New post before inserting...")
        val postIdLoc = if (newPost) {
            postDao.insertReturningId(PostEntity.fromDto(post)) // Генерируем id: это расплата за другие удобства
        } else {
            postDao.save(PostEntity.fromDto(post))
            post.id
        }

        if (newPost) ConsolePrinter.printText("Added new post with id = $postIdLoc")
        else ConsolePrinter.printText("Updated content of post with id = $postIdLoc")

        // Будем выбрасывать исключение во вьюмодель только после некоторой обработки
        var response: Response<Post>? = null
        try {
            // Затем отправляем запрос удаления на сервер
            response = PostsApi.retrofitService.save(
                if (unconfirmedPost) post.copy(
                    id = 0,
                    unconfirmed = 0
                ) else post
            )
            ConsolePrinter.printText("HAVE GOT SAVE RESPONSE")
        } catch (e: Exception) {
            ConsolePrinter.printText("HAVE NOT GOT SAVE RESPONSE")
            // Просто выбрасываем ошибку, а пост висит в очереди на запись
            throw RuntimeException(e.message.toString())
        }
        if (!(response?.isSuccessful ?: false)) {
            throw RuntimeException(response?.message() ?: "No server response")
        }
        val responsePost = response?.body() ?: throw RuntimeException("body is null")

        // Если вернулся ожидаемый Post,а не null, то

        // Если это записался неподтвержденный пост, то отметим его подтвержденным серверным id при unconfirmed = 0
        if (unconfirmedPost && (responsePost.id != 0L)) {
            // Конфликт ключей даже при асинхронности нам не грозит, ведь ключ двупольный
            // Синхронизируем ключ поста с данными сервера
            postDao.confirmWithPersistentId(postIdLoc, responsePost.id)
            ConsolePrinter.printText("POST CONFIRMED")
        }
        // Обновляем пришедший пост
        // К этому моменту первичный ключ железно синхронизирован с сервером, можем обновлять целиком запись
        postDao.insert(PostEntity.fromDto(responsePost).copy(unconfirmed = 0))


        // ПОРЯДОК:
        // Мы однократно ожидаем ответ сервера.
        // Если ответ есть, то в лок.БД отмечаем подтверждение и правильный id, затем обновляем все поля
        // Если ответ не пришел - тогда пост остается unconfirmed
        // При любом следующем запросе loadPosts:
        // отправляем серверу все unconfirmed в порядке возрастания id
        // затем отправляем все измененные и все удаляемые
        // это можно делать даже асинхронно, но мы пока оставим так

    }

    override suspend fun removeById(unconfirmedStatus: Int, id: Long) {
        // Сначала удаляем в локальной БД (оптимистичная модель)
        ConsolePrinter.printText("removeById($unconfirmedStatus, $id)")
        postDao.removeById(unconfirmedStatus, id)
        if (unconfirmedStatus != 0) {
            postDao.clearById(unconfirmedStatus, id)
            return  // Если пост не был подтвержден, то на сервер не отправляем
        }
        // Если запрос к серверу вызовет исключение, то аккуратно переправим его выше
        try {
            // отправляем запрос удаления на сервер
            val response = PostsApi.retrofitService.removeById(id)
            ConsolePrinter.printText("HAVE GOT DELETE RESPONSE")
            if (!response.isSuccessful) {
                throw RuntimeException("No server response")
            }
            val responseUnit = response?.body() ?: throw RuntimeException("body is null")
        } catch (e: Exception) {
            ConsolePrinter.printText("HAVE NOT GOT DELETE RESPONSE")
            throw RuntimeException(e.message.toString())
        }
        // Если мы тут, то сервер вернул ожидаемый Unit,а не null, тогда:
        // Мы уже ранее пометили пост к удалению в локальной БД
        // Остается почистить запись в локальной БД, чтобы не скапливать мусор
        postDao.clearById(unconfirmedStatus, id)
        ConsolePrinter.printText("POST CLEARED")
    }

    override suspend fun likeById(unconfirmedStatus: Int, id: Long, setLikedOn: Boolean) {
        // Лайкаем только подтвержденные посты и только если сервер доступен

        // Сначала обрабатываем лайк в локальной БД (оптимистичная модель)
        if (setLikedOn) postDao.likeById(unconfirmedStatus, id)
        else postDao.dislikeById(unconfirmedStatus, id)

        // Неподтвержденные - сразу возвращаем в нелайкнутое состояние
        if (unconfirmedStatus != 0) {
            // Поскольку нам надо добиться перерисовки сердечка, то чуть подождем
            delay(500)
            restoreLikesByIdAndThrow(
                unconfirmedStatus,
                id,
                setLikedOn,
                "Cannot like unconfirmed post"
            )
        }

        // Команду для подтвержденных постов направляем серверу
        // Будем выбрасывать исключение во вьюмодель только после некоторой обработки
        var response: Response<Post>? = null
        try {
            // Затем отправляем запрос лайка/дизлайка на сервер
            response =
                if (setLikedOn) PostsApi.retrofitService.likeById(id)
                else PostsApi.retrofitService.dislikeById(id)
            ConsolePrinter.printText("HAVE GOT LIKE RESPONSE")
        } catch (e: Exception) {
            ConsolePrinter.printText("HAVE NOT GOT LIKE RESPONSE")
            // Придется восстановить лайки в то состояние, что было до изменения в локальной БД
            restoreLikesByIdAndThrow(unconfirmedStatus, id, setLikedOn, e.message.toString())
        }
        if (!(response?.isSuccessful ?: false)) {
            delay(500)  // Пусть сердечко успеет восстановиться
            restoreLikesByIdAndThrow(
                unconfirmedStatus,
                id,
                setLikedOn,
                response?.message() ?: "No server response for like"
            )
        }
        response?.body() ?: restoreLikesByIdAndThrow(
            unconfirmedStatus,
            id,
            setLikedOn,
            "body is null"
        )

        // Если вернулся ожидаемый Post,а не null, то
        // ничего уже не делаем, ведь все сделали до отправки запроса на сервер
    }

    private suspend fun restoreLikesByIdAndThrow(
        unconfirmedStatus: Int,
        id: Long,
        setLikedOn: Boolean,
        exeptionText: String
    ) {
        ConsolePrinter.printText("CALL restoreLikesByIdAndThrow")

        // Тут условие противоположное условию основной ф-ции, т.к. надо вернуть назад
        if (!setLikedOn) postDao.likeById(unconfirmedStatus, id)
        else postDao.dislikeById(unconfirmedStatus, id)
        throw RuntimeException(exeptionText)
    }

    override suspend fun shareById(unconfirmedStatus: Int, id: Long) {
        TODO("Not yet implemented")
    }

    override fun getNewerCount(id: Long): Flow<Int> = flow<Int> {
        // Пока я не типизировала интом (flow<Int>), справа отображалось {this:FlowCollector<Nothing>
        // Почему??? Ведь в примерах было просто flow!!!
        while (true) {
            delay(10_000L)
            var response: Response<List<Post>>
            try {
                response = PostsApi.retrofitService.getNewer(id)
                ConsolePrinter.printText("HAVE GOT NEWER RESPONSE")
////
                if (!response.isSuccessful) {
                    throw ApiError(response.code(), response.message())
                }
                val body = response.body() ?: throw ApiError(response.code(), response.message())
                postDao.insert(body.fromDto())
                emit(body.size)
            } catch (e: Exception) {
                ConsolePrinter.printText("HAVE NOT GOT NEWER RESPONSE")
                // Нельзя прерывать Flow, поэтому ошибку игнорируем //throw RuntimeException(e.message.toString())
                ConsolePrinter.printText(e.message.toString())
            }
        }
    }  // .catch { e: Throwable -> throw AppError.from(e) }
        .flowOn(Dispatchers.Default)

    override suspend fun setAllVisible() {
        postDao.setAllVisible()
    }

//    override suspend fun countHidden(): Int {
//        return postDao.countHidden() ?: 0
//    }

}
