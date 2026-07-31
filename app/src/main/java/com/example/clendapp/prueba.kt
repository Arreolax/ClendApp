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
import android.view.WindowManager
import android.widget.Chronometer
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.Locale

class prueba : AppCompatActivity() {

    private var isLockedState = false
    private lateinit var chronometerView: Chronometer
    private var currentLayoutResId: Int = 0
    private var isServiceStarted = false

    private val timerReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == BackgroundService.ACTION_TIMER_UPDATE) {
                val remaining = intent.getLongExtra(BackgroundService.EXTRA_TIME, 0)
                val locked = intent.getBooleanExtra(BackgroundService.EXTRA_LOCKED, true)
                val finished = intent.getBooleanExtra(BackgroundService.EXTRA_FINISHED, false)

                updateUI(remaining)

                if (finished) {
                    unlockApp()
                    val backToCronometro = Intent(this@prueba, Cronometro::class.java)
                    backToCronometro.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    startActivity(backToCronometro)
                    finish()
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
                Toast.makeText(this@prueba, "Modo estudio activo: No puedes salir", Toast.LENGTH_SHORT).show()
            } else {
                if (!isServiceStarted) {
                    finish() // Si no ha empezado, permitir salir
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

        // IMPORTANTE: Primero verificamos permisos, el servicio NO inicia aquí
        checkOverlayPermissionAndStart()

        val filter = IntentFilter(BackgroundService.ACTION_TIMER_UPDATE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(timerReceiver, filter, RECEIVER_EXPORTED)
        } else {
            registerReceiver(timerReceiver, filter)
        }
    }

    private fun checkOverlayPermissionAndStart() {
        if (!Settings.canDrawOverlays(this)) {
            isLockedState = false
            backPressedCallback.isEnabled = false // Permitir atrás para salir si no quiere dar permiso
            
            MaterialAlertDialogBuilder(this)
                .setTitle("Permiso Requerido")
                .setMessage("Para que el modo estudio funcione y la app regrese sola tras el tiempo libre, activa 'Mostrar sobre otras apps'.")
                .setCancelable(false)
                .setPositiveButton("Configurar") { _, _ ->
                    val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
                    startActivity(intent)
                }
                .setNegativeButton("Cancelar") { _, _ ->
                    finish()
                }
                .show()
        } else {
            // Si ya tiene permiso, iniciamos el servicio
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
            lockApp() // Empezamos bloqueando
        }
    }

    override fun onResume() {
        super.onResume()
        // Cuando el usuario regresa de Ajustes, comprobamos si ya dio el permiso
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
        Toast.makeText(this, "BLOQUEADO - ¡A ESTUDIAR!", Toast.LENGTH_SHORT).show()
    }

    private fun unlockApp() {
        isLockedState = false
        backPressedCallback.isEnabled = false
        try {
            stopLockTask()
        } catch (e: Exception) {}
        Toast.makeText(this, "TIEMPO LIBRE - 10 SEGUNDOS", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(timerReceiver)
        } catch (e: Exception) {}
    }
}
