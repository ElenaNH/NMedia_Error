package ru.netology.nmedia.repository

import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import ru.netology.nmedia.api.PostsApi
import ru.netology.nmedia.dto.Post
//import java.io.IOException
//import java.util.concurrent.TimeUnit
//import kotlin.Exception
import java.lang.RuntimeException


class PostRepositoryImpl : PostRepository {

    /*override fun getAll(): List<Post> {
        return PostsApi.retrofitService.getAll()
            .execute()
            .let {
                if (!it.isSuccessful) {
                    throw RuntimeException(it.message())
                }
                it.body() ?: throw RuntimeException("body is null")
            }
    }*/

    override fun getAllAsync(callback: PostRepository.Callback<List<Post>>) {
        PostsApi.retrofitService.getAll()
            .enqueue(object : Callback<List<Post>> {
                override fun onResponse(call: Call<List<Post>>, response: Response<List<Post>>) {
                    if (!response.isSuccessful) {
                        callback.onError(RuntimeException(response.message()))
                        return
                    }
                    callback.onSuccess(response.body() ?: throw RuntimeException("body is null"))
                }

                override fun onFailure(call: Call<List<Post>>, t: Throwable) {
                    callback.onError(RuntimeException(t))
                }
            })
    }


    override fun likeById(id: Long, callback: PostRepository.Callback<Post>) {
        // Cначала получим пост, потому что в интерфейсе передается только id
        // поэтому не можем передавать пост, хоть это и удобнее
        getByIdAsync(id, object : PostRepository.Callback<Post> {
            override fun onSuccess(post: Post) {
                super.onSuccess(post)

                // По идее сюда попадаем, когда все уже хорошо, и пост непустой
                // Так что можем спокойно продолжить работать с лайком

                val callPostForLike = if (post.likedByMe) PostsApi.retrofitService.dislikeById(id)
                else PostsApi.retrofitService.likeById(id)

                callPostForLike.enqueue(object : Callback<Post> {
                    override fun onResponse(call: Call<Post>, response: Response<Post>) {
                        if (!response.isSuccessful) {
                            callback.onError(RuntimeException(response.message()))
                            return
                        }
                        callback.onSuccess(
                            response.body() ?: throw RuntimeException("body is null")
                        )
                    }

                    override fun onFailure(call: Call<Post>, t: Throwable) {
                        callback.onError(RuntimeException(t))
                    }
                })


            }

            override fun onError(e: Exception) {
                super.onError(e)
            }
        })


    }

    override fun shareById(id: Long) {
        //TODO("Not yet implemented")  //Наш сервер пока не обрабатывает шаринг, поэтому не наращиваем счетчик

    }

    override fun save(post: Post, callback: PostRepository.Callback<Post>) {
        PostsApi.retrofitService.save(post)
            .enqueue(object : Callback<Post> {
                override fun onResponse(call: Call<Post>, response: Response<Post>) {
                    if (!response.isSuccessful) {
                        callback.onError(RuntimeException(response.message()))
                        return
                    }
                    callback.onSuccess(response.body() ?: throw RuntimeException("body is null"))
                }

                override fun onFailure(call: Call<Post>, t: Throwable) {
                    callback.onError(RuntimeException(t))
                }
            })
    }

    override fun removeById(id: Long, callback: PostRepository.Callback<Unit>) {
        PostsApi.retrofitService.removeById(id)
            .enqueue(object : Callback<Unit> {
                override fun onResponse(call: Call<Unit>, response: Response<Unit>) {
                    if (!response.isSuccessful) {
                        callback.onError(RuntimeException(response.message()))
                        return
                    }
                    callback.onSuccess(response.body() ?: throw RuntimeException("body is null"))
                }

                override fun onFailure(call: Call<Unit>, t: Throwable) {
                    callback.onError(RuntimeException(t))
                }
            })
    }

    private fun getByIdAsync(id: Long, callback: PostRepository.Callback<Post>) {
        PostsApi.retrofitService.getById(id)
            .enqueue(object : Callback<Post> {
                override fun onResponse(call: Call<Post>, response: Response<Post>) {
                    if (!response.isSuccessful) {
                        callback.onError(RuntimeException(response.message()))
                        return
                    }
                    callback.onSuccess(response.body() ?: throw RuntimeException("body is null"))
                    /*val body = requireNotNull(response.body?.string()) { "body is null" }
                    val postEntity = gson.fromJson<PostEntity>(body, typeTokenOnePost.type)
                    // ВОТ ТУТ-ТО ВСЯ И БЕДА - РАНЬШЕ ВОЗВРАЩАЛОСЬ PostEntity
                    // а теперь нам возвращается Post
                    val post = postEntity.toDto()
                    postsCallBack.onSuccess(post)*/
                }

                override fun onFailure(call: Call<Post>, t: Throwable) {
                    callback.onError(RuntimeException(t))
                }
            })


    }


}
