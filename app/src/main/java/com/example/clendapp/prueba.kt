package com.example.clendapp

import android.os.Bundle
import android.os.CountDownTimer
import android.widget.Chronometer
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.util.Locale

class prueba : AppCompatActivity() {

    private var countDownTimer: CountDownTimer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Obtener el layout aleatorio y el tiempo desde el Intent
        val layoutResId = intent.getIntExtra("LAYOUT_RES_ID", R.layout.activity_prueba)
        val totalMillis = intent.getLongExtra("TOTAL_TIME_MILLIS", 0L)

        setContentView(layoutResId)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val chronometer = findViewById<Chronometer>(R.id.chronometer_prueba)

        if (totalMillis > 0) {
            startCountdown(totalMillis, chronometer)
        } else {
            finish()
        }
    }

    private fun startCountdown(duration: Long, chronometer: Chronometer) {
        countDownTimer = object : CountDownTimer(duration, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val seconds = (millisUntilFinished / 1000) % 60
                val minutes = (millisUntilFinished / (1000 * 60)) % 60
                val hours = (millisUntilFinished / (1000 * 60 * 60))

                val time = if (hours > 0) {
                    String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
                } else {
                    String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
                }
                
                // Usamos el texto del Chronometer para mostrar la cuenta regresiva
                chronometer.text = time
            }

            override fun onFinish() {
                chronometer.text = "00:00"
                // Al llegar a 0, regresar a la pantalla anterior (Cronometro)
                finish()
            }
        }.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
    }
}
