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

/**
 * Bonus - Phase 3.7 : Notifications locales
 * Vérifie les fenêtres de communication imminentes (dans 15 min).
 */
class FenetreWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val now = LocalDateTime.now()
        
        // Simule la récupération des fenêtres planifiées (Normalement via Room DAO)
        val fenetresImminentes = MockData.fenetres.filter { 
            it.statut == StatutFenetre.PLANIFIEE && 
            ChronoUnit.MINUTES.between(now, it.datetimeDebut) in 0..15 
        }

        fenetresImminentes.forEach { fenetre ->
            sendNotification(fenetre)
        }

        return Result.success()
    }

    private fun sendNotification(fenetre: FenetreCom) {
        val channelId = "nanoorbit_passages"
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Passages Satellites",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alertes pour les fenêtres de communication"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Passage imminent : ${fenetre.idSatellite}")
            .setContentText("Station ${fenetre.codeStation} - Début dans quelques minutes")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(fenetre.idFenetre, notification)
    }
}
