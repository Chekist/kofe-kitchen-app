package ru.kofenaberegu.kitchen.worker

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ru.kofenaberegu.kitchen.KitchenApp
import ru.kofenaberegu.kitchen.MainActivity
import ru.kofenaberegu.kitchen.data.local.KitchenDatabase
import ru.kofenaberegu.kitchen.data.remote.TelegramSender
import java.util.Calendar
import java.util.concurrent.TimeUnit

class TelegramSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val dao = KitchenDatabase.getInstance(applicationContext).kitchenDao()
        val pending = dao.getPendingQueue()
        if (pending.isEmpty()) return Result.success()

        var allSent = true
        for (item in pending) {
            val photos = item.photoPathsJoined.split("|").filter { it.isNotBlank() }
            val success = TelegramSender.sendViaBotApi(applicationContext, item.text, photos)
            if (success) {
                dao.removeFromQueue(item)
                dao.updateReportStatus(item.reportId, "SENT")
            } else {
                allSent = false
            }
        }
        return if (allSent) Result.success() else Result.retry()
    }
}

class EventReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val title = inputData.getString("title") ?: "Напоминание кухни"
        val body = inputData.getString("body") ?: "Проверьте регламент"
        val escalate = inputData.getBoolean("escalate", false)

        KitchenAlarmReceiver.showNotification(applicationContext, title, body)
        if (escalate) {
            TelegramSender.sendViaBotApi(
                applicationContext,
                "ЭСКАЛАЦИЯ (00:30): Форма закрытия кухни не отправлена вовремя!",
                emptyList()
            )
        }
        return Result.success()
    }
}

class KitchenAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra("title") ?: "Регламент кухни"
        val body = intent.getStringExtra("body") ?: "Пора выполнить проверку"
        val escalate = intent.getBooleanExtra("escalate", false)

        showNotification(context, title, body)

        if (escalate) {
            CoroutineScope(Dispatchers.IO).launch {
                TelegramSender.sendViaBotApi(
                    context,
                    "ЭСКАЛАЦИЯ АДМИНИСТРАТОРУ (00:30): Отчёт о закрытии кухни не отправлен!",
                    emptyList()
                )
            }
        }
    }

    companion object {
        fun showNotification(context: Context, title: String, body: String) {
            val openIntent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                context, title.hashCode(), openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val notification = NotificationCompat.Builder(context, KitchenApp.CHANNEL_REMINDERS)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(body)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()

            try {
                NotificationManagerCompat.from(context).notify(title.hashCode(), notification)
            } catch (_: SecurityException) {
            }
        }
    }
}

object ReminderScheduler {
    fun scheduleAllDefaultReminders(context: Context) {
        scheduleDailyAlarm(context, 101, 15, 25, "Приём смены через 5 минут", "15:30 — сверка отчёта и проверка зон кухни")
        scheduleDailyAlarm(context, 102, 23, 55, "Подготовка к закрытию (23:55)", "Начните 7 шагов закрытия кухни по регламенту")
        scheduleDailyAlarm(context, 103, 0, 0, "00:00 — Закрытие кухни", "Откройте мастер закрытия и заполните чек-лист")
        scheduleDailyAlarm(context, 104, 0, 30, "Просрочено закрытие кухни (00:30)", "Отчёт не отправлен! Уведомление передано администратору", escalate = true)

        val periodicCheck = PeriodicWorkRequestBuilder<EventReminderWorker>(12, TimeUnit.HOURS)
            .setInputData(
                workDataOf(
                    "title" to "Контроль регламента (Пн/Ср/Пт 15:20, масло фритюра раз в 3 дня, ген. уборка)",
                    "body" to "Проверьте актуальные задачи в приложении «Кофе на Берегу»"
                )
            )
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "kitchen_periodic_rules",
            ExistingPeriodicWorkPolicy.KEEP,
            periodicCheck
        )
    }

    fun scheduleEventTrigger(context: Context, uniqueName: String, delayMinutes: Long, title: String, body: String) {
        val request = OneTimeWorkRequestBuilder<EventReminderWorker>()
            .setInitialDelay(delayMinutes, TimeUnit.MINUTES)
            .setInputData(workDataOf("title" to title, "body" to body))
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(uniqueName, ExistingWorkPolicy.REPLACE, request)
    }

    fun cancelEventTrigger(context: Context, uniqueName: String) {
        WorkManager.getInstance(context).cancelUniqueWork(uniqueName)
    }

    private fun scheduleDailyAlarm(
        context: Context,
        requestCode: Int,
        hour: Int,
        minute: Int,
        title: String,
        body: String,
        escalate: Boolean = false
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, KitchenAlarmReceiver::class.java).apply {
            putExtra("title", title)
            putExtra("body", body)
            putExtra("escalate", escalate)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            if (before(Calendar.getInstance())) add(Calendar.DAY_OF_YEAR, 1)
        }
        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
    }
}
