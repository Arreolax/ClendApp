package com.example.clendapp

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.clendapp.data.AppDatabase
import com.example.clendapp.data.Tasks
import com.example.clendapp.databinding.FragmentHomeBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.*

class InicioFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var remindersAdapter: ReminderAdapter

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
        loadUserScores()
        setupCalendar()
        setupRecyclerView()
        observeReminders()
        setupNotesCard()
        setupSwipeRefresh()
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            updateGreeting()
            loadUserScores()
            setupCalendar()
            observeReminders()
            setupNotesCard()
            
            // Finalizar animación después de un pequeño retraso
            binding.swipeRefresh.isRefreshing = false
        }
    }

    private fun setupNotesCard() {
        binding.cardNotesResume.setOnClickListener {
            val intent = Intent(requireContext(), MisNotas::class.java)
            startActivity(intent)
        }
        
        // Cargar conteo de notas
        NoteManager.loadNotes(requireContext())
        val count = NoteManager.getNotes().size
        if (count > 1) {
            binding.tvNotesCount.text = count.toString()
            binding.tvNotesCount.visibility = View.VISIBLE
        } else {
            binding.tvNotesCount.visibility = View.GONE
        }
    }

    private fun loadUserScores() {
        val sharedPref = requireContext().getSharedPreferences("clend_app_prefs", Context.MODE_PRIVATE)
        val userId = sharedPref.getInt("user_id", -1)

        if (userId != -1) {
            val database = AppDatabase.getDatabase(requireContext())
            viewLifecycleOwner.lifecycleScope.launch {
                database.scoresDao().getScoresFlowByUserId(userId).collect { scores ->
                    if (_binding == null) return@collect
                    
                    scores?.let {
                        binding.tvResumeCalculator.text = "${it.calculator_score} points"
                        binding.tvResumeStudy.text = "${it.study_score} points"
                        
                        val rank = database.ranksDao().getRankById(it.id_rank)
                        binding.tvResumeRank.text = rank?.name ?: "Potato"
                    }
                }
            }
        }
    }

    private fun setupRecyclerView() {
        remindersAdapter = ReminderAdapter(
            onTaskClick = { _ ->
                activity?.findViewById<BottomNavigationView>(R.id.bottom_navigation)?.selectedItemId = R.id.nav_check
            }
        )
        binding.rvReminders.apply {
            adapter = remindersAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun observeReminders() {
        val sharedPref = requireContext().getSharedPreferences("clend_app_prefs", Context.MODE_PRIVATE)
        val loggedInUserId = sharedPref.getInt("user_id", -1)
        if (loggedInUserId == -1) return

        val today = LocalDate.now()
        val startOfDay = today.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endOfDay = today.atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        val database = AppDatabase.getDatabase(requireContext())
        viewLifecycleOwner.lifecycleScope.launch {
            database.tasksDao().updateLateTasks(System.currentTimeMillis())

            database.tasksDao().getTasksForDate(loggedInUserId, startOfDay, endOfDay).collect { tasks ->
                if (_binding == null) return@collect
                val pendingTasks = tasks.filter { !it.isCompleted }
                if (pendingTasks.isEmpty()) {
                    binding.rvReminders.visibility = View.GONE
                    binding.tvEmptyReminders.visibility = View.VISIBLE
                } else {
                    binding.rvReminders.visibility = View.VISIBLE
                    binding.tvEmptyReminders.visibility = View.GONE
                    remindersAdapter.submitTasks(pendingTasks)
                }
            }
        }
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
        
        val rangeCalendar = calendar.clone() as Calendar
        rangeCalendar.add(Calendar.DAY_OF_YEAR, -3)
        val startDateMillis = rangeCalendar.timeInMillis
        rangeCalendar.add(Calendar.DAY_OF_YEAR, 6)
        val endDateMillis = rangeCalendar.timeInMillis

        drawCalendarBase(currentDayOfYear, currentYear, dayFormat)

        val sharedPref = requireContext().getSharedPreferences("clend_app_prefs", Context.MODE_PRIVATE)
        val loggedInUserId = sharedPref.getInt("user_id", -1)

        if (loggedInUserId != -1) {
            val database = AppDatabase.getDatabase(requireContext())
            viewLifecycleOwner.lifecycleScope.launch {
                database.tasksDao().getTasksForDate(loggedInUserId, startDateMillis, endDateMillis).collect { tasks ->
                    if (_binding == null) return@collect
                    val tasksByDate = tasks.groupBy { 
                        java.time.Instant.ofEpochMilli(it.date).atZone(ZoneId.systemDefault()).toLocalDate()
                    }
                    updateCalendarDots(tasksByDate)
                }
            }
        }
    }

    private fun drawCalendarBase(currentDayOfYear: Int, currentYear: Int, dayFormat: SimpleDateFormat) {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -3)
        
        if (_binding == null) return
        
        for (i in 0 until binding.calendarRow.childCount) {
            val dayContainer = binding.calendarRow.getChildAt(i) as? LinearLayout
            if (dayContainer != null) {
                val tvDayNum = dayContainer.getChildAt(0) as? TextView
                val tvDayName = dayContainer.getChildAt(1) as? TextView
                
                val dayNum = calendar.get(Calendar.DAY_OF_MONTH)
                val dayName = dayFormat.format(calendar.time)
                
                tvDayNum?.text = dayNum.toString()
                tvDayName?.text = dayName

                if (calendar.get(Calendar.DAY_OF_YEAR) == currentDayOfYear && 
                    calendar.get(Calendar.YEAR) == currentYear) {
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

    private fun updateCalendarDots(tasksByDate: Map<LocalDate, List<Tasks>>) {
        if (_binding == null || context == null) return
        
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -3)
        
        for (i in 0 until binding.calendarRow.childCount) {
            val dayContainer = binding.calendarRow.getChildAt(i) as? LinearLayout
            val layoutDots = dayContainer?.getChildAt(2) as? LinearLayout
            
            layoutDots?.removeAllViews()
            
            val dateOfThisDay = java.time.Instant.ofEpochMilli(calendar.timeInMillis).atZone(ZoneId.systemDefault()).toLocalDate()
            val tasksForDay = tasksByDate[dateOfThisDay] ?: emptyList()
            val dotCount = tasksForDay.size.coerceAtMost(3)
            
            for (j in 0 until dotCount) {
                val dot = View(context)
                val dotSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4f, resources.displayMetrics).toInt()
                val dotParams = LinearLayout.LayoutParams(dotSize, dotSize)
                dotParams.setMargins(2, 0, 2, 0)
                dot.layoutParams = dotParams
                
                val currentTask = tasksForDay[j]
                val colorHex = when (currentTask.id_category) {
                    1 -> "#673AB7" // Purple
                    2 -> "#4CAF50" // Green
                    else -> "#2196F3" // Blue
                }
                
                val shape = android.graphics.drawable.GradientDrawable().apply {
                    shape = android.graphics.drawable.GradientDrawable.OVAL
                    setColor(android.graphics.Color.parseColor(colorHex))
                }
                dot.background = shape
                layoutDots?.addView(dot)
            }
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
