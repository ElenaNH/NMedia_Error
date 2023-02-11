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
import com.google.android.material.snackbar.Snackbar
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
            // Следующая строка необязательная - интент на красивый выбор запускаемого приложения
            val shareIntent = Intent.createChooser(intent, getString(R.string.chooser_share_post))
            // А здесь мы могли запустить наш intent без красоты, либо улучшенный shareIntent
            startActivity(shareIntent)
            // Увеличиваем счетчик шаринга
            viewModel.shareById(post.id)
        }

        override fun onRemove(post: Post) {
            viewModel.removeById(post.id)
        }

        override fun onEdit(post: Post) {
            viewModel.startEditing(post)
            // Если пост непустой, то запустим окно редактирования поста
            // А текст поста новая активити получит из нашего контракта
            // Но для этого мы должны передать его в лончер в качестве аргумента
            newPostContract.launch(post.content)
        }
    }

    // создаем привязку к элементам макета по первому обращению к ним
    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    val adapter by lazy { PostsAdapter(interactionListener) }

    // Регистрируем контракт, который вернет нам результат запуска новой октивити
    // а мы уже передадим результат в нашу ViewModel
    val newPostContract = registerForActivityResult(NewPostContract) { content ->
        content ?: return@registerForActivityResult
        viewModel.changeContent(content)
        viewModel.save()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)  // binding вынесли выше и отдали by lazy, и только при первом вызове реально создастся binding


        binding.list.adapter =
            adapter   // val adapter = PostsAdapter(interactionListener) вынесли выше и отдали by lazy

        subscribe()     // все подписки, которые могут нам потребоваться в данной активити
        setListeners()  // все лиснеры всех элементов данной активити

    }

    private fun subscribe() {
        // Подписки:

        // Подписка на список сообщений
        viewModel.data.observe(this) { posts ->
            adapter.submitList(posts)   // обновление списка отображаемых постов в RecyclerView
            // далее - прокрутка до верхнего элемента списка (с индексом 0)
            // ПОЧЕМУ-ТО ПРОКРУЧИВАЕТ К ЭЛЕМЕНТУ С ИНДЕКСОМ 1, а не к верхнему
            binding.list.smoothScrollToPosition(0)
        }

/*        // Подписка на нижнее поле добавления/изменения
        // После переноса редактирования в другую активить потеряла смысл
        viewModel.edited.observe(this) { post ->

            if (post.id == 0L) {
                // Если нулевой id, значит, в поле edited нашей модели помещен пустой пост
                // То есть, редактирование закончено или отменено
                return@observe
            }
            // Если пост непустой, то ...


        }*/

    }

    private fun setListeners() {
        // Обработчики кликов

        // Пока что все обработчики либо в адаптере, либо в другой активити,
        // тут только один fab

        binding.fab.setOnClickListener {
            // тут объект ActivityResultLauncher должен запуститься;
            // почему-то этот лончер назвали контрактом
            // в классной работе нам не требовалось передавать аргумент, поэтому мы запускали активить с объектом Unit
            //newPostContract.launch()    // newPostContract.launch(Unit) заменили на функцию расширения (для стильности)
            newPostContract.launch(null)  // а тут запускаем с null, т.к. пост новый, у него еще нет контента
        }

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
