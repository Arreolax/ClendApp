package com.example.clendapp

import android.app.*
import android.content.Intent
import android.media.MediaPlayer
import android.os.Build
import android.os.CountDownTimer
import android.os.IBinder
import androidx.core.app.NotificationCompat
import java.util.Locale

class BackgroundService : Service() {

    private var countDownTimer: CountDownTimer? = null
    private var isCurrentlyLocked: Boolean? = null
    private var mediaPlayer: MediaPlayer? = null
    private var ringtonePlayer: MediaPlayer? = null
    private var isMusicEnabled = true

    companion object {
        const val ACTION_TIMER_UPDATE = "com.example.clendapp.TIMER_UPDATE"
        const val ACTION_TOGGLE_MUSIC = "com.example.clendapp.TOGGLE_MUSIC"
        const val EXTRA_TIME = "extra_time"
        const val EXTRA_LOCKED = "extra_locked"
        const val EXTRA_FINISHED = "extra_finished"
        const val EXTRA_MUSIC_ON = "extra_music_on"
        const val CHANNEL_ID = "TimerServiceChannel"
        const val NOTIFICATION_ID = 1
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        mediaPlayer = MediaPlayer.create(this, R.raw.subwooferlullaby)
        mediaPlayer?.isLooping = true
        
        ringtonePlayer = MediaPlayer.create(this, R.raw.cuteringtone2)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_TOGGLE_MUSIC) {
            isMusicEnabled = !isMusicEnabled
            handleMusicState()
            broadcastUpdate(0, isCurrentlyLocked ?: true, false)
            return START_NOT_STICKY
        }

        val totalTime = intent?.getLongExtra("TOTAL_TIME", 0) ?: 0
        if (totalTime > 0) {
            startForeground(NOTIFICATION_ID, createNotification("Iniciando modo concentración..."))
            startTimer(totalTime)
        }
        return START_NOT_STICKY
    }

    private fun startTimer(duration: Long) {
        countDownTimer?.cancel()
        countDownTimer = object : CountDownTimer(duration, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val elapsedSeconds = (duration - millisUntilFinished) / 1000
                val cycleSeconds = elapsedSeconds % 40
                val shouldLock = cycleSeconds < 30

                if (isCurrentlyLocked != shouldLock) {
                    isCurrentlyLocked = shouldLock
                    handleLockTransition(shouldLock, millisUntilFinished)
                } else {
                    broadcastUpdate(millisUntilFinished, shouldLock, false)
                }

                updateNotification(millisUntilFinished, shouldLock)
            }

            override fun onFinish() {
                isCurrentlyLocked = false
                handleMusicState()
                playRingtone() // Suena al finalizar todo el tiempo
                broadcastUpdate(0, false, true)
                
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        stopForeground(STOP_FOREGROUND_REMOVE)
                    } else {
                        @Suppress("DEPRECATION")
                        stopForeground(true)
                    }
                    stopSelf()
                }, 2000)
            }
        }.start()
    }

    private fun handleLockTransition(locked: Boolean, time: Long) {
        broadcastUpdate(time, locked, false)
        handleMusicState()
        
        if (locked) {
            val activityIntent = Intent(this, prueba::class.java)
            activityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or 
                                    Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or 
                                    Intent.FLAG_ACTIVITY_SINGLE_TOP)
            startActivity(activityIntent)
        } else {
            playRingtone()
        }
    }

    private fun playRingtone() {
        try {
            if (ringtonePlayer?.isPlaying == true) {
                ringtonePlayer?.stop()
                ringtonePlayer?.prepare()
            }
            ringtonePlayer?.start()
        } catch (e: Exception) {
            ringtonePlayer = MediaPlayer.create(this, R.raw.cuteringtone2)
            ringtonePlayer?.start()
        }
    }

    private fun handleMusicState() {
        if (isMusicEnabled && isCurrentlyLocked == true) {
            if (mediaPlayer?.isPlaying == false) {
                mediaPlayer?.start()
            }
        } else {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.pause()
            }
        }
    }

    private fun broadcastUpdate(time: Long, locked: Boolean, finished: Boolean) {
        val intent = Intent(ACTION_TIMER_UPDATE)
        intent.putExtra(EXTRA_TIME, time)
        intent.putExtra(EXTRA_LOCKED, locked)
        intent.putExtra(EXTRA_FINISHED, finished)
        intent.putExtra(EXTRA_MUSIC_ON, isMusicEnabled)
        sendBroadcast(intent)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Temporizador de Estudio",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    private fun createNotification(content: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Clend App")
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification(millis: Long, locked: Boolean) {
        val status = if (locked) "MODO ESTUDIO: BLOQUEADO" else "TIEMPO LIBRE: DESCANSA"
        val seconds = (millis / 1000) % 60
        val minutes = (millis / (1000 * 60)) % 60
        val hours = (millis / (1000 * 3600))
        
        val timeStr = if (hours > 0) {
            String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
        }
        
        val notification = createNotification("$status ($timeStr)")
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, notification)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        countDownTimer?.cancel()
        mediaPlayer?.release()
        ringtonePlayer?.release()
        mediaPlayer = null
        ringtonePlayer = null
        super.onDestroy()
    }
}
