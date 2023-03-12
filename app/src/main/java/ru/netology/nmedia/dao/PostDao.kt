package ru.netology.nmedia.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import ru.netology.nmedia.entity.PostEntity

@Dao
interface PostDao {
    @Query("SELECT * FROM PostEntity ORDER BY id DESC")
    fun getAll(): LiveData<List<PostEntity>>

    @Insert
    fun insert(post: PostEntity)

    @Query("UPDATE PostEntity SET content = :content, videoLink = :videoLink WHERE id = :id")
    fun updateContentById(id: Long, content: String, videoLink: String) // Лучше весь пост передавать

    fun save(post: PostEntity) =
        if (post.id == 0L) insert(post)
        else updateContentById(post.id, post.content, post.videoLink ?: "")

    @Query(
        """
        UPDATE PostEntity SET
        countLikes = countLikes + CASE WHEN likedByMe THEN -1 ELSE 1 END,
        likedByMe = CASE WHEN likedByMe THEN 0 ELSE 1 END
        WHERE id = :id
        """
    )
    fun likeById(id: Long)

    @Query(
        """
        UPDATE PostEntity SET
        countShare = countShare + 1
        WHERE id = :id
        """
    )
    fun shareById(id: Long)
    @Query("DELETE FROM PostEntity WHERE id = :id")
    fun removeById(id: Long)
}

/*
// Старый код
import ru.netology.nmedia.dto.Post

interface PostDao {
    fun getAll(): List<Post>
    fun save(post: Post): Post
    fun likeById(id: Long)
    fun shareById(id: Long)
    fun removeById(id: Long)
}
*/

