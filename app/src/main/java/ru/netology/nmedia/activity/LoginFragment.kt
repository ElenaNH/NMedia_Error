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
                // Блокируем кнопку, чтобы дважды не нажимать
                binding.signIn.isEnabled = false
                // Делаем попытку залогиниться
                viewModel.doLogin()

            } else {
                // Пока повторим предупреждение - TODO ПЕРЕДЕЛАТЬ НОРМАЛЬНО

                val warns = viewModel.completionWarningSet.value
                val errText = warns?.map { getString(it) }
                    ?.joinToString("; ") ?: "Login info is not completed!"
                showToast(errText)

            }
        }

        //TODO Как сделать обработчики ввода текста в поля, чтобы проверять, когда логин/пароль непусты?

    }

    private fun subscribe() {

        /*viewModel.loginInfo.observe(viewLifecycleOwner) {

        }*/

        viewModel.loginSuccessEvent.observe(viewLifecycleOwner) {
            // TODO - если сделать реакцию на ввод с клавиатуры, то кнопка будет не здесь включаться
            binding.signIn.isEnabled = true // Теперь можем снова нажимать кнопку

            // Закрытие текущего фрагмента (переход к нижележащему в стеке)
            findNavController().navigateUp()
            ConsolePrinter.printText("LoginFragment was leaved")
        }

        viewModel.completionWarningSet.observe(viewLifecycleOwner) { warnings ->
            if (warnings.count() == 0) return@observe

            // Предупреждение об ошибке комплектования данных (нет логина либо пароля либо ...)
            showToast(warnings.map { getString(it) }.joinToString("; "))

        }

        viewModel.loginError.observe(viewLifecycleOwner) { errText ->
            if (errText == null) return@observe

            // Сообщение об ошибке логина
            showToast(errText)

            binding.signIn.isEnabled = true // Теперь можем снова нажимать кнопку
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
