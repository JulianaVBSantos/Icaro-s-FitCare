package br.com.icaros.fitcare

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import br.com.icaros.fitcare.data.FcmTokenManager
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class FitCareMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        FcmTokenManager.atualizarNovoToken(applicationContext, token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val titulo = message.notification?.title
            ?: message.data["title"]
            ?: "Ícaros FitCare"

        val corpo = message.notification?.body
            ?: message.data["body"]
            ?: "Você recebeu uma nova atualização."

        exibirNotificacao(
            context = applicationContext,
            titulo = titulo,
            mensagem = corpo,
            notificacaoId = (System.currentTimeMillis() % Int.MAX_VALUE).toInt()
        )
    }

    companion object {
        private const val CANAL_ID = "fitcare_notificacoes"
        private const val CANAL_NOME = "Notificações Ícaros FitCare"

        fun criarCanalNotificacao(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val canal = NotificationChannel(
                    CANAL_ID,
                    CANAL_NOME,
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Alertas de anamneses atribuídas e respondidas."
                }
                val manager =
                    context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                manager.createNotificationChannel(canal)
            }
        }

        fun exibirNotificacao(
            context: Context,
            titulo: String,
            mensagem: String,
            notificacaoId: Int
        ) {
            criarCanalNotificacao(context)

            if (
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }

            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                notificacaoId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val builder = NotificationCompat.Builder(context, CANAL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(titulo)
                .setContentText(mensagem)
                .setStyle(NotificationCompat.BigTextStyle().bigText(mensagem))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)

            NotificationManagerCompat.from(context).notify(notificacaoId, builder.build())
        }
    }
}