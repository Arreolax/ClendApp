package com.example.clendapp

import android.content.Context
import android.os.Bundle
import android.util.TypedValue
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.clendapp.data.AppDatabase
import com.example.clendapp.data.Tasks
import com.example.clendapp.databinding.FragmentCalendarBinding
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

class CalendarFragment : Fragment() {

    private var _binding: FragmentCalendarBinding? = null
    private val binding get() = _binding!!

    private var displayedMonthDate: LocalDate = LocalDate.now()
    private var selectedDate: LocalDate = LocalDate.now()
    
    private lateinit var tasksAdapter: TasksAdapter
    private var tasksJob: Job? = null
    private var monthTasksJob: Job? = null
    private var tasksByDate: Map<LocalDate, List<Tasks>> = emptyMap()

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

        setupRecyclerView()
        setMonthView()
        observeMonthTasks()
        observeTasksForSelectedDate()

        binding.btnPrevMonth.setOnClickListener {
            displayedMonthDate = displayedMonthDate.minusMonths(1)
            setMonthView()
            observeMonthTasks()
        }

        binding.btnNextMonth.setOnClickListener {
            displayedMonthDate = displayedMonthDate.plusMonths(1)
            setMonthView()
            observeMonthTasks()
        }

        binding.fabAddTask.setOnClickListener {
            val dateMillis = selectedDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val addTaskSheet = AddTaskSheetFragment.newInstance(dateMillis)
            addTaskSheet.show(parentFragmentManager, AddTaskSheetFragment.TAG)
        }
    }

    private fun observeMonthTasks() {
        monthTasksJob?.cancel()
        
        val sharedPref = requireContext().getSharedPreferences("clend_app_prefs", Context.MODE_PRIVATE)
        val loggedInUserId = sharedPref.getInt("user_id", -1)
        if (loggedInUserId == -1) return
        
        val firstOfMonth = displayedMonthDate.withDayOfMonth(1)
        val dayOfWeek = firstOfMonth.dayOfWeek.value - 1
        
        val startDate = firstOfMonth.minusDays(dayOfWeek.toLong())
        val endDate = startDate.plusDays(41)

        val startMillis = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endMillis = endDate.atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        val database = AppDatabase.getDatabase(requireContext())
        monthTasksJob = lifecycleScope.launch {
            database.tasksDao().getTasksForDate(loggedInUserId, startMillis, endMillis).collect { tasks ->
                tasksByDate = tasks.groupBy { 
                    java.time.Instant.ofEpochMilli(it.date).atZone(ZoneId.systemDefault()).toLocalDate()
                }
                // Refresh the view to show dots
                setMonthView()
            }
        }
    }

    private fun setupRecyclerView() {
        val database = AppDatabase.getDatabase(requireContext())
        tasksAdapter = TasksAdapter(
            onTaskClick = { task ->
                Toast.makeText(requireContext(), "Task: ${task.title}", Toast.LENGTH_SHORT).show()
            },
            onEditClick = { task ->
                val editSheet = AddTaskSheetFragment.newEditInstance(task.id)
                editSheet.show(parentFragmentManager, AddTaskSheetFragment.TAG)
            },
            onDeleteClick = { task ->
                lifecycleScope.launch {
                    database.tasksDao().delete(task)
                    Toast.makeText(requireContext(), "Task deleted", Toast.LENGTH_SHORT).show()
                }
            },
            onDoneClick = { task ->
                lifecycleScope.launch {
                    val updatedTask = task.copy(isCompleted = !task.isCompleted)
                    database.tasksDao().update(updatedTask)
                    val message = if (updatedTask.isCompleted) "Task completed" else "Task pending"
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                }
            }
        )
        binding.rvCalendarTasks.apply {
            adapter = tasksAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun observeTasksForSelectedDate() {
        tasksJob?.cancel()
        
        val sharedPref = requireContext().getSharedPreferences("clend_app_prefs", Context.MODE_PRIVATE)
        val loggedInUserId = sharedPref.getInt("user_id", -1)
        if (loggedInUserId == -1) return
        
        val startOfDay = selectedDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endOfDay = selectedDate.atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        val database = AppDatabase.getDatabase(requireContext())
        tasksJob = lifecycleScope.launch {
            database.tasksDao().getTasksForDate(loggedInUserId, startOfDay, endOfDay).collect { tasks ->
                if (tasks.isEmpty()) {
                    binding.rvCalendarTasks.visibility = View.GONE
                    binding.tvEmptyTasks.visibility = View.VISIBLE
                } else {
                    binding.rvCalendarTasks.visibility = View.VISIBLE
                    binding.tvEmptyTasks.visibility = View.GONE
                    tasksAdapter.submitTasks(tasks)
                }
            }
        }
    }

    private fun showPopupMenu(view: View) {
        val popup = PopupMenu(requireContext(), view)
        popup.menu.add("Edit")
        popup.menu.add("Delete")
        popup.menu.add("Mark as done")
        popup.show()
    }

    private fun setMonthView() {
        binding.tvMonth.text = displayedMonthDate.month.getDisplayName(TextStyle.FULL, Locale.getDefault())
        binding.tvYear.text = displayedMonthDate.year.toString()

        val childCount = binding.calendarGrid.childCount
        if (childCount > 7) {
            binding.calendarGrid.removeViews(7, childCount - 7)
        }

        val daysInMonth = daysInMonthArray(displayedMonthDate)
        val currentYearMonth = YearMonth.from(displayedMonthDate)
        
        val firstOfMonth = displayedMonthDate.withDayOfMonth(1)
        val dayOfWeek = firstOfMonth.dayOfWeek.value - 1 
        
        val today = LocalDate.now()
        val inflater = LayoutInflater.from(requireContext())

        for (i in 0 until 42) {
            val dayText = daysInMonth[i]
            val isCurrentMonth = i in dayOfWeek until (dayOfWeek + currentYearMonth.lengthOfMonth())
            
            val dateOfThisDay = if (isCurrentMonth) {
                displayedMonthDate.withDayOfMonth(dayText.toInt())
            } else if (i < dayOfWeek) {
                displayedMonthDate.minusMonths(1).withDayOfMonth(dayText.toInt())
            } else {
                displayedMonthDate.plusMonths(1).withDayOfMonth(dayText.toInt())
            }

            val isSelected = dateOfThisDay == selectedDate
            val isToday = dateOfThisDay == today

            val styleRes = when {
                !isCurrentMonth -> R.style.CalendarDayNumberMuted
                isToday -> R.style.CalendarDayNumberActive
                else -> R.style.CalendarDayNumber
            }

            val dayView = inflater.inflate(R.layout.item_calendar_day, binding.calendarGrid, false)
            val tvDayNumber = dayView.findViewById<TextView>(R.id.tv_day_number)
            val layoutDots = dayView.findViewById<android.widget.LinearLayout>(R.id.layout_dots)

            tvDayNumber.text = dayText
            tvDayNumber.setTextAppearance(styleRes)

            val gridParams = GridLayout.LayoutParams()
            gridParams.width = 0
            gridParams.height = GridLayout.LayoutParams.WRAP_CONTENT
            gridParams.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
            dayView.layoutParams = gridParams

            if (isSelected) {
                tvDayNumber.setBackgroundResource(R.drawable.bg_day_selected)
                tvDayNumber.setTextColor(android.graphics.Color.BLACK)
            } else if (isToday) {
                tvDayNumber.setBackgroundResource(R.drawable.bg_day_active)
            }

            // Add dots based on tasks
            val tasksForDay: List<Tasks> = tasksByDate[dateOfThisDay] ?: emptyList()
            val dotCount = tasksForDay.size.coerceAtMost(3)
            
            for (j in 0 until dotCount) {
                val dot = View(requireContext())
                val dotSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4f, resources.displayMetrics).toInt()
                val dotParams = android.widget.LinearLayout.LayoutParams(dotSize, dotSize)
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
                layoutDots.addView(dot)
            }

            dayView.setOnClickListener {
                selectedDate = dateOfThisDay
                setMonthView()
                observeTasksForSelectedDate()
            }

            binding.calendarGrid.addView(dayView)
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
