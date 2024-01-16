package ru.netology.nmedia.activity

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import ru.netology.nmedia.databinding.FragmentImageBinding
import ru.netology.nmedia.uiview.loadImage
import ru.netology.nmedia.util.ARG_POST_ID
import ru.netology.nmedia.util.ARG_POST_UNCONFIRMED
import ru.netology.nmedia.viewmodel.PostViewModel

@AndroidEntryPoint
class ImageFragment : Fragment() {

    companion object {
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            ImageFragment().apply {
                arguments = Bundle().apply {
                }
            }
    }

//    private val viewModel: PostViewModel by viewModels(ownerProducer = ::requireParentFragment)
    private val viewModel: PostViewModel by activityViewModels()

    private lateinit var binding: FragmentImageBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        //return inflater.inflate(R.layout.fragment_image, container, false)

        binding = FragmentImageBinding.inflate(
            inflater,
            container,
            false
        )


        // Находим наш пост
        val current_post_id = arguments?.getLong(ARG_POST_ID) ?: 0
        val current_post_unconfirmed = arguments?.getInt(ARG_POST_UNCONFIRMED) ?: 0
        val post = viewModel.emptyPostForCurrentUser()
        /*val post = viewModel.data.value?.posts?.filter {
            (it.id == current_post_id) && (it.unconfirmed == current_post_unconfirmed)
        }?.first()*/

        loadImage(post, binding.photo)

        binding.back.setOnClickListener {
            // выходим в предыдущий фрагмент
            findNavController().navigateUp()
        }

        return binding.root
    }

}
