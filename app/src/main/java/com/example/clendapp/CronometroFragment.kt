package com.example.clendapp

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.clendapp.databinding.FragmentCronometroBinding

class CronometroFragment : Fragment() {

    private var _binding: FragmentCronometroBinding? = null
    private val binding get() = _binding!!

    private var hours = 0
    private var minutes = 25
    private var seconds = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCronometroBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        updateDisplay()

        // Horas
        binding.btnHhUp.setOnClickListener {
            hours = (hours + 1) % 100
            updateDisplay()
        }
        binding.btnHhDown.setOnClickListener {
            hours = if (hours > 0) hours - 1 else 99
            updateDisplay()
        }

        // Minutos
        binding.btnMmUp.setOnClickListener {
            minutes = (minutes + 1) % 60
            updateDisplay()
        }
        binding.btnMmDown.setOnClickListener {
            minutes = if (minutes > 0) minutes - 1 else 59
            updateDisplay()
        }

        // Segundos
        binding.btnSsUp.setOnClickListener {
            seconds = (seconds + 1) % 60
            updateDisplay()
        }
        binding.btnSsDown.setOnClickListener {
            seconds = if (seconds > 0) seconds - 1 else 59
            updateDisplay()
        }

        // Presets
        binding.btn20m.setOnClickListener {
            hours = 0
            minutes = 20
            seconds = 0
            updateDisplay()
        }
        binding.btn60m.setOnClickListener {
            hours = 1
            minutes = 0
            seconds = 0
            updateDisplay()
        }
        binding.btn90m.setOnClickListener {
            hours = 1
            minutes = 30
            seconds = 0
            updateDisplay()
        }

        binding.btnStart.setOnClickListener {
            val totalMillis = ((hours * 3600L) + (minutes * 60L) + seconds) * 1000L
            if (totalMillis > 0) {
                val layouts = listOf(
                    R.layout.activity_prueba,
                    R.layout.fondo2,
                    R.layout.fondo3,
                    R.layout.fondo4
                )
                val randomLayout = layouts.random()

                val intent = Intent(requireContext(), prueba::class.java)
                intent.putExtra("TOTAL_TIME_MILLIS", totalMillis)
                intent.putExtra("LAYOUT_RES_ID", randomLayout)
                startActivity(intent)
            }
        }
    }

    private fun updateDisplay() {
        val locale = java.util.Locale.getDefault()
        binding.tvHh.text = String.format(locale, "%02d", hours)
        binding.tvMm.text = String.format(locale, "%02d", minutes)
        binding.tvSs.text = String.format(locale, "%02d", seconds)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
