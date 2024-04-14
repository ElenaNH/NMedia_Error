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
//import android.view.Gravity
//import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView.AdapterDataObserver
import com.google.android.material.snackbar.Snackbar
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.paging.LoadState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import ru.netology.nmedia.R
import ru.netology.nmedia.activity.NewPostFragment.Companion.textArg
import ru.netology.nmedia.adapter.PostLoadingStateAdapter
import ru.netology.nmedia.uiview.PostInteractionListenerImpl // Было до клиент-серверной модели
import ru.netology.nmedia.adapter.PostsAdapter
import ru.netology.nmedia.viewmodel.PostViewModel
import ru.netology.nmedia.databinding.FragmentFeedBinding
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.enumeration.PostActionType
import ru.netology.nmedia.uiview.goToLogin
import ru.netology.nmedia.util.ConsolePrinter
import ru.netology.nmedia.viewmodel.AuthViewModel

@AndroidEntryPoint
class FeedFragment : Fragment() {
    //    private val viewModel: PostViewModel by viewModels(ownerProducer = ::requireParentFragment)
    private val viewModel: PostViewModel by activityViewModels()
    private val authViewModel: AuthViewModel by activityViewModels()

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
            adapter.withLoadStateHeaderAndFooter(
                header = PostLoadingStateAdapter { adapter.retry() },
                footer = PostLoadingStateAdapter { adapter.retry() }
            )
        // val adapter = PostsAdapter(interactionListener) вынесли выше и отдали by lazy

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

        // на логон/логоф
        this.activity?.let { thisActivity ->
            authViewModel.data.observe(thisActivity) {
                ConsolePrinter.printText("authViewModel.data.observe - adapter.refresh()")
                adapter.refresh()
                val stop1 = 1
            }
        }


        // Подписка на FeedModelState - состояние списка сообщений
        viewModel.dataState.observe(viewLifecycleOwner) { state ->
            binding.progress.isVisible = state.loading
            if (state.error) {
                Snackbar.make(binding.root, R.string.error_loading, Snackbar.LENGTH_LONG)
                    .setAction(R.string.retry_loading) { viewModel.loadPosts() }
                    .show()
            }
            binding.refreshLayout.isRefreshing = state.refreshing
        }
        lifecycleScope.launchWhenCreated {
            viewModel.data.collectLatest {
                adapter.submitData(it)
            }
        }
        lifecycleScope.launchWhenCreated {
            adapter.loadStateFlow.collectLatest {
                binding.refreshLayout.isRefreshing = it.refresh is LoadState.Loading
                        || it.append is LoadState.Loading
                        || it.prepend is LoadState.Loading

            }
        }

        // Подписка на адаптер
        adapter.registerAdapterDataObserver(object : AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                if (positionStart == 0) {
                    binding.someUnread.isVisible = false
                    binding.list.smoothScrollToPosition(0)
                }
            }
        })


        // Подписка на однократную ошибку
        viewModel.postActionFailed.observe(viewLifecycleOwner) { // Сообщаем однократно
            whenPostActionFailed(binding.root, viewModel, it)
            lifecycleScope.launchWhenCreated {
                if (it == PostActionType.ACTION_POST_LIKE_CHANGE) {
                    viewModel.data.collectLatest {
                        adapter.submitData(it)
                    }
                }
            }
        }

    }

    private fun setListeners() {
        // Обработчики кликов

        binding.fab.setOnClickListener {

            if (viewModel.isAuthorized) {
                // Запуск фрагмента NewPostFragment
                findNavController().navigate(
                    R.id.action_feedFragment_to_newPostFragment,
                    Bundle().apply {
                        ConsolePrinter.printText("Draft content for textArg = ${viewModel.getDraftContent()}")
                        //Через вьюмодель
                        viewModel.startEditing(
                            viewModel.draft.value ?: viewModel.emptyPostForCurrentUser()
                        )
                        //Через аргумент
                        textArg =
                            viewModel.getDraftContent()  // В запускаемый фрагмент передаем содержимое черновика
                        // Эта передача имеет смысл для двух разных активитей, а у нас фрагменты
                        // так что это архаизм, и можно все передать через вьюмодель
                    }
                )
            } else {
                goToLogin(this)
            }
        }

        binding.refreshLayout.setOnRefreshListener {
            //viewModel.refresh()
            adapter.refresh()
        }

        binding.someUnread.setOnClickListener {
            // Показать скрытые посты
            viewModel.setAllVisible()
            // Скрыть кнопку
            it.isVisible = false
            // Прокрутить к верхнему посту можно в наблюдателе за адаптером (подписаться на него)

        }

    }
}
