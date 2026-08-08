package com.example.clendapp

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
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
                showConfirmationDialog(totalMillis)
            }
        }
    }

    private fun showConfirmationDialog(totalMillis: Long) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_confirm_start, null)
        val tvDesc = dialogView.findViewById<TextView>(R.id.tv_warning_desc)
        
        val fullText = "During your study session, phone access will be completely blocked. You won't be able to use apps, calls, or notifications until the scheduled time ends or during breaks."
        val target = "phone access will be completely blocked"
        val start = fullText.indexOf(target)
        
        if (start != -1) {
            val spannable = SpannableStringBuilder(fullText)
            val end = start + target.length
            spannable.setSpan(ForegroundColorSpan(Color.RED), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            spannable.setSpan(StyleSpan(Typeface.BOLD), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            tvDesc.text = spannable
        }

        val dialog = AlertDialog.Builder(requireContext(), R.style.CustomDialogTheme)
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
    }

    private fun startStudySession(totalMillis: Long) {
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
