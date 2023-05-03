package ru.netology.nmedia.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.ListAdapter
import ru.netology.nmedia.R
import ru.netology.nmedia.databinding.CardPostBinding
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.dto.statisticsToString   // при этом dto.Post импортируется через PostViewModel и связанный с ней Repository

interface OnInteractionListener {
    fun onLike(post: Post) {}
    fun onShare(post: Post) {}
    fun onEdit(post: Post) {}
    fun onRemove(post: Post) {}
    fun onVideoLinkClick(post: Post) {}
    fun onViewSingle(post: Post) {}
}


class PostsAdapter(private val onInteractionListener: OnInteractionListener) :
    ListAdapter<Post, PostViewHolder>(PostDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val binding = CardPostBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PostViewHolder(
            binding,
            onInteractionListener
        )
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = getItem(position)
        holder.bind(post)
    }

}

class PostViewHolder(
    private val binding: CardPostBinding,
    private val onInteractionListener: OnInteractionListener
) : RecyclerView.ViewHolder(binding.root) {
    fun bind(post: Post) {
        binding.apply {
            if (post.id == 8L) {
                val myPoint = 1
            }
            messageAuthor.text = post.author
            messagePublished.text = post.published
            messageContent.text = post.content
            if ((post.videoLink ?: "").trim() == "") videoLinkPic.setImageDrawable(null)
            else videoLinkPic.setImageResource(R.mipmap.ic_banner_foreground)
            // Для MaterialButton (но не для Button)
            ibtnLikes.isChecked = post.likedByMe
            ibtnLikes.text =
                post.likes.toLong().statisticsToString() // Число лайков прямо на кнопке
            ibtnShare.text = post.countShare.toLong().statisticsToString()
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
        }
    }
}


/*// Сначала проверим наличие ссылки внутри поста (возьмем первую подходящую)
            val regex = "(https?://)?([\\w-]{1,32})(\\.[\\w-]{1,32})+[^\\s@]*".toRegex()
            val match = regex.find(post.content)
            // Если ссылка есть в тексте, то поместим ее в отдельное поле
            // Если нет ссылки, то поле ссылки будет пустым
            if (match?.value == null) videoLinkPic.setImageDrawable(null)
            else videoLinkPic.setImageResource(R.mipmap.ic_banner_foreground)*/
