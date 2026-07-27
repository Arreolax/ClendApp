package com.example.clendapp

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import kotlin.random.Random

class calculadora : AppCompatActivity() {

    private lateinit var tvOperacion: TextView
    private lateinit var tvResultado: TextView
    private lateinit var btnJuego: TextView

    private var actualExpression = ""
    private var lastWasOperator = false

    // Variables del juego
    private var isGameMode = false
    private var score = 0
    private var targetResult = 0.0

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

        tvOperacion.text = ""
        tvResultado.text = "0"

        btnJuego.setOnClickListener {
            toggleGameMode()
        }

        setupButtons()
    }

    private fun toggleGameMode() {
        isGameMode = !isGameMode
        if (isGameMode) {
            score = 0
            btnJuego.text = "Score: $score (Quit)"
            btnJuego.setBackgroundResource(R.drawable.bg_tecla_igual) // Color distintivo
            nextQuestion()
            Toast.makeText(this, "Game Mode: Solve the operations!", Toast.LENGTH_SHORT).show()
        } else {
            btnJuego.text = "Game"
            btnJuego.setBackgroundResource(R.drawable.bg_boton_juego)
            actualExpression = ""
            tvOperacion.text = ""
            tvResultado.text = "0"
            Toast.makeText(this, "Calculator Mode", Toast.LENGTH_SHORT).show()
        }
    }

    private fun nextQuestion() {
        actualExpression = ""
        tvResultado.text = "0"

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
            if (userResult == targetResult) {
                score++
                btnJuego.text = "Score: $score (Quit)"
                Toast.makeText(this, "Correct!", Toast.LENGTH_SHORT).show()
                nextQuestion()
            } else {
                Toast.makeText(this, "Incorrect. It was ${targetResult.toInt()}. Score reset.", Toast.LENGTH_LONG).show()
                score = 0
                btnJuego.text = "Score: $score (Quit)"
                nextQuestion()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Enter a valid number", Toast.LENGTH_SHORT).show()
        }
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
                    // Permitir signo negativo al inicio en el juego
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
            finish()
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
