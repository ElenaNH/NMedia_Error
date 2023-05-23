package ru.netology.nmedia.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
//import ru.netology.nmedia.BuildConfig
//Build Type 'debug' contains custom BuildConfig fields, but the feature is disabled.
import ru.netology.nmedia.dto.Post

//private const val BASE_URL = "${BuildConfig.BASE_URL}/api/slow/"
//Build Type 'debug' contains custom BuildConfig fields, but the feature is disabled.
private const val BASE_URL = "https://10.0.2.2:9999/api/slow/"

private val logging = HttpLoggingInterceptor().apply {
//    if (BuildConfig.DEBUG) {
    if (1 == 1) {
        level = HttpLoggingInterceptor.Level.BODY
    }
}

private val okhttp = OkHttpClient.Builder()
    .addInterceptor(logging)
    .build()

private val retrofit = Retrofit.Builder()
    .addConverterFactory(GsonConverterFactory.create())
    .baseUrl(BASE_URL)
    .client(okhttp)
    .build()

interface PostsApiService {
    @GET("posts")
    fun getAll(): Call<List<Post>>

    @GET("posts/{id}")
    fun getById(@Path("id") id: Long): Call<Post>

    @POST("posts")
    fun save(@Body post: Post): Call<Post>

    @DELETE("posts/{id}")
    fun removeById(@Path("id") id: Long): Call<Unit>

    @POST("posts/{id}/likes")
    fun likeById(@Path("id") id: Long): Call<Post>

    @DELETE("posts/{id}/likes")
    fun dislikeById(@Path("id") id: Long): Call<Post>
}

object PostsApi {
    //val service: PostsApiService by lazy {
    val retrofitService: PostsApiService by lazy {
        retrofit.create(PostsApiService::class.java)
    }
}