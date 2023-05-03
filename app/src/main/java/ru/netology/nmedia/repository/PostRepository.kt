package ru.netology.nmedia.repository

import androidx.lifecycle.LiveData
import ru.netology.nmedia.dto.Post

interface PostRepository {
    fun getAll(postsCallBack: PostsCallBack<List<Post>>)

    fun likeById(id: Long, postsCallBack: PostsCallBack<Unit>)  //fun likeById(ratedPost: Post, postsCallBack: PostsCallBack<Unit>)
    fun shareById(id: Long)
    fun removeById(id: Long, postsCallBack: PostsCallBack<Unit>)

    fun save(post: Post, postsCallBack: PostsCallBack<Post>)
}

interface PostsCallBack<T> {
    fun onSuccess(data: T)
    fun onError(e: Exception)
}
