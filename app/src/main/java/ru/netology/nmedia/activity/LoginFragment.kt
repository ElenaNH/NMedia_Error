package ru.netology.nmedia.activity

import android.app.ProgressDialog
import android.os.Bundle
import android.util.Log
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
import ru.netology.nmedia.util.AndroidUtils
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

        // TODO Разблокируем кнопку (это действие позже удалим, когда сделаем анализ ввода с клавиатуры)
        binding.signIn.isEnabled = true

        subscribe()     // все подписки, которые могут нам потребоваться в данном фрагменте
        setListeners()  // все лиснеры всех элементов данном фрагменте

        return binding.root
    }

    private fun setListeners() {
        val tmpModel = viewModel

        binding.signIn.setOnClickListener {

            AndroidUtils.hideKeyboard(requireView())  // Скрыть клавиатуру

            // Эта функция тут временно - пока не обрабатываются события клавиатуры
            viewModel.resetLoginInfo(
                LoginInfo(
                    binding.login.text.toString(),
                    binding.password.text.toString()
                )
            )

            // Попытка логина тут так и останется

            if (viewModel.completed()) {
                // Блокируем кнопку, чтобы дважды не нажимать
                binding.signIn.isEnabled = false
                // Делаем попытку залогиниться
                viewModel.doLogin()
            } // else {} // НЕ будем уведомлять. Уже должно было появиться уведомление после resetLoginInfo
        }

        //TODO Как сделать обработчики ввода текста в поля, чтобы проверять, когда логин/пароль непусты?

    }

    private fun subscribe() {

        /*viewModel.loginInfo.observe(viewLifecycleOwner) {

        }*/

        viewModel.loginSuccessEvent.observe(viewLifecycleOwner) {
            // TODO - если сделать реакцию на ввод с клавиатуры, то кнопка будет не здесь включаться
            //binding.signIn.isEnabled = true // Теперь можем снова нажимать кнопку

            AndroidUtils.hideKeyboard(requireView())  // Скрыть клавиатуру (если вдруг она опять открылась)

            // Закрытие текущего фрагмента (переход к нижележащему в стеке)
            findNavController().navigateUp()
            ConsolePrinter.printText("LoginFragment was leaved")
        }

        viewModel.completionWarningSet.observe(viewLifecycleOwner) { warnings ->
            if (warnings.count() == 0) return@observe

            // Предупреждение об ошибке комплектования данных (нет логина либо пароля либо ...)
            val msg = warnings.map { getString(it) }.joinToString("; ")
            showToast(msg)

            Log.d("LoginFragment", "Login info: ${viewModel.loginInfo}")

        }

        viewModel.loginError.observe(viewLifecycleOwner) { errText ->
            if (errText == null) return@observe
            //if (errText == "") return@observe

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
