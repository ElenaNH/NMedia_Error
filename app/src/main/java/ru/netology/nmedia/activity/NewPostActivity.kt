package ru.netology.nmedia.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContract
import androidx.appcompat.app.AppCompatActivity
import ru.netology.nmedia.R
import ru.netology.nmedia.databinding.ActivityNewPostBinding

class NewPostActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityNewPostBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.editContent.requestFocus()
        intent?.let {
            val listOf2 = it.getStringArrayListExtra("LIST_OF_2")
            if (listOf2?.size ?: 0 == 2) {
                binding.editContent.setText(listOf2?.get(0).toString())
                binding.editVideoLink.setText(listOf2?.get(1).toString())
            }
        }

        binding.btnOk.setOnClickListener {

            if ((binding.editContent.text.isNullOrBlank()) and (binding.editVideoLink.text.isNullOrBlank())) {

                // Предупреждение о непустом содержимом
                val warnToast = Toast.makeText(
                    this@NewPostActivity,
                    getString(R.string.error_empty_content),
                    Toast.LENGTH_SHORT
                )
                warnToast.setGravity(Gravity.CENTER_VERTICAL, 0, 0)
                warnToast.show()
                return@setOnClickListener

            } else {
                val intent = Intent()

                val postContent = binding.editContent.text.toString().trim()
                val videoLinkContent = binding.editVideoLink.text.toString().trim()
                var listOf2 = ArrayList<String>()
                listOf2.add(postContent)
                listOf2.add(videoLinkContent)
                intent.putExtra("LIST_OF_2", listOf2)
                setResult(
                    Activity.RESULT_OK,
                    intent
                ) // Это результат работы текущей активити - ИЗМЕНЕННЫЙ интент
            }
            finish()
        }
    }

    // Cоздаем контракт - это синглтон, т.к. достаточно единственного экземпляра

    object NewPostContract : ActivityResultContract<ArrayList<String>?, ArrayList<String>?>() {
        // Первый тип-параметр - это тип входных данных для передачи в вызываемую активить
        // Второй - это тип возвращаемых данных
        // На уроке не было входных данных, а только возвращаемые:  object NewPostContract : ActivityResultContract<Unit, String?>()

        // переопределим создание интента в контракте, передав в него контекст и нужную нам активить нового поста
        // второй параметр input должен быть того самого типа, что и первый тип-параметр синглтона (или класса, если будет класс)
        // на уроке мы ничего не передавали в новую активить, поэтому не требовался вызов putExtra: override fun createIntent(context: Context, input: Unit) = Intent(context, NewPostActivity::class.java)
        override fun createIntent(context: Context, input: ArrayList<String>?): Intent {
            val intent = Intent(context, NewPostActivity::class.java)
            if (input?.size ?: 0 == 2) {
                intent.putExtra(
                    "LIST_OF_2",
                    input
                )    // В данном случае input имеет тип String?, поэтому используем константу EXTRA_TEXT
            }
            return intent
        }

        // переопределим получение результата работы запущенной активити
        // ф-ция возвращает данные такого же типа, что и второй тип-параметр синглтона (или класса), поэтому использовали getStringExtra в данном случае
        override fun parseResult(resultCode: Int, intent: Intent?) =
            intent?.getStringArrayListExtra("LIST_OF_2")
    }

}
