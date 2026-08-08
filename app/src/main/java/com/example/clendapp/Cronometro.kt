package com.example.clendapp

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.bottomnavigation.BottomNavigationView

class Cronometro : AppCompatActivity() {

    private var hours = 0
    private var minutes = 25
    private var seconds = 0

    private lateinit var tvHh: TextView
    private lateinit var tvMm: TextView
    private lateinit var tvSs: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_cronometro)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        tvHh = findViewById(R.id.tv_hh)
        tvMm = findViewById(R.id.tv_mm)
        tvSs = findViewById(R.id.tv_ss)

        updateDisplay()

        // Horas
        findViewById<TextView>(R.id.btn_hh_up).setOnClickListener {
            hours = (hours + 1) % 100
            updateDisplay()
        }
        findViewById<TextView>(R.id.btn_hh_down).setOnClickListener {
            hours = if (hours > 0) hours - 1 else 99
            updateDisplay()
        }

        // Minutos
        findViewById<TextView>(R.id.btn_mm_up).setOnClickListener {
            minutes = (minutes + 1) % 60
            updateDisplay()
        }
        findViewById<TextView>(R.id.btn_mm_down).setOnClickListener {
            minutes = if (minutes > 0) minutes - 1 else 59
            updateDisplay()
        }

        // Segundos
        findViewById<TextView>(R.id.btn_ss_up).setOnClickListener {
            seconds = (seconds + 1) % 60
            updateDisplay()
        }
        findViewById<TextView>(R.id.btn_ss_down).setOnClickListener {
            seconds = if (seconds > 0) seconds - 1 else 59
            updateDisplay()
        }

        // Presets
        findViewById<Button>(R.id.btn_20m).setOnClickListener {
            hours = 0
            minutes = 20
            seconds = 0
            updateDisplay()
        }
        findViewById<Button>(R.id.btn_60m).setOnClickListener {
            hours = 1
            minutes = 0
            seconds = 0
            updateDisplay()
        }
        findViewById<Button>(R.id.btn_90m).setOnClickListener {
            hours = 1
            minutes = 30
            seconds = 0
            updateDisplay()
        }

        findViewById<Button>(R.id.btn_start).setOnClickListener {
            val totalMillis = ((hours * 3600L) + (minutes * 60L) + seconds) * 1000L
            if (totalMillis > 0) {
                showConfirmationDialog(totalMillis)
            } else {
                Toast.makeText(this, "Please set a time", Toast.LENGTH_SHORT).show()
            }
        }

        findViewById<TextView>(R.id.btn_back).setOnClickListener {
            finish()
        }

        setupBottomNavigation()
    }

    private fun showConfirmationDialog(totalMillis: Long) {
        try {
            val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_confirm_start, null)
            val dialog = AlertDialog.Builder(this, R.style.CustomDialogTheme)
                .setView(dialogView)
                .setCancelable(true)
                .create()

            dialogView.findViewById<TextView>(R.id.btn_start_accept).setOnClickListener {
                dialog.dismiss()
                startStudySession(totalMillis)
            }

            dialogView.findViewById<TextView>(R.id.btn_start_cancel).setOnClickListener {
                dialog.dismiss()
            }

            dialog.show()
            
            dialog.window?.setLayout(
                (resources.displayMetrics.widthPixels * 0.9).toInt(),
                WindowManager.LayoutParams.WRAP_CONTENT
            )
        } catch (e: Exception) {
            // Fallback if dialog fails
            startStudySession(totalMillis)
        }
    }

    private fun startStudySession(totalMillis: Long) {
        val layouts = listOf(
            R.layout.activity_prueba,
            R.layout.fondo2,
            R.layout.fondo3,
            R.layout.fondo4
        )
        val randomLayout = layouts.random()

        val intent = Intent(this, prueba::class.java)
        intent.putExtra("TOTAL_TIME_MILLIS", totalMillis)
        intent.putExtra("LAYOUT_RES_ID", randomLayout)
        startActivity(intent)
    }

    private fun setupBottomNavigation() {
        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigation.selectedItemId = R.id.nav_history

        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_history -> true
                R.id.nav_menu -> {
                    val intent = Intent(this, MainActivity::class.java)
                    intent.putExtra("OPEN_DRAWER", true)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    startActivity(intent)
                    finish()
                    false
                }
                else -> {
                    val intent = Intent(this, MainActivity::class.java)
                    intent.putExtra("NAV_DESTINATION", item.itemId)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    startActivity(intent)
                    finish()
                    true
                }
            }
        }
    }

    private fun updateDisplay() {
        val locale = java.util.Locale.getDefault()
        tvHh.text = String.format(locale, "%02d", hours)
        tvMm.text = String.format(locale, "%02d", minutes)
        tvSs.text = String.format(locale, "%02d", seconds)
    }
}
