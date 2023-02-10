package ru.netology.nmedia.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.launch
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import ru.netology.nmedia.R
import ru.netology.nmedia.adapter.OnInteractionListener
import ru.netology.nmedia.adapter.PostsAdapter
import ru.netology.nmedia.databinding.ActivityMainBinding
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.util.AndroidUtils
import ru.netology.nmedia.viewmodel.PostViewModel
import ru.netology.nmedia.activity.NewPostActivity.NewPostContract

class MainActivity : AppCompatActivity() {
    val viewModel: PostViewModel by viewModels()
    private val interactionListener = object : OnInteractionListener {
        override fun onLike(post: Post) {
            viewModel.likeById(post.id)
        }

        override fun onShare(post: Post) {
            val intent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, post.content)
                type = "text/plain"
            }

            val shareIntent = Intent.createChooser(intent, getString(R.string.chooser_share_post))
            startActivity(shareIntent)

            viewModel.shareById(post.id)    // раньше был только счетчик
        }

        override fun onRemove(post: Post) {
            viewModel.removeById(post.id)
        }

        override fun onEdit(post: Post) {
            viewModel.startEditing(post)
        }
    }

    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    val adapter by lazy { PostsAdapter(interactionListener) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)  // binding вынесли выше и отдали by lazy, и только при первом вызове реально создастся binding

        // Регистрируем контракт, который вернет нам результат запуска новой октивити
        // а мы уже передадим результат в нашу ViewModel
        val newPostContract = registerForActivityResult(NewPostContract) { content ->
            content ?: return@registerForActivityResult
            viewModel.changeContent(content)
            viewModel.save()
        }

        binding.list.adapter =
            adapter   // val adapter = PostsAdapter(interactionListener) вынесли выше и отдали by lazy

        subscribe()
        setListeners()  // все лиснеры, кроме FAB - т.к. не удалось сделать из  контракта переменную уровня MainActivity
        // лиснер для fab
        binding.fab.setOnClickListener {
            // тут какой-то ActivityResultLauncher должен запуститься; почему-то лончер назвали контрактом
            newPostContract.launch()    // newPostContract.launch(Unit) заменили на фаункцию расширения (для стильности)

        }

    }

    private fun subscribe() {
        // Подписки:

        // Подписка на список сообщений
        viewModel.data.observe(this) { posts ->
            adapter.submitList(posts)
        }

        // Подписка на нижнее поле добавления/изменения
        viewModel.edited.observe(this) { post ->

            if (post.id == 0L) {
                // Если нулевой id, значит, в поле edited нашей модели помещен пустой пост
                return@observe
            }
            // Если пост непустой, то запустим окно редактирования поста
            // А текст поста новая активити получит из нашего контракта

            // Регистрируем отдельно еще и тут
            // А как бы так ее получать после того, как она уже зарегистрировалась в onCreate?
            val newPostContract2 = registerForActivityResult(NewPostContract) { content ->
                content ?: return@registerForActivityResult
                viewModel.changeContent(content)
                viewModel.save()
            }
            newPostContract2.launch()
        }

    }

    private fun setListeners() {
        // Обработчики кликов

        // Пока что все обработчики либо в адаптере, либо в другой активити,
        // либо не получилось их сюда вынести (fab)


/*        binding.ibtnSave.setOnClickListener {
            with(binding.editContent) {
                if (text.isNullOrBlank()) {
                    Toast.makeText(
                        this@MainActivity,
                        context.getString(R.string.error_empty_content),
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setOnClickListener
                }

                viewModel.changeContent(text.toString())
                viewModel.save()
            }
            clearEditContent()
        }

        binding.ibtnClear.setOnClickListener {
            viewModel.quitEditing()
            clearEditContent()
        }*/

    }

    /* Сброс редактирования: скрыть кнопки, очистить поле, сбросить фокус, скрыть клавиатуру */
    /*private fun clearEditContent() {
        with(binding.editContent) {
            binding.group.visibility = View.GONE
            setText("")
            clearFocus()
            AndroidUtils.hideKeyboard(this)
        }
        with(binding.txtMessageOld) {
            text = ""
        }
    }*/

}
