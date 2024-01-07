package ru.netology.nmedia.activity

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
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
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment ImageFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            ImageFragment().apply {
                arguments = Bundle().apply {
//                    putString(ARG_PARAM1, param1)
//                    putString(ARG_PARAM2, param2)
                }
            }
    }

    private val viewModel: PostViewModel by viewModels(ownerProducer = ::requireParentFragment)

    private lateinit var binding: FragmentImageBinding

    // TODO: Rename and change types of parameters
//    private var param1: String? = null
//    private var param2: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        arguments?.let {
//            param1 = it.getString(ARG_PARAM1)
//            param2 = it.getString(ARG_PARAM2)

//        }


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
        val post = viewModel.data.value?.posts?.filter {
            (it.id == current_post_id) && (it.unconfirmed == current_post_unconfirmed)
        }?.first()

        loadImage(post, binding.photo)

        binding.back.setOnClickListener {
            // выходим в предыдущий фрагмент
            findNavController().navigateUp()
        }

        return binding.root
    }

}
