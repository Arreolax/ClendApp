package com.example.clendapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.clendapp.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val sharedPref = context.getSharedPreferences("clend_app_prefs", Context.MODE_PRIVATE)
            val userId = sharedPref.getInt("user_id", -1)

            if (userId != -1) {
                val database = AppDatabase.getDatabase(context)
                CoroutineScope(Dispatchers.IO).launch {
                    val tasks = database.tasksDao().getAllTasksList(userId)
                    tasks.forEach { task ->
                        NotificationHelper.scheduleTaskNotification(context, task)
                    }
                }
            }
        }
    }
}
