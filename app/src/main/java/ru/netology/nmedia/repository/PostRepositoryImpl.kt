package ru.netology.nmedia.repository

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.entity.PostEntity
import java.util.concurrent.TimeUnit


class PostRepositoryImpl : PostRepository {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .build()
    private val gson = Gson()

    /*private val typeToken = object : TypeToken<List<Post>>() {}
    private val typeTokenOnePost = object : TypeToken<Post>() {}*/
    private val typeToken = object : TypeToken<List<PostEntity>>() {}
    private val typeTokenOnePost = object : TypeToken<PostEntity>() {}

    companion object {
        private const val BASE_URL = "http://10.0.2.2:9999"
        private val jsonType = "application/json".toMediaType()
    }

    /*    private fun getAllTEST(): List<Post> {
            val request: Request = Request.Builder()
                .url("${BASE_URL}/api/slow/posts")
                .build()

            return client.newCall(request)
                .execute()
                .let { it.body?.string() ?: throw RuntimeException("body is null") }
                .let {
                    gson.fromJson(it, typeToken.type)
                }.map { entity -> entity.toDto() }
        }*/

    private fun getAllEntities(): List<PostEntity> {
        val request: Request = Request.Builder()
            .url("${BASE_URL}/api/slow/posts")
            .build()

        val typeVal = PostEntity(0, "", "", "", false, 0)
        return client.newCall(request)
            .execute()
            .let { it.body?.string() ?: throw RuntimeException("body is null") }
            .let {
                gson.fromJson(it, typeToken.type)
            }
    }

    override fun getAll(): List<Post> {
        return getAllEntities()
            .map { entity -> entity.toDto() }
    }

    override fun likeById(id: Long) {
        // TODO: do this in homework
        // POST /api/posts/{id}/likes
        // DELETE /api/posts/{id}/likes

        val post = getById(id)  // Т.к. репозиторий ничего не знает про ViewModel, то идем на сервер

        val request: Request = when {
            (post.likedByMe) -> Request.Builder()
                .delete(gson.toJson(post).toRequestBody(jsonType))
                .url("${BASE_URL}/api/posts/$id/likes")
                .build()

            else -> Request.Builder()
                .post(gson.toJson(post).toRequestBody(jsonType))
                .url("${BASE_URL}/api/posts/$id/likes")
                .build()
        }
        client.newCall(request)
            .execute()
            .close()


    }

    override fun shareById(id: Long) {
        TODO("Not yet implemented")

    }

    override fun save(post: Post) {
        val request: Request = Request.Builder()
            .post(gson.toJson(post).toRequestBody(jsonType))
            .url("${BASE_URL}/api/slow/posts")
            .build()

        client.newCall(request)
            .execute()
            .close()
    }

    override fun removeById(id: Long) {
        val request: Request = Request.Builder()
            .delete()
            .url("${BASE_URL}/api/slow/posts/$id")
            .build()

        client.newCall(request)
            .execute()
            .close()
    }

    /*        private fun getTESTById(id: Long): Post {
            val request: Request = Request.Builder()
                .url("${BASE_URL}/api/slow/posts/$id")
                .build()

            return client.newCall(request)
                .execute()
                .let { it.body?.string() ?: throw RuntimeException("body is null") }
                .let {
                    gson.fromJson(it, typeTokenOnePost.type)
                }
            .toDto()
        }*/

    private fun getEntityById(id: Long): PostEntity {
        val request: Request = Request.Builder()
            .url("${BASE_URL}/api/slow/posts/$id")
            .build()

        return client.newCall(request)
            .execute()
            .let { it.body?.string() ?: throw RuntimeException("body is null") }
            .let {
                gson.fromJson(it, typeTokenOnePost.type)
            }
    }

    private fun getById(id: Long): Post {
        return getEntityById(id)
            .toDto()
    }
}
