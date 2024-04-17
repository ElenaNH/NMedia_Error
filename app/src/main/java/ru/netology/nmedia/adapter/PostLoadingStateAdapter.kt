package ru.netology.nmedia.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.paging.LoadState
import androidx.paging.LoadStateAdapter
import androidx.recyclerview.widget.RecyclerView
import ru.netology.nmedia.databinding.ItemLoadingBinding

class PostLoadingStateAdapter(
    private val retryListener: () -> Unit,
) : LoadStateAdapter<PostLoadingViewHolder>() {
    override fun onBindViewHolder(holder: PostLoadingViewHolder, loadState: LoadState) {
        // Заполняем наш PostLoadingViewHolder данными, пришедшими в виде loadState
        holder.bind(loadState)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        loadState: LoadState
    ): PostLoadingViewHolder = PostLoadingViewHolder(
        ItemLoadingBinding.inflate(LayoutInflater.from(parent.context), parent, false),
        retryListener,
    )

}


class PostLoadingViewHolder(
    private val itemLoadingBinding: ItemLoadingBinding,
    private val retryListener: () -> Unit,
) : RecyclerView.ViewHolder(itemLoadingBinding.root) {

    fun bind(loadState: LoadState) {
        // Показываем при загрузке
        itemLoadingBinding.apply {
            progress.isVisible = loadState is LoadState.Loading
            //retryButton.isVisible = loadState is LoadState.Error   // Кнопка видна при ошибке
            retryButton.isVisible = true    // Кнопка видна всегда в хедере/футере
            // TODO - пока кнопка видна только в футере! Надо менять обработку PREPEND!!!

            // Также для кнопки повтора необходим обработчик нажатия
            // (который мы передадим через конструктор класса PostLoadingViewHolder)
            retryButton.setOnClickListener {
                retryListener()
            }
        }

    }
}
