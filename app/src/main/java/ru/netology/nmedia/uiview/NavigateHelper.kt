package ru.netology.nmedia.uiview

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.navigation.fragment.findNavController
import ru.netology.nmedia.R
import ru.netology.nmedia.activity.FeedFragment
import ru.netology.nmedia.activity.ImageFragment
import ru.netology.nmedia.activity.PostFragment

fun goToLogin(startFragment: Fragment) {
    // Поскольку вынесли функцию в другой файл, то нужен аргумент, а иначе можно без аргумента
    // Нам нужно знать, в каком мы фрагменте, чтобы задать правильный переход
    val action_from_to =
        when {
            (startFragment is FeedFragment) -> R.id.action_feedFragment_to_loginFragment
            (startFragment is PostFragment) -> R.id.action_postFragment_to_loginFragment
            (startFragment is ImageFragment) -> R.id.action_imageFragment_to_loginFragment
            else -> null
        }

    // ЗАПУСК ЛОГИНА
    if (action_from_to != null)
        startFragment.findNavController().navigate(
            action_from_to
        ) // Когда тот фрагмент закроется, опять окажемся здесь (по стеку)
}

fun FragmentManager.getCurrentFragment(): Fragment? {
    return this
        .findFragmentById(R.id.nav_host_fragment)
        ?.childFragmentManager
        ?.primaryNavigationFragment
}


