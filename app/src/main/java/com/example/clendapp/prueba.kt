package com.example.clendapp

import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.WindowManager
import android.widget.Chronometer
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.util.Locale
import kotlin.math.min

class prueba : AppCompatActivity() {

    private var isLockedState = false
    private lateinit var chronometerView: Chronometer
    private lateinit var volumeToggle: ImageView
    private var currentLayoutResId: Int = 0
    private var isServiceStarted = false

    private val timerReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == BackgroundService.ACTION_TIMER_UPDATE) {
                val remaining = intent.getLongExtra(BackgroundService.EXTRA_TIME, 0)
                val locked = intent.getBooleanExtra(BackgroundService.EXTRA_LOCKED, true)
                val finished = intent.getBooleanExtra(BackgroundService.EXTRA_FINISHED, false)
                val musicOn = intent.getBooleanExtra(BackgroundService.EXTRA_MUSIC_ON, true)

                updateUI(remaining)
                updateMusicIcon(musicOn)

                if (finished) {
                    showFinishModal()
                    return
                }

                if (locked != isLockedState) {
                    if (locked) lockApp() else unlockApp()
                }
            }
        }
    }

    private val backPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            if (isLockedState) {
                Toast.makeText(this@prueba, "Study mode active: You cannot leave", Toast.LENGTH_SHORT).show()
            } else {
                if (!isServiceStarted) {
                    finish() 
                } else {
                    moveTaskToBack(true)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        currentLayoutResId = intent.getIntExtra("LAYOUT_RES_ID", R.layout.activity_prueba)
        setContentView(currentLayoutResId)

        val rootView = findViewById<android.view.View>(R.id.main)
        if (rootView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(rootView) { v, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
                insets
            }
        }

        onBackPressedDispatcher.addCallback(this, backPressedCallback)
        chronometerView = findViewById(R.id.chronometer_prueba)
        chronometerView.text = "00:00:00"
        
        volumeToggle = findViewById(R.id.btn_volume_toggle)
        volumeToggle.setOnClickListener {
            val toggleIntent = Intent(this, BackgroundService::class.java)
            toggleIntent.action = BackgroundService.ACTION_TOGGLE_MUSIC
            startService(toggleIntent)
        }

        checkOverlayPermissionAndStart()

        val filter = IntentFilter(BackgroundService.ACTION_TIMER_UPDATE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(timerReceiver, filter, RECEIVER_EXPORTED)
        } else {
            registerReceiver(timerReceiver, filter)
        }
    }

    private fun showFinishModal() {
        unlockApp()
        val totalTimeMillis = intent.getLongExtra("TOTAL_TIME_MILLIS", 0)
        
        val fullCycles = totalTimeMillis / 40000
        val remainder = totalTimeMillis % 40000
        val studyMillis = (fullCycles * 30000) + min(remainder, 30000)
        
        val totalSeconds = studyMillis / 1000
        val h = totalSeconds / 3600
        val m = (totalSeconds % 3600) / 60
        val s = totalSeconds % 60
        val studyTimeFormatted = String.format(Locale.getDefault(), "%02d:%02d:%02d", h, m, s)

        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_calculadora_game, null)
        val dialog = AlertDialog.Builder(this, R.style.CustomDialogTheme)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        dialogView.findViewById<TextView>(R.id.tv_dialog_title).text = "Session Finished!"
        dialogView.findViewById<TextView>(R.id.tv_dialog_message).text = "Congratulations on completing your session.\n\nReal study time: $studyTimeFormatted"
        
        dialogView.findViewById<TextView>(R.id.btn_dialog_accept).text = "Accept"
        dialogView.findViewById<TextView>(R.id.btn_dialog_accept).setOnClickListener {
            dialog.dismiss()
            val backToCronometro = Intent(this, Cronometro::class.java)
            backToCronometro.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            startActivity(backToCronometro)
            finish()
        }

        dialog.show()
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.9).toInt(),
            WindowManager.LayoutParams.WRAP_CONTENT
        )
    }

    private fun updateMusicIcon(musicOn: Boolean) {
        if (musicOn) {
            volumeToggle.setImageResource(R.drawable.ic_volume_up)
        } else {
            volumeToggle.setImageResource(R.drawable.ic_volume_off)
        }
    }

    private fun checkOverlayPermissionAndStart() {
        if (!Settings.canDrawOverlays(this)) {
            isLockedState = false
            backPressedCallback.isEnabled = false 
            
            val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_calculadora_game, null)
            val dialog = AlertDialog.Builder(this, R.style.CustomDialogTheme)
                .setView(dialogView)
                .setCancelable(false)
                .create()

            dialogView.findViewById<TextView>(R.id.tv_dialog_title).text = "Permission Required"
            dialogView.findViewById<TextView>(R.id.tv_dialog_message).apply {
                text = "To enable study mode and auto-return after breaks, please activate 'Display over other apps'."
                textSize = 18f // Un poco más grande para mejor lectura
            }
            
            dialogView.findViewById<TextView>(R.id.btn_dialog_accept).text = "Configure"
            dialogView.findViewById<TextView>(R.id.btn_dialog_accept).setOnClickListener {
                dialog.dismiss()
                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
                startActivity(intent)
            }

            val btnCancel = dialogView.findViewById<TextView>(R.id.btn_dialog_cancel)
            btnCancel.visibility = android.view.View.VISIBLE
            btnCancel.text = "Cancel"
            btnCancel.setOnClickListener {
                dialog.dismiss()
                finish()
            }

            dialog.show()
            dialog.window?.setLayout(
                (resources.displayMetrics.widthPixels * 0.9).toInt(),
                WindowManager.LayoutParams.WRAP_CONTENT
            )
        } else {
            startStudyService()
        }
    }

    private fun startStudyService() {
        if (isServiceStarted) return
        
        val totalTime = intent.getLongExtra("TOTAL_TIME_MILLIS", 0)
        if (totalTime > 0) {
            val serviceIntent = Intent(this, BackgroundService::class.java)
            serviceIntent.putExtra("TOTAL_TIME", totalTime)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
            isServiceStarted = true
            lockApp() 
        }
    }

    override fun onResume() {
        super.onResume()
        if (!isServiceStarted && Settings.canDrawOverlays(this)) {
            startStudyService()
        }

        if (isLockedState && isServiceStarted) {
            try {
                val am = getSystemService(ACTIVITY_SERVICE) as ActivityManager
                if (am.lockTaskModeState == ActivityManager.LOCK_TASK_MODE_NONE) {
                    startLockTask()
                }
            } catch (e: Exception) {}
        }
    }

    private fun updateUI(millis: Long) {
        val totalSeconds = millis / 1000
        val h = totalSeconds / 3600
        val m = (totalSeconds % 3600) / 60
        val s = totalSeconds % 60
        chronometerView.text = String.format(Locale.getDefault(), "%02d:%02d:%02d", h, m, s)
    }

    private fun lockApp() {
        isLockedState = true
        backPressedCallback.isEnabled = true
        try {
            startLockTask()
        } catch (e: Exception) {}
        Toast.makeText(this, "LOCKED - STUDY TIME!", Toast.LENGTH_SHORT).show()
    }

    private fun unlockApp() {
        isLockedState = false
        backPressedCallback.isEnabled = false
        try {
            stopLockTask()
        } catch (e: Exception) {}
        Toast.makeText(this, "FREE TIME - 10 SECONDS", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(timerReceiver)
        } catch (e: Exception) {}
    }
}
