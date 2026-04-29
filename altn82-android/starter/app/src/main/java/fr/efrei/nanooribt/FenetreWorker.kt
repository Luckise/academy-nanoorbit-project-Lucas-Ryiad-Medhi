package fr.efrei.nanooribt

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class FenetreWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val now = LocalDateTime.now()
        val db = AppDatabase.getDatabase(applicationContext)
        val fenetreEntities = db.fenetreDao().getAllFenetresList()

        val fenetresImminentes = fenetreEntities.filter { entity ->
            entity.statut == "PLANIFIEE" && entity.datetimeDebut != null &&
            try {
                val dt = LocalDateTime.parse(entity.datetimeDebut)
                ChronoUnit.MINUTES.between(now, dt) in 0..15
            } catch (_: Exception) { false }
        }

        fenetresImminentes.forEach { fenetre ->
            sendNotification(fenetre)
        }

        return Result.success()
    }

    private fun sendNotification(fenetre: FenetreEntity) {
        val channelId = "nanoorbit_passages"
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Passages Satellites",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alertes pour les fenetres de communication"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Passage imminent : ${fenetre.idSatellite}")
            .setContentText("Station ${fenetre.codeStation} - Debut dans quelques minutes")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(fenetre.idFenetre, notification)
    }
}
