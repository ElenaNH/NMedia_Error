package ru.netology.nmedia.activity

import android.app.Activity
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
//import androidx.fragment.app.viewModels
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import android.view.Gravity
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.net.toFile
import androidx.core.view.MenuProvider
import androidx.core.view.isGone
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.github.dhaval2404.imagepicker.ImagePicker
import ru.netology.nmedia.databinding.FragmentNewPostBinding
import ru.netology.nmedia.util.AndroidUtils
import ru.netology.nmedia.util.StringArg
import ru.netology.nmedia.viewmodel.PostViewModel
import ru.netology.nmedia.R
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.model.DraftModel
import ru.netology.nmedia.model.PhotoModel
import ru.netology.nmedia.util.BASE_URL
import ru.netology.nmedia.util.ConsolePrinter

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

            // Для нового поста запоминаем черновик. Даже если этот новый пост - недоделанный репост
            // Но если после попытки создания поста уже заходили в редактирование другого поста, то сбрасываем черновик
            if (viewModel.edited.value?.id ?: 0 == 0L) {
                viewModel.setDraft(
                    viewModel.edited.value?.copy(content = binding.editContent.text.toString())
                ) //viewModel.setDraftContent(binding.editContent.text.toString())
                ConsolePrinter.printText("Draft content saved: ${viewModel.draft.value?.post?.content ?: ""}")
            } else {
                viewModel.setDraft(Post.getEmptyPost()) //viewModel.setDraftContent("")
                ConsolePrinter.printText("Draft content cleared")
            }
            viewModel.clearPhoto()  // Установим при следующем входе во фрагмент, а сейчас почистим

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
            false  // false означает, что система сама добавыит этот view, когда посчитает нужным
        )

        arguments?.textArg
            ?.let(binding.editContent::setText) // Задаем текст поста из передаточного элемента textArg

        if (viewModel.edited.value?.id == 0L) {  // новый пост
            // Передачу контента для примера передали выше через textArg, как раньше для двух активитей было сделано,
            // а остальное возьмем прямо из вьюмодели
            viewModel.draft.value?.photo?.let { draftPhoto ->
                viewModel.setPhoto(draftPhoto.uri, draftPhoto.file)
            }
        } else {  //редактирование
            showHideAttachment(viewModel.photo.value) // Отображение картинки при наличии
        }

        // Подписки этого фрагмента
        subscribe()

        requireActivity().addMenuProvider(
            object : MenuProvider {
                override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                    menuInflater.inflate(R.menu.save_menu, menu)
                }

                override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                    return when (menuItem.itemId) {
                        R.id.save -> {
                            if (binding.editContent.text.isNullOrBlank() && (viewModel.photo.value == null)) {
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

                                // После записи есть смысл почистить viewModel._photo
                                viewModel.clearPhoto()
                            }
                            true
                        }

                        else -> false
                    }
                }
            },
            viewLifecycleOwner,
        )

        // Воплощаем все элементы текущего фрагмента
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
        viewModel.photo.observe(viewLifecycleOwner) { photo ->
            // Отрисовка оттача
            showHideAttachment(photo)

        }

    }

    private fun showHideAttachment(photo: PhotoModel?) {
        val post = requireNotNull(viewModel.edited.value)
        if ((photo == null) && (post.attachment == null)) {
            ConsolePrinter.printText("Null photo and attach => photoContainer.isGone=true")
            binding.photoContainer.isGone = true
            //return@observe
        } else {
            ConsolePrinter.printText("Not null photo or attach => photoContainer.isVisible=true")
            binding.photoContainer.isVisible = true

            if (photo == null) { // => attach is not null
                try {
                    val imgUrl =
                        "$BASE_URL/media/${post.attachment?.url ?: ""}" // Если нет аттача, то мы сюда не попадем, но все же обработаем null
                    //Ниже идет работающее решение от Романа Лешина,
                    // а мое еще ниже
                    /*Glide.with(binding.photo)
                        .load(imgUrl)
                        .error(R.drawable.ic_error_100dp)
                        .timeout(10_000)
                        .into(
                            object : CustomTarget<Drawable>() {
                                override fun onResourceReady(
                                    resource: Drawable,
                                    transition: Transition<in Drawable>?
                                ) {
                                    binding.photo.setImageDrawable(resource)
                                    val layoutParams = binding.photo.layoutParams
                                    val width = resource.intrinsicWidth
                                    val height = resource.intrinsicHeight

                                    val displayMetrics =
                                        binding.root.context.resources.displayMetrics
                                    val screenWidth = displayMetrics.widthPixels
                                    layoutParams.width = screenWidth

                                    val calculatedHeight =
                                        (screenWidth.toFloat() / width.toFloat() * height).toInt()
                                    layoutParams.height = calculatedHeight
                                    binding.photo.layoutParams = layoutParams
                                }

                                override fun onLoadCleared(placeholder: Drawable?) {
                                    binding.photo.setImageDrawable(placeholder)
                                }
                            }
                        )*/
                    // Условие для загрузки: wrap_content для высоты photo
                    Glide.with(binding.photo)
                        .load(imgUrl)
                        .error(R.drawable.ic_error_100dp)
                        .timeout(10_000)
                        .into(binding.photo)
                } catch (e: Exception) {
                    binding.photo.setImageResource(R.drawable.ic_loading_100dp)

                }
            } else {
                binding.photo.setImageURI(photo.uri)
            }
            val point2 = 2

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
