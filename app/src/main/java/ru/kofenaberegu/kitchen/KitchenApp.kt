package ru.kofenaberegu.kitchen

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.work.*
import ru.kofenaberegu.kitchen.data.local.KitchenDatabase
import ru.kofenaberegu.kitchen.worker.ReminderScheduler
import ru.kofenaberegu.kitchen.worker.TelegramSyncWorker
import java.util.concurrent.TimeUnit

class KitchenApp : Application() {
    val database: KitchenDatabase by lazy { KitchenDatabase.getInstance(this) }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        scheduleBackgroundSync()
        ReminderScheduler.scheduleAllDefaultReminders(this)
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            val channel = NotificationChannel(
                CHANNEL_REMINDERS,
                "Напоминания кухни и эскалация",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Уведомления о приёме/закрытии смены, списаниях, маркировке и контроле"
            }
            manager.createNotificationChannel(channel)
        }
    }

    private fun scheduleBackgroundSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = PeriodicWorkRequestBuilder<TelegramSyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "telegram_queue_sync",
            ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )
    }

    companion object {
        const val CHANNEL_REMINDERS = "kitchen_reminders_channel"
    }
}
