package ru.netology.nmedia.activity

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
//import androidx.fragment.app.viewModels       // Вместо этого используем androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
//import androidx.navigation.Navigation.findNavController  // этот не подходит
//import androidx.navigation.findNavController  // и этот не подходит (но он использовался для перехода из активити)
import ru.netology.nmedia.R
import ru.netology.nmedia.activity.NewPostFragment.Companion.textArg
import ru.netology.nmedia.uiview.PostInteractionListenerImpl // Было до клиент-серверной модели
import ru.netology.nmedia.adapter.PostsAdapter
import ru.netology.nmedia.viewmodel.PostViewModel
import ru.netology.nmedia.databinding.FragmentFeedBinding
//
import android.content.Intent
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.navigation.findNavController
import ru.netology.nmedia.adapter.OnInteractionListener
import ru.netology.nmedia.dto.Post


class FeedFragment : Fragment() {
    //  viewModels используем теперь с аргументом, чтобы сделать общую viewModel для всех фрагментов
//    val viewModel: PostViewModel by viewModels(ownerProducer = ::requireParentFragment)
    private val viewModel: PostViewModel by activityViewModels()

    // interactionListener должен быть доступен также из фрагмента PostFragment
    private val interactionListener by lazy { PostInteractionListenerImpl(viewModel, this) }

    // создаем привязку к элементам макета по первому обращению к ним
    //private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    private lateinit var binding: FragmentFeedBinding // как сделать by lazy ????

    val adapter by lazy { PostsAdapter(interactionListener) }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = makeBinding(
            container
        )   // в лекции layoutInflater, а в примерах переданный параметр inflater

        // Содержимое onCreate, оставшееся от activity, должно быть здесь
        binding.list.adapter =
            adapter   // val adapter = PostsAdapter(interactionListener) вынесли выше и отдали by lazy
        subscribe()     // все подписки, которые могут нам потребоваться в данной активити
        setListeners()  // все лиснеры всех элементов данной активити

        return binding.root
    }


    private fun makeBinding(
        container: ViewGroup?
    ): FragmentFeedBinding {
        return FragmentFeedBinding.inflate(
            layoutInflater,
            container,
            false  // false означает, что система сама добавить этот view, когда посчитает нужным
        )  // в лекции layoutInflater, а в примерах переданный параметр inflater
    }

    private fun subscribe() {
        // Подписки:

        // Подписка на список сообщений
        /*viewModel.data.observe(viewLifecycleOwner) { posts ->
            val newPost = (adapter.currentList.size < posts.size) // элементов в списке стало больше
            // далее обновление списка отображаемых постов в RecyclerView
            adapter.submitList(posts) {
                // далее - прокрутка до верхнего элемента списка (с индексом 0)
                // ее нужно делать только если обновился список в адаптере
                // иначе он не к верхнему прокрутит, а ко второму
                if (newPost) binding.list.smoothScrollToPosition(0)
            }
        }*/
        viewModel.data.observe(viewLifecycleOwner) { state ->
            adapter.submitList(state.posts)
            binding.progress.isVisible = state.loading
            binding.errorGroup.isVisible = state.error
            binding.emptyText.isVisible = state.empty
        }

    }

    private fun setListeners() {
        // Обработчики кликов

        binding.retryButton.setOnClickListener {
            viewModel.loadPosts()
        }

        // Пока что все обработчики либо в адаптере, либо в другом обработчике,
        // fab не получилось сделать безопасно (не знаю, как сделать by lazy с аргументами)

        binding.fab.setOnClickListener {
            // Запуск фрагмента NewPostFragment
            findNavController().navigate(
                R.id.action_feedFragment_to_newPostFragment,
                Bundle().apply {
                    //textArg = ""  // В запускаемый фрагмент передаем пустое содержимое нового поста
                    textArg = viewModel.getDraftContent()  // В запускаемый фрагмент передаем содержимое черновика
                }
            )

        }

    }
}
