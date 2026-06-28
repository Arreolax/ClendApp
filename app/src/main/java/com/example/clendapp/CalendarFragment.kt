package com.example.clendapp

import android.os.Bundle
import android.util.TypedValue
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.clendapp.databinding.FragmentCalendarBinding
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

class CalendarFragment : Fragment() {

    private var _binding: FragmentCalendarBinding? = null
    private val binding get() = _binding!!

    private var selectedDate: LocalDate = LocalDate.now()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCalendarBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setMonthView()

        binding.btnPrevMonth.setOnClickListener {
            selectedDate = selectedDate.minusMonths(1)
            setMonthView()
        }

        binding.btnNextMonth.setOnClickListener {
            selectedDate = selectedDate.plusMonths(1)
            setMonthView()
        }
    }

    private fun setMonthView() {
        binding.tvMonth.text = selectedDate.month.getDisplayName(TextStyle.FULL, Locale.getDefault())
        binding.tvYear.text = selectedDate.year.toString()

        // Limpiar días anteriores del GridLayout (manteniendo los encabezados de los días de la semana)
        val childCount = binding.calendarGrid.childCount
        if (childCount > 7) {
            binding.calendarGrid.removeViews(7, childCount - 7)
        }

        val daysInMonth = daysInMonthArray(selectedDate)
        val currentYearMonth = YearMonth.from(selectedDate)
        
        val firstOfMonth = selectedDate.withDayOfMonth(1)
        val dayOfWeek = firstOfMonth.dayOfWeek.value - 1 // 0 (Lun) a 6 (Dom)
        
        val today = LocalDate.now()

        for (i in 0 until 42) {
            val dayText = daysInMonth[i]
            
            val isCurrentMonth = i in dayOfWeek until (dayOfWeek + currentYearMonth.lengthOfMonth())
            val isToday = isCurrentMonth && 
                          dayText.toInt() == today.dayOfMonth && 
                          selectedDate.month == today.month && 
                          selectedDate.year == today.year

            val styleRes = when {
                !isCurrentMonth -> R.style.CalendarDayNumberMuted
                isToday -> R.style.CalendarDayNumberActive
                else -> R.style.CalendarDayNumber
            }

            val contextWrapper = ContextThemeWrapper(requireContext(), styleRes)
            val textView = TextView(contextWrapper, null, 0)
            textView.text = dayText
            textView.gravity = Gravity.CENTER

            val gridParams = GridLayout.LayoutParams()
            gridParams.width = 0
            gridParams.height = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 48f, resources.displayMetrics).toInt()
            gridParams.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)

            if (isToday) {
                val frameLayout = FrameLayout(requireContext())
                frameLayout.layoutParams = gridParams
                
                textView.setBackgroundResource(R.drawable.bg_day_active)
                val textParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
                // Añadir margen para que el círculo no toque los bordes
                val margin = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4f, resources.displayMetrics).toInt()
                textParams.setMargins(margin, margin, margin, margin)
                textView.layoutParams = textParams
                
                frameLayout.addView(textView)
                binding.calendarGrid.addView(frameLayout)
            } else {
                textView.layoutParams = gridParams
                binding.calendarGrid.addView(textView)
            }
        }
    }

    private fun daysInMonthArray(date: LocalDate): ArrayList<String> {
        val daysInMonthArray = ArrayList<String>()
        val yearMonth = YearMonth.from(date)

        val daysInMonth = yearMonth.lengthOfMonth()
        val firstOfMonth = date.withDayOfMonth(1)
        val dayOfWeek = firstOfMonth.dayOfWeek.value - 1

        val prevMonth = yearMonth.minusMonths(1)
        val daysInPrevMonth = prevMonth.lengthOfMonth()

        for (i in 1..42) {
            if (i <= dayOfWeek) {
                daysInMonthArray.add((daysInPrevMonth - dayOfWeek + i).toString())
            } else if (i > daysInMonth + dayOfWeek) {
                daysInMonthArray.add((i - daysInMonth - dayOfWeek).toString())
            } else {
                daysInMonthArray.add((i - dayOfWeek).toString())
            }
        }
        return daysInMonthArray
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
