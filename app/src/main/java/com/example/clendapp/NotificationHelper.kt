package com.example.clendapp

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.clendapp.data.Tasks

object NotificationHelper {
    const val CHANNEL_ID = "task_reminders"
    private const val NOTIFICATION_NAME = "Task Reminders"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, NOTIFICATION_NAME, importance).apply {
                description = "Notifications for upcoming tasks"
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun scheduleTaskNotification(context: Context, task: Tasks) {
        if (task.isCompleted) return

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val currentTime = System.currentTimeMillis()

        // 1. Schedule for 10 minutes before dueDate
        val triggerTimeSoon = task.dueDate - (10 * 60 * 1000)
        if (triggerTimeSoon > currentTime) {
            val intentSoon = Intent(context, TaskNotificationReceiver::class.java).apply {
                putExtra("TASK_ID", task.id)
                putExtra("TASK_TITLE", task.title)
                putExtra("DUE_DATE", task.dueDate)
                putExtra("IS_DUE_NOW", false)
            }
            val pendingIntentSoon = PendingIntent.getBroadcast(
                context,
                task.id,
                intentSoon,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            setAlarm(alarmManager, triggerTimeSoon, pendingIntentSoon)
        }

        // 2. Schedule for the exact dueDate
        val triggerTimeNow = task.dueDate
        if (triggerTimeNow > currentTime) {
            val intentNow = Intent(context, TaskNotificationReceiver::class.java).apply {
                putExtra("TASK_ID", task.id)
                putExtra("TASK_TITLE", task.title)
                putExtra("DUE_DATE", task.dueDate)
                putExtra("IS_DUE_NOW", true)
            }
            val pendingIntentNow = PendingIntent.getBroadcast(
                context,
                task.id + 2000, // Unique request code for the "Due Now" alarm
                intentNow,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            setAlarm(alarmManager, triggerTimeNow, pendingIntentNow)
        }
    }

    private fun setAlarm(alarmManager: AlarmManager, triggerTime: Long, pendingIntent: PendingIntent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
        }
    }

    fun cancelTaskNotification(context: Context, taskId: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        
        // Cancel "Due Soon" notification
        val intentSoon = Intent(context, TaskNotificationReceiver::class.java)
        val pendingIntentSoon = PendingIntent.getBroadcast(
            context,
            taskId,
            intentSoon,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntentSoon)

        // Cancel "Due Now" notification
        val intentNow = Intent(context, TaskNotificationReceiver::class.java)
        val pendingIntentNow = PendingIntent.getBroadcast(
            context,
            taskId + 2000,
            intentNow,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntentNow)
    }
}
