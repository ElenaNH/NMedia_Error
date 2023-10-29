package ru.netology.nmedia.activity

import android.app.Activity
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
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.net.toFile
import androidx.core.view.MenuProvider
import androidx.core.view.isGone
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.github.dhaval2404.imagepicker.ImagePicker
import ru.netology.nmedia.R
import ru.netology.nmedia.enumeration.AttachmentType
import ru.netology.nmedia.util.ConsolePrinter
import java.net.URI
import android.net.Uri
import androidx.core.graphics.green
import androidx.core.graphics.toColor

//import ru.netology.nmedia.databinding.FragmentFeedBinding
val BASE_URL = "http://10.0.2.2:9999"

class NewPostFragment : Fragment() {

    companion object {
        var Bundle.textArg: String? by StringArg
    }

    //  viewModels используем теперь с аргументом, чтобы сделать общую viewModel для всех фрагментов
//    private val viewModel: PostViewModel by viewModels(ownerProducer = ::requireParentFragment)
    private val viewModel: PostViewModel by activityViewModels()

    private val photoLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode != Activity.RESULT_OK) {
                return@registerForActivityResult
            }
            val uri =
                requireNotNull(it.data?.data)   //1-я data - это интент, а вторая - ресурс данного интента

            val file =
                uri.toFile() // Если бы ранее не потребовали существования uri, то так: uri?.toFile()

            viewModel.setPhoto(uri, file)
        }

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
            // TODO - надо ли сделать сохранение черновиковой картинки локально?
            // Наверное, она и так сохранена в памяти за счет общей модели
            else {
                viewModel.setDraftContent("")
                viewModel.clearPhoto()
            }

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

        if (viewModel.edited.value?.attachment?.type == AttachmentType.IMAGE)
            binding.photoContainer.isVisible = true
        else binding.photoContainer.isGone = true
        binding.editContent.requestFocus()

        // Подписки этого фрагмента
        subscribe()

        requireActivity().addMenuProvider(
            object : MenuProvider {
                override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                    menuInflater.inflate(R.menu.save_menu, menu)
                    //R.menu.save_menu.green   // Тоже не помогает :(
                }

                override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                    return when (menuItem.itemId) {
                        R.id.save -> {
                            if (binding.editContent.text.isNullOrBlank()) {
                                // Предупреждение о непустом содержимом
                                val warnToast = Toast.makeText(
                                    activity,
                                    getString(R.string.error_empty_content),
                                    Toast.LENGTH_SHORT
                                )
                                warnToast.setGravity(Gravity.CENTER_VERTICAL, 0, 0)
                                warnToast.show()
                                //return@setOnClickListener

                            } else {
                                // Поскольку viewModel общая, то можно прямо тут сохраниться
                                viewModel.changeContent(binding.editContent.text.toString())
                                viewModel.save()
                                AndroidUtils.hideKeyboard(requireView())

                            }
                            true
                        }

                        else -> false
                    }
                }
            },
            viewLifecycleOwner,
        )

        viewModel.photo.observe(viewLifecycleOwner) { photo ->
            if (photo == null) {
                binding.photoContainer.isGone = true
                return@observe
            }

            binding.photoContainer.isVisible = true
            binding.photo.setImageURI(photo.uri)
        }

        binding.gallery.setOnClickListener {
            ImagePicker.Builder(this)  // this в нашем случае это NewPostFragment
                .galleryOnly()
                .crop()
                .maxResultSize(2048, 2048)
                .createIntent(photoLauncher::launch) // чуть более короткая запись
        }
        binding.takePhoto.setOnClickListener {
            ImagePicker.Builder(this)  // this в нашем случае это NewPostFragment
                .cameraOnly()
                .crop()
                .maxResultSize(2048, 2048)
                .createIntent {
                    photoLauncher.launch(it) // чуть более длинная запись
                }
        }

        binding.removeAttachment.setOnClickListener {
            viewModel.clearPhoto()
        }

        return binding.root
    }

    private fun subscribe() {
        // Отдельно выходим из фрагмента, и отдельно обновляем посты
        viewModel.postSavingStarted.observe(viewLifecycleOwner) { // Выходим однократно
            ConsolePrinter.printText("postSavingStarted observe: Before navigation up...")
            // Закрытие текущего фрагмента (переход к нижележащему в стеке)
            findNavController().navigateUp()
            ConsolePrinter.printText("After navigation up...")
        }
        viewModel.postCreated.observe(viewLifecycleOwner) { // Загружаем однократно
            ConsolePrinter.printText("postCreated observe: Before loading posts...")
            viewModel.loadPosts()
            ConsolePrinter.printText("After loading posts...")
        }

        // !!!!! Если ошибка произошла в этом фрагменте до его закрытия, то postActionFailed
        // Подписка на однократную ошибку
        viewModel.postActionFailed.observe(viewLifecycleOwner) {
            whenPostActionFailed(binding.root, viewModel, it)
        }
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
