package ru.netology.nmedia.activity

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.MenuProvider
//import androidx.fragment.app.activityViewModels
//import androidx.navigation.fragment.findNavController
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.messaging.FirebaseMessaging
import ru.netology.nmedia.R
import ru.netology.nmedia.activity.NewPostFragment.Companion.textArg
import ru.netology.nmedia.auth.AppAuth
import ru.netology.nmedia.dto.Token
import ru.netology.nmedia.uiview.goToLogin
import ru.netology.nmedia.uiview.getCurrentFragment
import ru.netology.nmedia.util.AndroidUtils
import ru.netology.nmedia.viewmodel.AuthViewModel
import ru.netology.nmedia.viewmodel.PostViewModel

class AppActivity : AppCompatActivity(R.layout.activity_app) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        intent?.let {
            if (it.action != Intent.ACTION_SEND) {
                return@let
            }

            val text =
                it.getStringExtra(Intent.EXTRA_TEXT) // Проверяем, что передано извне (чем с нами "поделились")
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
        checkGoogleApiAvailability()

        val viewModel by viewModels<AuthViewModel>()

        var oldMenuProvider: MenuProvider? = null
        viewModel.data.observe(this) {
            oldMenuProvider?.let(::removeMenuProvider) // Удаляем старые меню, если они были

            addMenuProvider(object : MenuProvider {
                override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                    menuInflater.inflate(R.menu.menu_auth, menu)
                    val authorized = viewModel.isAuthorized
                    menu.setGroupVisible(R.id.authorized, authorized)
                    menu.setGroupVisible(R.id.unauthorized, !authorized)
                }

                override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                    // Определяем текущий отображаемый фрагмент
                    val currentFragment = supportFragmentManager.getCurrentFragment()

                    // Обработка выбора меню и возврат true для обработанных
                    return when (menuItem.itemId) {
                        R.id.auth, R.id.register -> {
                            if (currentFragment != null) {
                                goToLogin(currentFragment)
                            } else {
                                val stop1 = 1 // мы тут не должны оказаться по идее
                            }
                            // Fix in HomeWork - прогрузку тестового токена заменить на авторизацию
                            //AppAuth.getInstance().setToken(Token(5L, "x-token"))
                            true
                        }

                        R.id.logout -> {
                            if (currentFragment != null) {
                                AndroidUtils.hideKeyboard(currentFragment.requireView())  // Скрыть клавиатуру

                                // Подтверждение логофа //LENGTH_LONG?? //it.rootView??
                                currentFragment.view?.let {
                                    Snackbar.make(
                                        it.rootView,
                                        R.string.logout_confirm_request,
                                        Snackbar.LENGTH_INDEFINITE
                                    )
                                        .setAction(R.string.action_cancel) { val cancelling = true }
                                        .setAction(R.string.action_continue) {
                                            // Логоф
                                            AppAuth.getInstance().clearAuth()
                                            // Уходим из режима редактирования в режим чтения
                                            if (currentFragment is NewPostFragment) {
                                                // в контроллер нужно передать аргумент, т.к. мы "над" фрагментами, в активити
                                                //findNavController(R.id.newPostFragment).navigateUp()
                                                val navHostFragment =
                                                    supportFragmentManager.findFragmentById(
                                                        R.id.nav_host_fragment
                                                    ) as NavHostFragment
                                                navHostFragment.navController.navigateUp()
                                            }
                                        }
                                        .setTextMaxLines(4)
                                        .show()
                                } ?: {
                                    val stop2 = 2 // мы тут не должны оказаться по идее
                                }
                            }
                            // Возвращаем true как признак обработки
                            true
                        }

                        else -> {
                            false
                        }
                    }
                }

            }.apply {
                oldMenuProvider = this
            }, this)
        }
    }

    private fun checkGoogleApiAvailability() {
        with(GoogleApiAvailability.getInstance()) {
            val code = isGooglePlayServicesAvailable(this@AppActivity)
            if (code == ConnectionResult.SUCCESS) {
                return@with
            }
            if (isUserResolvableError(code)) {
                getErrorDialog(this@AppActivity, code, 9000)?.show()
                return
            }
            Toast.makeText(this@AppActivity, R.string.google_play_unavailable, Toast.LENGTH_LONG)
                .show()
        }

        FirebaseMessaging.getInstance().token.addOnSuccessListener {
            println(it)
        }
    }

}
