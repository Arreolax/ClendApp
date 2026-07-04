package com.example.clendapp

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.clendapp.databinding.FragmentHomeBinding
import java.text.SimpleDateFormat
import java.util.*

class InicioFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        updateGreeting()
        setupCalendar()
    }

    private fun updateGreeting() {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        
        val greeting = when (hour) {
            in 0..11 -> "Good Morning!!"
            in 12..18 -> "Good Afternoon!!"
            else -> "Good Evening!!"
        }
        binding.tvGreeting.text = greeting
    }

    private fun setupCalendar() {
        val calendar = Calendar.getInstance()
        val currentDayOfYear = calendar.get(Calendar.DAY_OF_YEAR)
        val currentYear = calendar.get(Calendar.YEAR)
        
        val dayFormat = SimpleDateFormat("EE", Locale.getDefault())
        val calendarRow = binding.calendarRow
        
        // Retrocedemos 3 días para centrar hoy
        calendar.add(Calendar.DAY_OF_YEAR, -3)
        
        for (i in 0 until calendarRow.childCount) {
            val dayContainer = calendarRow.getChildAt(i) as? LinearLayout
            if (dayContainer != null) {
                val tvDayNum = dayContainer.getChildAt(0) as? TextView
                val tvDayName = dayContainer.getChildAt(1) as? TextView
                
                val dayNum = calendar.get(Calendar.DAY_OF_MONTH)
                val dayName = dayFormat.format(calendar.time)
                
                tvDayNum?.text = dayNum.toString()
                tvDayName?.text = dayName

                if (calendar.get(Calendar.DAY_OF_YEAR) == currentDayOfYear && 
                    calendar.get(Calendar.YEAR) == currentYear) {
                    // Hoy
                    dayContainer.setBackgroundResource(R.drawable.bg_day_active)
                    dayContainer.layoutParams = (dayContainer.layoutParams as LinearLayout.LayoutParams).apply {
                        height = (80 * resources.displayMetrics.density).toInt()
                        weight = 1.2f
                    }
                    tvDayNum?.textSize = 22f
                    tvDayNum?.setTextColor(Color.parseColor("#6B9CFF"))
                    tvDayName?.textSize = 14f
                    tvDayName?.setTextColor(Color.parseColor("#6B9CFF"))
                } else {
                    // Otros días
                    dayContainer.background = null
                    dayContainer.layoutParams = (dayContainer.layoutParams as LinearLayout.LayoutParams).apply {
                        height = LinearLayout.LayoutParams.WRAP_CONTENT
                        weight = 1.0f
                    }
                    tvDayNum?.textSize = 18f
                    tvDayNum?.setTextColor(Color.parseColor("#2A2A2A"))
                    tvDayName?.textSize = 12f
                    tvDayName?.setTextColor(Color.parseColor("#999999"))
                }
            }
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
