package ru.netology.nmedia.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import ru.netology.nmedia.R
import ru.netology.nmedia.auth.AppAuth
import kotlin.random.Random
import ru.netology.nmedia.util.ConsolePrinter
import javax.inject.Inject

@AndroidEntryPoint
class FCMService : FirebaseMessagingService() {
    private val action = "action"
    private val content = "content"
    private val channelId = "remote"
    private val gson = Gson()

    @Inject
    lateinit var appAuth: AppAuth

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.channel_remote_name)
            val descriptionText = getString(R.string.channel_remote_description)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {

//        Log.d("message received : ", "${message.data.keys}")
        ConsolePrinter.printText("message received : ")

        try {
            message.data[content]?.let {
                val simpleInfo = gson.fromJson(
                    message.data[content],
                    SimpleInfo::class.java
                )
                val userId = appAuth.data.value?.id
                ConsolePrinter.printText("userId=")
                when {
                    (simpleInfo.recipientId == null) -> handleSimple(simpleInfo)
                    (simpleInfo.recipientId == userId) -> handleSimple(simpleInfo)
                    else -> {
                        // Отправляем заново push-токен
                        appAuth.sendPushToken()
                    }
                }
            }

            /*  message.data[action]?.let {
               if (!Action.values().map { elem -> elem.toString() }.contains(it)) return@let
               when (Action.valueOf(it)) {
                   Action.LIKE -> handleLike(gson.fromJson(message.data[content], Like::class.java))
                   Action.NEW_POST -> handleNewPost(
                       gson.fromJson(
                           message.data[content],
                           NewPostInfo::class.java
                       )
                   )
               }
           }*/

            val myStop = 1  // Просто для точки останова

        } catch (e: Exception) {
            //        Log.d("message treating error : ")
            ConsolePrinter.printText("message treating error : ")
        }
    }

    override fun onNewToken(token: String) {
        // Печатает токен в консоль, но возможно сохранять в файл или базу и т.п.
        ConsolePrinter.printText("push-token=$token")
        appAuth.sendPushToken(token) // отправка на сервер
    }

    private fun handleSimple(simpleInfo: SimpleInfo) {
        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(
                getString(R.string.notification_info).plus(simpleInfo.content)
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        notifyWith(notification)

    }

    private fun handleLike(content: Like) {
        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(
                getString(
                    R.string.notification_user_liked,
                    content.userName,
                    content.postAuthor,
                )
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        notifyWith(notification)

    }

    private fun handleNewPost(postInfo: NewPostInfo) {
        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(
                getString(
                    R.string.notification_new_post,
                    postInfo.userName,
                )
            )
            .setContentText(postInfo.content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(postInfo.content))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        //.setLargeIcon(R.drawable.ic_launcher_foreground2)

        notifyWith(notification)

    }
    /* .setContentText(emailObject.getSubject())
    .setLargeIcon(emailObject.getSenderAvatar())
    .setStyle(NotificationCompat.BigTextStyle().bigText(emailObject.getSubjectAndSnippet())) */


    private fun notifyWith(notification: Notification) {

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        NotificationManagerCompat.from(this)
            .notify(Random.nextInt(100_000), notification)

    }
}


enum class Action {
    LIKE,
    NEW_POST,
}

data class Like(
    val userId: Long,
    val userName: String,
    val postId: Long,
    val postAuthor: String,
)

data class NewPostInfo(
    val userId: Long,
    val userName: String,
    val postId: Long,
    val content: String
)

data class SimpleInfo(
    val recipientId: Long?,
    val content: String,
)

