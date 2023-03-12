package ru.netology.nmedia.activity

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import ru.netology.nmedia.R
import ru.netology.nmedia.activity.NewPostFragment.Companion.textArg

class AppActivity : AppCompatActivity(R.layout.activity_app) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        intent?.let {
            if (it.action != Intent.ACTION_SEND) {
                return@let
            }

            val text = it.getStringExtra(Intent.EXTRA_TEXT) // Проверяем, что передано извне (чем с нами "поделились")
            if (text?.isNotBlank() != true) {    // Можно ли isNullOrBlank()???
                return@let  // Если никто ничего нам не передал, то выходим
            }
            intent.removeExtra(Intent.EXTRA_TEXT)   // Удаляем то, что было передано извне
            // Поскольку имеем данные извне, то запустим переход ко второму фрагменту newPostFragment
            findNavController(R.id.nav_host_fragment).navigate(
                R.id.action_feedFragment_to_newPostFragment,
                Bundle().apply {
                    textArg = text  // В запускаемый фрагмент передаем полученные извне данные
                }
            )
        }
    }
}
