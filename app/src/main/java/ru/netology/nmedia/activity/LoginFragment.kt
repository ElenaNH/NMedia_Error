package ru.netology.nmedia.activity

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import ru.netology.nmedia.auth.LoginInfo
import ru.netology.nmedia.databinding.FragmentLoginBinding
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
            viewModel.resetLoginInfo(LoginInfo(binding.login.text.toString(), binding.password.text.toString()))
        }

    // Как сделать обработчики ввода текста в каждое поле, чтобы проверять, когда логин/пароль непусты?

    }

    private fun subscribe() {

        viewModel.loginInfo.observe(viewLifecycleOwner) {
            if (viewModel.completed()) {
                // Делаем попытку залогиниться
                viewModel.doLogin()

            } else {
                // Предупреждаем или молчим??

            }
        }

    }

}
