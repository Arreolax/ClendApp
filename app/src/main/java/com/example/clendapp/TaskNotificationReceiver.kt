package com.example.clendapp

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import java.text.SimpleDateFormat
import java.util.*

class TaskNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getIntExtra("TASK_ID", 0)
        val taskTitle = intent.getStringExtra("TASK_TITLE") ?: "Task Reminder"
        val dueDate = intent.getLongExtra("DUE_DATE", 0L)
        val isDueNow = intent.getBooleanExtra("IS_DUE_NOW", false)

        // Intent for clicking the notification itself
        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            taskId + (if (isDueNow) 3000 else 0), // Unique request code
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Intent for the "Complete" button action
        val completeIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("OPEN_TASKS", true)
        }

        val completePendingIntent = PendingIntent.getActivity(
            context,
            taskId + (if (isDueNow) 4000 else 1000), // Unique request code
            completeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val timeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())
        val timeStr = if (dueDate != 0L) timeFormatter.format(Date(dueDate)) else ""
        
        val title = if (isDueNow) "Pending task" else "Task Due Soon"
        val contentText = if (isDueNow) {
            "Time to complete: $taskTitle"
        } else {
            if (timeStr.isNotEmpty()) "$taskTitle at $timeStr" else taskTitle
        }

        val builder = NotificationCompat.Builder(context, NotificationHelper.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_clock)
            .setContentTitle(title)
            .setContentText(contentText)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .addAction(R.drawable.ic_check, "Complete", completePendingIntent)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        // Use different notification ID for "Due Now" if we want both to coexist, 
        // but typically we want to update the existing one or just use taskId.
        // Let's use taskId + 2000 for the actual notification ID if it's due now to avoid overwriting if they are close.
        val notificationId = if (isDueNow) taskId + 2000 else taskId
        notificationManager.notify(notificationId, builder.build())
    }
}
