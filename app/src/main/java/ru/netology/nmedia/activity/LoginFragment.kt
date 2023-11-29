package ru.netology.nmedia.activity

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import ru.netology.nmedia.R
import ru.netology.nmedia.auth.LoginInfo
import ru.netology.nmedia.databinding.FragmentLoginBinding
import ru.netology.nmedia.util.ConsolePrinter
import ru.netology.nmedia.viewmodel.LoginViewModel


class LoginFragment : Fragment() {

    val viewModel by viewModels<LoginViewModel>()
    private lateinit var binding: FragmentLoginBinding

    // Создано по образцу FeedFragment
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding = FragmentLoginBinding.inflate(layoutInflater, container, false)

        subscribe()     // все подписки, которые могут нам потребоваться в данном фрагменте
        setListeners()  // все лиснеры всех элементов данном фрагменте

        return binding.root
    }

    private fun setListeners() {
        binding.signIn.setOnClickListener {

            // Эта функция тут временно - пока не обрабатываются события клавиатуры
            viewModel.resetLoginInfo(
                LoginInfo(
                    binding.login.text.toString(),
                    binding.password.text.toString()
                )
            )
            // Эта функция тут так и останется
            if (viewModel.completed()) {
                // Делаем попытку залогиниться
                // TODO - отделить отсутствие ответа сервера от отказа в доступе
                if (!viewModel.doLogin())
                    showToast("Cannot login!")

            } else {
                // Предупреждаем или молчим??
                // Пока повторим предупреждение - TODO ПЕРЕДЕЛАТЬ НОРМАЛЬНО
                showToast("Login info is not completed!")
            }
        }

        // Как сделать обработчики ввода текста в каждое поле, чтобы проверять, когда логин/пароль непусты?

    }

    private fun subscribe() {

        /*viewModel.loginInfo.observe(viewLifecycleOwner) {
            if (viewModel.completed()) {
                // Делаем попытку залогиниться
                viewModel.doLogin()

            } else {
                // Предупреждаем или молчим??

            }
        }*/

        viewModel.loginSuccess.observe(viewLifecycleOwner) {
            // Закрытие текущего фрагмента (переход к нижележащему в стеке)
            findNavController().navigateUp()
            ConsolePrinter.printText("LoginFragment was leaved")
        }

        viewModel.completionWarningSet.observe(viewLifecycleOwner) {warnings ->
            if (warnings.count() == 0) return@observe

            // Предупреждение об ошибке комплектования данных (нет логина либо пароля либо ...)
            showToast(warnings.map { getString(it) }.joinToString("; "))

        }

    }

    private fun showToast(textInformation: String) {
        val warnToast = Toast.makeText(
            activity,
            textInformation,
            Toast.LENGTH_SHORT
        )
        warnToast.setGravity(Gravity.CENTER_VERTICAL, 0, 0)
        warnToast.show()
    }

}
