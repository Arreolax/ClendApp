package com.example.clendapp

import android.content.Context
import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.clendapp.data.AppDatabase
import com.example.clendapp.data.Scores
import kotlinx.coroutines.launch
import kotlin.random.Random

class calculadora : AppCompatActivity() {

    private lateinit var tvOperacion: TextView
    private lateinit var tvResultado: TextView
    private lateinit var btnJuego: TextView
    private lateinit var tvScore: TextView
    private lateinit var tvTimer: TextView

    private var actualExpression = ""
    private var lastWasOperator = false

    // Variables del juego
    private var isGameMode = false
    private var score = 0
    private var bestScore = 0
    private var targetResult = 0.0
    private var countDownTimer: CountDownTimer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_calculadora)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        tvOperacion = findViewById(R.id.tv_operacion)
        tvResultado = findViewById(R.id.tv_resultado)
        btnJuego = findViewById(R.id.btn_juego)
        tvScore = findViewById(R.id.tv_score)
        tvTimer = findViewById(R.id.tv_timer)

        tvOperacion.text = ""
        tvResultado.text = "0"

        // Cargar mejor puntaje
        val sharedPref = getSharedPreferences("clend_app_prefs", Context.MODE_PRIVATE)
        val userId = sharedPref.getInt("user_id", -1)
        bestScore = sharedPref.getInt("best_score_calc", 0)

        if (userId != -1) {
            val database = AppDatabase.getDatabase(this)
            lifecycleScope.launch {
                val scores = database.scoresDao().getScoresByUserId(userId)
                scores?.let {
                    if (it.calculator_score > bestScore) {
                        bestScore = it.calculator_score
                        with(sharedPref.edit()) {
                            putInt("best_score_calc", bestScore)
                            apply()
                        }
                    }
                }
            }
        }

        btnJuego.setOnClickListener {
            if (isGameMode) {
                showExitConfirmation()
            } else {
                toggleGameMode()
            }
        }

        setupButtons()
    }

    private fun toggleGameMode() {
        isGameMode = !isGameMode
        if (isGameMode) {
            score = 0
            btnJuego.text = "Quit"
            btnJuego.setBackgroundResource(R.drawable.bg_tecla_igual) // Color distintivo
            tvScore.text = "Score: $score"
            tvScore.visibility = View.VISIBLE
            tvTimer.visibility = View.VISIBLE
            nextQuestion()
            Toast.makeText(this, "Game Mode: 7 seconds per question!", Toast.LENGTH_SHORT).show()
        } else {
            stopGame()
        }
    }

    private fun stopGame() {
        countDownTimer?.cancel()
        isGameMode = false
        btnJuego.text = "Game"
        btnJuego.setBackgroundResource(R.drawable.bg_boton_juego)
        tvScore.visibility = View.GONE
        tvTimer.visibility = View.GONE
        actualExpression = ""
        tvOperacion.text = ""
        tvResultado.text = "0"
    }

    private fun startTimer() {
        countDownTimer?.cancel()
        countDownTimer = object : CountDownTimer(7000, 100) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsLeft = (millisUntilFinished / 1000.0).toInt() + 1
                tvTimer.text = "${secondsLeft}s"
            }

            override fun onFinish() {
                tvTimer.text = "0s"
                handleTimeout()
            }
        }.start()
    }

    private fun handleTimeout() {
        val finalScore = score
        updateBestScore(finalScore)
        showGameOverModal(finalScore, "Time's up!")
    }

    private fun updateBestScore(currentScore: Int) {
        val sharedPref = getSharedPreferences("clend_app_prefs", Context.MODE_PRIVATE)
        val userId = sharedPref.getInt("user_id", -1)

        // 1. Actualizar localmente si es record
        if (currentScore > bestScore) {
            bestScore = currentScore
            with(sharedPref.edit()) {
                putInt("best_score_calc", bestScore)
                apply()
            }
        }

        // 2. Sincronizar con base de datos siempre que haya un usuario
        if (userId != -1) {
            val database = AppDatabase.getDatabase(this)
            lifecycleScope.launch {
                val scores = database.scoresDao().getScoresByUserId(userId)
                if (scores != null) {
                    if (currentScore > scores.calculator_score) {
                        val updatedScores = scores.copy(calculator_score = currentScore)
                        database.scoresDao().updateScores(updatedScores)
                    }
                } else {
                    // Si no existe el registro, lo creamos
                    val newScores = Scores(id_user = userId, calculator_score = currentScore)
                    database.scoresDao().insertScores(newScores)
                }
            }
        }
    }

    private fun nextQuestion() {
        actualExpression = ""
        tvResultado.text = "0"
        startTimer()

        // Aumentar dificultad basada en score
        val range = 10 + (score * 5)
        val num1 = Random.nextInt(1, range)
        val num2 = Random.nextInt(1, range)

        val operators = mutableListOf("+", "-")
        if (score >= 5) operators.add("×")
        if (score >= 10) operators.add("÷")

        val op = operators.random()

        when (op) {
            "+" -> {
                targetResult = (num1 + num2).toDouble()
                tvOperacion.text = "$num1 + $num2 = ?"
            }
            "-" -> {
                targetResult = (num1 - num2).toDouble()
                tvOperacion.text = "$num1 - $num2 = ?"
            }
            "×" -> {
                val n1 = Random.nextInt(1, 10 + score / 2)
                val n2 = Random.nextInt(1, 10)
                targetResult = (n1 * n2).toDouble()
                tvOperacion.text = "$n1 × $n2 = ?"
            }
            "÷" -> {
                val divisor = Random.nextInt(1, 10)
                val quotient = Random.nextInt(1, 10 + score / 3)
                val dividend = divisor * quotient
                targetResult = quotient.toDouble()
                tvOperacion.text = "$dividend ÷ $divisor = ?"
            }
        }
    }

    private fun checkAnswer() {
        if (actualExpression.isEmpty() || actualExpression == "-") return

        try {
            val userResult = actualExpression.replace(",", ".").toDouble()
            countDownTimer?.cancel()
            
            if (userResult == targetResult) {
                score++
                tvScore.text = "Score: $score"
                // No modal for correct answer, just go to next question
                nextQuestion()
            } else {
                val finalScore = score
                updateBestScore(finalScore)
                showGameOverModal(finalScore)
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Enter a valid number", Toast.LENGTH_SHORT).show()
            startTimer()
        }
    }

    private fun showGameOverModal(finalScore: Int, title: String = "Incorrect") {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_calculadora_game, null)
        val dialog = AlertDialog.Builder(this, R.style.CustomDialogTheme)
            .setCancelable(false)
            .setView(dialogView)
            .create()

        dialogView.findViewById<TextView>(R.id.tv_dialog_title).text = title
        dialogView.findViewById<TextView>(R.id.tv_dialog_message).text = "Game Over\nYour score is: $finalScore"
        
        val btnRetry = dialogView.findViewById<TextView>(R.id.btn_dialog_accept)
        val btnExit = dialogView.findViewById<TextView>(R.id.btn_dialog_cancel)
        
        btnRetry.text = "Retry"
        btnExit.text = "Exit"
        btnExit.visibility = View.VISIBLE

        btnRetry.setOnClickListener {
            dialog.dismiss()
            score = 0
            tvScore.text = "Score: $score"
            nextQuestion()
        }

        btnExit.setOnClickListener {
            dialog.dismiss()
            stopGame()
        }

        dialog.show()
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.9).toInt(),
            WindowManager.LayoutParams.WRAP_CONTENT
        )
    }

    private fun showStatusModal(title: String, message: String, onAccept: () -> Unit) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_calculadora_game, null)
        val dialog = AlertDialog.Builder(this, R.style.CustomDialogTheme)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        dialogView.findViewById<TextView>(R.id.tv_dialog_title).text = title
        dialogView.findViewById<TextView>(R.id.tv_dialog_message).text = message
        
        dialogView.findViewById<TextView>(R.id.btn_dialog_accept).setOnClickListener {
            dialog.dismiss()
            onAccept()
        }

        dialog.show()
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.9).toInt(),
            WindowManager.LayoutParams.WRAP_CONTENT
        )
    }

    private fun showExitConfirmation() {
        countDownTimer?.cancel()
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_calculadora_game, null)
        val dialog = AlertDialog.Builder(this, R.style.CustomDialogTheme)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        dialogView.findViewById<TextView>(R.id.tv_dialog_title).text = "Exit Game"
        dialogView.findViewById<TextView>(R.id.tv_dialog_message).text = "Are you sure you want to quit the game?"
        
        val btnAccept = dialogView.findViewById<TextView>(R.id.btn_dialog_accept)
        val btnCancel = dialogView.findViewById<TextView>(R.id.btn_dialog_cancel)
        
        btnAccept.text = "Exit"
        btnCancel.text = "Keep Playing"
        btnCancel.visibility = View.VISIBLE

        btnAccept.setOnClickListener {
            dialog.dismiss()
            updateBestScore(score)
            showGoodbyeModal()
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
            startTimer()
        }

        dialog.show()
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.9).toInt(),
            WindowManager.LayoutParams.WRAP_CONTENT
        )
    }

    private fun showGoodbyeModal() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_calculadora_game, null)
        val dialog = AlertDialog.Builder(this, R.style.CustomDialogTheme)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        dialogView.findViewById<TextView>(R.id.tv_dialog_title).text = "Goodbye!"
        dialogView.findViewById<TextView>(R.id.tv_dialog_message).text = "Thanks for playing!\nYour best score is: $bestScore"
        
        dialogView.findViewById<TextView>(R.id.btn_dialog_accept).setOnClickListener {
            dialog.dismiss()
            stopGame()
        }

        dialog.show()
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.9).toInt(),
            WindowManager.LayoutParams.WRAP_CONTENT
        )
    }

    private fun setupButtons() {
        val numbers = listOf(
            R.id.btn_0, R.id.btn_1, R.id.btn_2, R.id.btn_3, R.id.btn_4,
            R.id.btn_5, R.id.btn_6, R.id.btn_7, R.id.btn_8, R.id.btn_9, R.id.btn_punto
        )

        for (id in numbers) {
            findViewById<Button>(id).setOnClickListener {
                val btn = it as Button
                val text = btn.text.toString()
                
                if (isGameMode) {
                    if (text == "." && actualExpression.contains(".")) return@setOnClickListener
                    
                    if (actualExpression == "0") actualExpression = ""
                    actualExpression += text
                    tvResultado.text = actualExpression
                } else {
                    actualExpression += text
                    tvOperacion.text = actualExpression
                }
                lastWasOperator = false
            }
        }

        val operators = mapOf(
            R.id.btn_sumar to "+",
            R.id.btn_restar to "-",
            R.id.btn_multiplicar to "×",
            R.id.btn_dividir to "÷"
        )

        for ((id, symbol) in operators) {
            findViewById<Button>(id).setOnClickListener {
                if (!isGameMode) {
                    if (actualExpression.isNotEmpty() && !lastWasOperator) {
                        actualExpression += symbol
                        tvOperacion.text = actualExpression
                        lastWasOperator = true
                    }
                } else {
                    if (symbol == "-" && actualExpression.isEmpty()) {
                        actualExpression = "-"
                        tvResultado.text = actualExpression
                    }
                }
            }
        }

        findViewById<Button>(R.id.btn_c).setOnClickListener {
            actualExpression = ""
            tvResultado.text = "0"
            if (!isGameMode) tvOperacion.text = ""
            lastWasOperator = false
        }

        findViewById<Button>(R.id.btn_igual).setOnClickListener {
            if (isGameMode) {
                checkAnswer()
            } else {
                if (actualExpression.isNotEmpty()) {
                    calcularResultado()
                }
            }
        }

        findViewById<TextView>(R.id.btn_back).setOnClickListener {
            if (isGameMode) {
                showExitConfirmation()
            } else {
                finish()
            }
        }

        findViewById<Button>(R.id.btn_mas_menos)?.setOnClickListener {
            if (!isGameMode && actualExpression.isNotEmpty()) {
                if (actualExpression.startsWith("-")) {
                    actualExpression = actualExpression.substring(1)
                } else {
                    actualExpression = "-$actualExpression"
                }
                tvOperacion.text = actualExpression
            }
        }
    }

    private fun calcularResultado() {
        try {
            val expression = actualExpression.replace("×", "*").replace("÷", "/")
            val result = eval(expression)
            val resultString = if (result % 1.0 == 0.0) {
                result.toLong().toString()
            } else {
                String.format(java.util.Locale.US, "%.2f", result)
            }
            tvResultado.text = resultString
            actualExpression = resultString
            lastWasOperator = false
        } catch (e: Exception) {
            tvResultado.text = "Error"
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
    }

    private fun eval(str: String): Double {
        return object : Any() {
            var pos = -1
            var ch = 0

            fun nextChar() {
                ch = if (++pos < str.length) str[pos].toInt() else -1
            }

            fun eat(charToEat: Int): Boolean {
                while (ch == ' '.toInt()) nextChar()
                if (ch == charToEat) {
                    nextChar()
                    return true
                }
                return false
            }

            fun parse(): Double {
                nextChar()
                val x = parseExpression()
                if (pos < str.length) throw RuntimeException("Unexpected: " + ch.toChar())
                return x
            }

            fun parseExpression(): Double {
                var x = parseTerm()
                while (true) {
                    if (eat('+'.toInt())) x += parseTerm()
                    else if (eat('-'.toInt())) x -= parseTerm()
                    else return x
                }
            }

            fun parseTerm(): Double {
                var x = parseFactor()
                while (true) {
                    if (eat('*'.toInt())) x *= parseFactor()
                    else if (eat('/'.toInt())) x /= parseFactor()
                    else return x
                }
            }

            fun parseFactor(): Double {
                if (eat('+'.toInt())) return parseFactor()
                if (eat('-'.toInt())) return -parseFactor()
                var x: Double
                val startPos = pos
                if (eat('('.toInt())) {
                    x = parseExpression()
                    eat(')'.toInt())
                } else if (ch >= '0'.toInt() && ch <= '9'.toInt() || ch == '.'.toInt()) {
                    while (ch >= '0'.toInt() && ch <= '9'.toInt() || ch == '.'.toInt()) nextChar()
                    x = java.lang.Double.parseDouble(str.substring(startPos, pos))
                } else {
                    throw RuntimeException("Unexpected: " + ch.toChar())
                }
                return x
            }
        }.parse()
    }
}
