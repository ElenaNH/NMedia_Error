package ru.netology.nmedia.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.core.view.isVisible
import androidx.paging.PagingDataAdapter  //import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide   //import com.squareup.picasso.Picasso
import ru.netology.nmedia.BuildConfig
import ru.netology.nmedia.R
import ru.netology.nmedia.databinding.CardAdBinding
import ru.netology.nmedia.databinding.CardPostBinding
import ru.netology.nmedia.dto.Ad
import ru.netology.nmedia.dto.FeedItem
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.dto.statisticsToString
import ru.netology.nmedia.enumeration.AttachmentType
import ru.netology.nmedia.uiview.loadImage

interface OnInteractionListener {
    fun onLike(post: Post) {}
    fun onShare(post: Post) {}
    fun onEdit(post: Post) {}
    fun onRemove(post: Post) {}
    fun onVideoLinkClick(post: Post) {}
    fun onViewSingle(post: Post) {}
}


class PostsAdapter(private val onInteractionListener: OnInteractionListener) :
    PagingDataAdapter<FeedItem, RecyclerView.ViewHolder>(PostDiffCallback()) {
    override fun getItemViewType(position: Int): Int {
        // Получение типа элемента из данных:
        // Можно описать свои константы типа Int
        // либо использовать сгенерированные R.layout.id (рекомендуется, т.к. они уникальные)
        return when (getItem(position)) {
            is Ad -> R.layout.card_ad
            is Post -> R.layout.card_post
            null -> error("unknown item type")
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        when (viewType) {
            R.layout.card_post -> {
                val binding =
                    CardPostBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                PostViewHolder(binding, onInteractionListener)
            }

            R.layout.card_ad -> {
                val binding =
                    CardAdBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                AdViewHolder(binding)
            }

            else -> error("unknown view type: $viewType")
        }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is Ad -> (holder as? AdViewHolder)?.bind(item)
            is Post -> (holder as? PostViewHolder)?.bind(item)
            null -> error("unknown item type")
        }
    }

}

class AdViewHolder(
    private val binding: CardAdBinding,
) : RecyclerView.ViewHolder(binding.root) {
    fun bind(ad: Ad) {

        Glide.with(binding.image).load("${BuildConfig.BASE_URL}/media/${ad.image}")
            //.placeholder(R.drawable.ic_loading_100dp)
            .error(R.drawable.ic_error_100dp).timeout(10_000).into(binding.image)
        binding.image

        // binding.image.load("${BuildConfig.BASE_URL}/media/${ad.image}")   // не компилится
    }
}

class PostViewHolder(
    private val binding: CardPostBinding, private val onInteractionListener: OnInteractionListener
) : RecyclerView.ViewHolder(binding.root) {
    fun bind(post: Post) {
        binding.apply {
            messageAuthor.text = post.author
            messagePublished.text = post.published
            messageContent.text = post.content
            // Наличие прикрепленной картинки первично по отношению к наличию ссылки => отображаем аттач, если есть
            if ((post.attachment != null) and (post.attachment?.type == AttachmentType.IMAGE)) {
                // Сначала сбросим старое изображение
//                videoLinkPic.setImageDrawable(null)  // перенесли в loadImage
                loadImage(post, videoLinkPic)
            } else if ((post.videoLink ?: "").trim() == "") videoLinkPic.setImageDrawable(null)
            else videoLinkPic.setImageResource(R.mipmap.ic_banner_foreground)
            // Для MaterialButton (но не для Button)
            ibtnLikes.isChecked = post.likedByMe
            ibtnLikes.text =
                post.likes.toLong().statisticsToString() // Число лайков прямо на кнопке
            ibtnShare.text = post.countShare.toLong().statisticsToString()
            if (post.unsaved == 1) btnViews.setIconResource(R.drawable.ic_eye_of_view_off)
            else btnViews.setIconResource(R.drawable.ic_eye_of_view)

            btnViews.text = post.countViews.toLong().statisticsToString()


            // Обработчики кликов

            ibtnLikes.setOnClickListener {
                onInteractionListener.onLike(post)
            }
            ibtnShare.setOnClickListener {
                onInteractionListener.onShare(post)
            }

            videoLinkPic.setOnClickListener() {
                onInteractionListener.onVideoLinkClick(post)
            }

            messageContent.setOnClickListener() {
                onInteractionListener.onViewSingle(post)
            }

            ibtnMenuMoreActions.isVisible = post.ownedByMe
            // Пока выбор меню обработаем в любом случае, а не только для ownedByMe
            // Может, позже вставим условие if (post.ownedByMe)
            ibtnMenuMoreActions.setOnClickListener {
                PopupMenu(it.context, it).apply {
                    inflate(R.menu.options_post)
                    setOnMenuItemClickListener { item ->
                        when (item.itemId) {
                            R.id.remove -> {
                                onInteractionListener.onRemove(post)
                                true
                            }

                            R.id.edit -> {
                                onInteractionListener.onEdit(post)
                                true
                            }

                            else -> false
                        }
                    }
                }.show()
            }

            // И после всех привязок начинаем, наконец, грузить картинку
            //val url = "${BASE_URL}/avatars/${post.avatarFileName()}"
            val url = "${BuildConfig.BASE_URL}/avatars/${post.authorAvatar}"
            Glide.with(binding.imgAvatar).load(url).circleCrop()
                .placeholder(R.drawable.ic_loading_100dp).error(R.drawable.ic_error_100dp)
                .timeout(10_000).into(binding.imgAvatar)

//            Picasso.get()
//                .load(url)
//                .error(R.drawable.ic_error_100dp)
//                .into(binding.image);

        }
    }
}



