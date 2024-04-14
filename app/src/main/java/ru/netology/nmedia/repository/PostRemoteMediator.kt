package ru.netology.nmedia.repository

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import retrofit2.HttpException
import ru.netology.nmedia.api.ApiService
import ru.netology.nmedia.dao.PostDao
import ru.netology.nmedia.dao.PostRemoteKeyDao
import ru.netology.nmedia.db.AppDb
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.entity.PostEntity
import ru.netology.nmedia.entity.PostRemoteKeyEntity
import ru.netology.nmedia.error.ApiError
import ru.netology.nmedia.util.ConsolePrinter
import java.io.IOException

// PagingSource<Long, Post> (Long - это тип уникального идентификатора для класса Post)
@OptIn(ExperimentalPagingApi::class)
class PostRemoteMediator(
    private val service: ApiService,
    private val postDao: PostDao,
    private val postRemoteKeyDao: PostRemoteKeyDao,
    private val appDb: AppDb,
) : RemoteMediator<Int, PostEntity>() {
    // getRefreshKey не будем использовать (она нужна, чтобы использовать ключ при обновлении данных)
    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, PostEntity>
    ): MediatorResult {
        try {
            val postDaoEmpty = postDao.isEmpty()
            val result = when (loadType) {
                LoadType.REFRESH -> {
                    // Не затирать старые данные, а добавлять сверху новые
                    if (postDaoEmpty) {
                        service.getLatest(state.config.pageSize)
                    } else {
                        val id = postRemoteKeyDao.max() ?: return MediatorResult.Success(false)
                        service.getAfter(id, state.config.pageSize)
                    }
                }

                LoadType.PREPEND -> {
                    // Автоматическое обновление отключить
                    //val id = postRemoteKeyDao.max() ?: return MediatorResult.Success(false)
                    //service.getAfter(id, state.config.pageSize)  //Response<List<Post>>

                    // Мы выходим с успехом, как будто все записали в базу,
                    // но на самом деле ничего не записываем
                    return MediatorResult.Success(true)
                }

                LoadType.APPEND -> {
                    // Добавлять снизу новые данные
                    val id = postRemoteKeyDao.min() ?: return MediatorResult.Success(false)
                    service.getBefore(id, state.config.pageSize)
                }
            }

            if (!result.isSuccessful) {
                throw HttpException(result)
            }

            val body = result.body() ?: throw ApiError(result.code(), result.message())

// Локальная БД (транзакция)
            with(appDb) {
                when (loadType) {
                    LoadType.REFRESH -> {

                        //postDao.clear()   - Не затирать предыдущий кэш при обновлении

                        if (postDaoEmpty) {
                            // Для пустого списка делаем реальный рефреш
                            postRemoteKeyDao.insert(
                                listOf(
                                    PostRemoteKeyEntity(
                                        PostRemoteKeyEntity.KeyType.AFTER,
                                        body.first().id
                                    ),
                                    PostRemoteKeyEntity(
                                        PostRemoteKeyEntity.KeyType.BEFORE,
                                        body.last().id
                                    ),  // Два элемента обеспечат первичный REFRESH
                                )
                            )
                        } else {
                            // Это PREPEND вместо REFRESH
                            postRemoteKeyDao.insert(
                                listOf(
                                    PostRemoteKeyEntity(
                                        PostRemoteKeyEntity.KeyType.AFTER,
                                        body.first().id
                                    ),
                                )
                            )
                        }
                    }

                    LoadType.APPEND -> {
                        postRemoteKeyDao.insert(
                            PostRemoteKeyEntity(
                                PostRemoteKeyEntity.KeyType.BEFORE,
                                body.last().id
                            ),
                        )
                    }

//              Сюда НЕ МОЖЕМ ПОПАСТЬ, поскольку ранее выходим по return
                    LoadType.PREPEND -> {
//                        postRemoteKeyDao.insert(
//                            PostRemoteKeyEntity(
//                                PostRemoteKeyEntity.KeyType.AFTER,
//                                body.first().id
//                            ),
//                        )
                    }
                }

                // Теперь, когда
                postDao.insert(body.map(PostEntity.Companion::fromDto))  //postDao.insert(body.map{ PostEntity.fromDto(it) })

            }

// Завершение обработки локальной БД

            return MediatorResult.Success(body.isEmpty()) // Успех, если сюда дошли

        } catch (e: IOException) {
            ConsolePrinter.printText("PostRemoteMediator.load ERROR!!!")
            return MediatorResult.Error(e)
        }
    }

}
