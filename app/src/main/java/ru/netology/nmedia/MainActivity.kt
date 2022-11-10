package ru.netology.nmedia.activity

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import ru.netology.nmedia.R
import ru.netology.nmedia.databinding.ActivityMainBinding
import ru.netology.nmedia.viewmodel.PostViewModel

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val viewModel: PostViewModel by viewModels()
        viewModel.data.observe(this) { post ->
            with(binding) {
                messageAuthor.text = post.author
                messagePublished.text = post.published
                messageContent.text = post.content
                ibtnLikes.setImageResource(
                    if (post.likedByMe) R.drawable.ic_heart_filled_red else R.drawable.ic_heart_unfilled
                )
            }
        }
        // Обработчики кликов
        binding.ibtnLikes.setOnClickListener {
            viewModel.like()
        }
    }
}

/*
            // Тут надо сделать 1К, 1.1К, 1М и т.п.
            txtCountLikes.text = post.countLikes.statisticsToString()
            txtCountShare.text = post.countShare.statisticsToString()
            txtCountViews.text = post.countViews.statisticsToString()
 */


