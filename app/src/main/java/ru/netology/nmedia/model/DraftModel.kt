package ru.netology.nmedia.model

import android.net.Uri
import ru.netology.nmedia.dto.Post

data class DraftModel(
    val post: Post,
    val photo: PhotoModel? = null,
)
