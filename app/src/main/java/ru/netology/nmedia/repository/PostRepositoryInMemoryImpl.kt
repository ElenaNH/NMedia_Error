package ru.netology.nmedia.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import ru.netology.nmedia.R
import ru.netology.nmedia.dto.Post

class PostRepositoryInMemoryImpl : PostRepository {
    private var nextId = 1L
    private var posts = listOf(
        Post(
            id = nextId++,
            author = "Нетология-0. Пустой пост",
            content = "",
            published = "21 мая в 18:36",
            likedByMe = false,
            countLikes = 1099,
            countShare = 997,
            countViews = 5
        ),
        Post(
            id = nextId++,
            author = "Нетология-1.! Университет интернет-профессий будущего",
            content = "Привет, это новая Нетология!",
            published = "21 мая в 18:36",
            likedByMe = false,
            countLikes = 1099,
            countShare = 997,
            countViews = 5
        ),
        Post(
            id = nextId++,
            author = "Sophie Loren",
            content = "Very nice dancing",
            videoLink = "https://youtu.be/CdQqIkx3V88",
            published = "21 мая в 18:37",
            likedByMe = false,
            countLikes = 99,
            countShare = 997,
            countViews = 1005
        ),
        Post(
            id = nextId++,
            author = "Нетология-3. Университет интернет-профессий будущего",
            content = "Привет, это новая Нетология! Когда-то Нетология начиналась с интенсивов по онлайн-маркетингу. Затем появились курсы по дизайну, разработке, аналитике и управлению. Мы растём сами и помогаем расти студентам: от новичков до уверенных профессионалов. Но самое важное остаётся с нами: мы верим, что в каждом уже есть сила, которая заставляет хотеть больше, целиться выше, бежать быстрее. Наша миссия — помочь встать на путь роста и начать цепочку перемен → http://netolo.gy/fyb",
            published = "21 мая в 18:38",
            likedByMe = false,
            countLikes = 1099,
            countShare = 997,
            countViews = 5
        ),
        Post(
            id = nextId++,
            author = "Нетология-4. Университет",
            content = "Добро пожаловать в наш университет!",
            published = "21 мая в 18:31",
            likedByMe = false,
            countLikes = 2098,
            countShare = 12999,
            countViews = 55006
        )
    )
    private val data = MutableLiveData(posts)


    override fun getAll(): LiveData<List<Post>> = data

    override fun likeById(id: Long) {
        posts = posts.map {
            if (it.id != id) it else it.copy(
                likedByMe = !it.likedByMe,
                countLikes = it.countLikes + if (!it.likedByMe) 1 else -1
            )
        }
        data.value = posts
    }

    override fun shareById(id: Long) {
        posts = posts.map {
            if (it.id != id) it else it.copy(countShare = it.countShare + 1)
        }
        data.value = posts
    }

    override fun removeById(id: Long) {
        posts = posts.filter { it.id != id }
        data.value = posts
    }

    override fun save(post: Post) {
        if (post.id == 0L) {
            // ????? TODO: remove hardcoded author & published ?????
            posts = listOf(
                post.copy(
                    id = nextId++,
                    author = "Me",
                    likedByMe = false,
                    published = "now"
                )
            ) + posts
        } else {
            posts = posts.map {
                if (it.id != post.id) it else it.copy(content = post.content)
            }
        }
        data.value = posts
    }

}

