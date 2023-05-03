package ru.netology.nmedia.activity

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
//import androidx.fragment.app.viewModels
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import ru.netology.nmedia.databinding.FragmentNewPostBinding
import ru.netology.nmedia.util.AndroidUtils
import ru.netology.nmedia.util.StringArg
import ru.netology.nmedia.viewmodel.PostViewModel
import android.view.Gravity
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.addCallback
import ru.netology.nmedia.R
import ru.netology.nmedia.databinding.FragmentFeedBinding
import ru.netology.nmedia.util.ARG_POST_ID

//import ru.netology.nmedia.databinding.FragmentFeedBinding

class NewPostFragment : Fragment() {

    companion object {
        var Bundle.textArg: String? by StringArg
    }

    //  viewModels используем теперь с аргументом, чтобы сделать общую viewModel для всех фрагментов
//    private val viewModel: PostViewModel by viewModels(ownerProducer = ::requireParentFragment)
    private val viewModel: PostViewModel by activityViewModels()

    // С этим ужасом надо что-то делать:
    private lateinit var binding: FragmentNewPostBinding // как сделать by lazy ????

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // This callback will only be called when MyFragment is at least Started.
        val callback = requireActivity().onBackPressedDispatcher.addCallback(this) {
            // Handle the back button event

            // Для нового поста запоминаем черновик
            // Даже если этот новый пост - недоделанный репост
            //      (кстати, я не знаю, как понять, репост ли он)
            // Но если после попытки создания поста уже заходили в редактирование другого поста,
            // то сбрасываем черновик
            if (viewModel.edited.value?.id ?: 0 == 0L)
                viewModel.setDraftContent(binding.editContent.text.toString())
            else viewModel.setDraftContent("")

            // А выходим в предыдущий фрагмент в любом случае - хоть новый пост, хоть старый
            findNavController().navigateUp()
        }
        // The callback can be enabled or disabled here or in the lambda

    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
// Если удастся объявить binding через by lazy, то этот кусочек кода уйдет
        //val
        binding = FragmentNewPostBinding.inflate(
            inflater,
            container,
            false  // false означает, что система сама добавит этот view, когда посчитает нужным
        )

        arguments?.textArg
            ?.let(binding.editContent::setText) // Задаем текст поста из передаточного элемента textArg

        binding.btnOk.setOnClickListener {

            if (binding.editContent.text.isNullOrBlank()) {
                // Предупреждение о непустом содержимом
                val warnToast = Toast.makeText(
                    this.activity,
                    getString(R.string.error_empty_content),
                    Toast.LENGTH_SHORT
                )
                warnToast.setGravity(Gravity.CENTER_VERTICAL, 0, 0)
                warnToast.show()
                return@setOnClickListener

            } else {
                // Поскольку viewModel общая, то можно прямо тут сохраниться
                viewModel.changeContent(binding.editContent.text.toString())
                viewModel.save()
                AndroidUtils.hideKeyboard(requireView())
                // findNavController().navigateUp()  // Тут нельзя выходить из фрагмента, т.к. еще не загружены посты
                // Теперь запись и загрузка постов разделены (ранее не требовалась отдельная загрузка с сервера)
                viewModel.postCreated.observe(viewLifecycleOwner) { // Загружаем однократно
                    viewModel.loadPosts()
                    // Закрытие текущего фрагмента (переход к нижележащему в стеке)
                    findNavController().navigateUp()
                }
                // Чтобы нельзя было вкинуть пост много раз, блокируем кнопку, пока пост не запишется
                viewModel.postCreateLoading.observe(viewLifecycleOwner) {
                    binding.btnOk.isEnabled = !it
                }

            }

        }
        return binding.root
    }

    // Попробуем вынести создание binding
    private fun makeBinding(
        container: ViewGroup?
    ): FragmentNewPostBinding {
        return FragmentNewPostBinding.inflate(
            layoutInflater,
            container,
            false  // false означает, что система сама добавит этот view, когда посчитает нужным
        )
    }

}
