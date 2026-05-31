package com.chehartemple.app.data.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.*
import com.chehartemple.app.R
import com.chehartemple.app.data.api.RetrofitClient
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

class EventNotificationWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        createChannel()
        try {
            val events = RetrofitClient.api.getEvents()
            val today = LocalDate.now()
            val tomorrow = today.plusDays(1)

            events.forEach { event ->
                val eventDate = event.eventDate?.let {
                    try { LocalDate.parse(it, DateTimeFormatter.ISO_LOCAL_DATE) } catch (_: Exception) { null }
                } ?: return@forEach

                when (eventDate) {
                    today -> showNotification(
                        "🙏 Today's Event",
                        "${event.title} is happening today!${if (!event.startTime.isNullOrEmpty()) " at ${event.startTime}" else ""}",
                        event.id.toInt()
                    )
                    tomorrow -> showNotification(
                        "📅 Tomorrow's Event",
                        "${event.title} is tomorrow!${if (!event.startTime.isNullOrEmpty()) " at ${event.startTime}" else ""}",
                        event.id.toInt() + 10000
                    )
                }
            }
        } catch (_: Exception) {}
        return Result.success()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel("events", "Event Reminders", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Notifications for upcoming temple events"
            }
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    private fun showNotification(title: String, body: String, id: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return

        val notification = NotificationCompat.Builder(context, "events")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(id, notification)
    }

    companion object {
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<EventNotificationWorker>(12, TimeUnit.HOURS)
                .setInitialDelay(1, TimeUnit.MINUTES)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "event_notifications", ExistingPeriodicWorkPolicy.KEEP, request
            )
        }
    }
}
