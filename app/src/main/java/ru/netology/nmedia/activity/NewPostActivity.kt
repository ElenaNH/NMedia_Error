package ru.netology.nmedia.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContract
import androidx.appcompat.app.AppCompatActivity
import ru.netology.nmedia.databinding.ActivityNewPostBinding

class NewPostActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityNewPostBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.editContent.requestFocus()
        intent?.let {
            binding.editContent.setText(it.getStringExtra(Intent.EXTRA_TEXT))
        }
        binding.btnOk.setOnClickListener {
            val intent = Intent()
            if (binding.editContent.text.isNullOrBlank()) {
                setResult(
                    Activity.RESULT_CANCELED,
                    intent
                ) // Если результата нет, то им будет НЕизмененный intent (новый)
            } else {
                val postContent = binding.editContent.text.toString()
                intent.putExtra(Intent.EXTRA_TEXT, postContent)
                setResult(
                    Activity.RESULT_OK,
                    intent
                ) // Это результат текущей активити - ИЗМЕНЕННЫЙ интент
            }
            finish()
        }
    }

    // Cоздаем контракт - это синглтон

    object NewPostContract : ActivityResultContract<Unit, String?>() {
        // переопределим создание интента в контракте, передав в него контекст и нужную нам активить нового поста
        override fun createIntent(context: Context, input: Unit) =
            Intent(context, NewPostActivity::class.java)

        // переопределим получение результата работы запущенной активити
        override fun parseResult(resultCode: Int, intent: Intent?) =
            intent?.getStringExtra(Intent.EXTRA_TEXT)

    }

}
