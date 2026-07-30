package com.example.clendapp

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.example.clendapp.data.AppDatabase
import com.example.clendapp.data.Tasks
import com.example.clendapp.databinding.BottomSheetAddTaskBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class AddTaskSheetFragment : BottomSheetDialogFragment() {

    private var _binding: BottomSheetAddTaskBinding? = null
    private val binding get() = _binding!!

    private var selectedDate: Calendar = Calendar.getInstance()
    private var startTime: Calendar = Calendar.getInstance()
    private var endTime: Calendar = Calendar.getInstance()
    private var selectedCategory: Int = 1
    private var editingTaskId: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            if (it.containsKey(ARG_TASK_ID)) {
                editingTaskId = it.getInt(ARG_TASK_ID)
            }
            if (it.containsKey(ARG_DATE)) {
                val dateMillis = it.getLong(ARG_DATE)
                selectedDate.timeInMillis = dateMillis
                startTime.timeInMillis = dateMillis
                endTime.timeInMillis = dateMillis
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetAddTaskBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupDateTimePickers()
        setupCategorySelection()
        
        if (editingTaskId != null) {
            loadTaskData(editingTaskId!!)
        } else {
            binding.btnCreateTask.text = "Create Task"
            binding.btnCreateTask.setTextColor(android.graphics.Color.WHITE)
            updateUI()
        }

        binding.btnCreateTask.setOnClickListener {
            saveTask()
        }
    }

    private fun loadTaskData(taskId: Int) {
        val database = AppDatabase.getDatabase(requireContext())
        lifecycleScope.launch {
            val task = database.tasksDao().getTask(taskId).first()
            task?.let {
                binding.etTaskName.setText(it.title)
                binding.etTaskNote.setText(it.description)
                selectedDate.timeInMillis = it.date
                startTime.timeInMillis = it.startDate
                endTime.timeInMillis = it.dueDate
                selectedCategory = it.id_category
                binding.switchRepeat.isChecked = it.repeat
                
                binding.btnCreateTask.text = "Update Task"
                updateUI()
            }
        }
    }

    private fun updateUI() {
        val dateFormatter = SimpleDateFormat("dd MMM, yyyy", Locale.getDefault())
        val timeFormatter = SimpleDateFormat("hh:mm a", Locale.getDefault())
        binding.tvDate.text = dateFormatter.format(selectedDate.time)
        binding.tvStartTime.text = timeFormatter.format(startTime.time)
        binding.tvEndTime.text = timeFormatter.format(endTime.time)
        
        updateCategorySelectionVisuals()
    }

    private fun updateCategorySelectionVisuals() {
        for (i in 0 until binding.layoutCategories.childCount) {
            val child = binding.layoutCategories.getChildAt(i)
            val categoryId = child.tag as? Int
            child.alpha = if (selectedCategory == categoryId) 1.0f else 0.5f
        }
    }

    private fun setupDateTimePickers() {
        val dateFormatter = SimpleDateFormat("dd MMM, yyyy", Locale.getDefault())
        val timeFormatter = SimpleDateFormat("hh:mm a", Locale.getDefault())

        binding.tvDate.text = dateFormatter.format(selectedDate.time)
        binding.tvStartTime.text = timeFormatter.format(startTime.time)
        binding.tvEndTime.text = timeFormatter.format(endTime.time)

        binding.layoutDate.setOnClickListener {
            val datePickerDialog = DatePickerDialog(
                requireContext(),
                { _, year, month, dayOfMonth ->
                    selectedDate.set(year, month, dayOfMonth)
                    binding.tvDate.text = dateFormatter.format(selectedDate.time)
                },
                selectedDate.get(Calendar.YEAR),
                selectedDate.get(Calendar.MONTH),
                selectedDate.get(Calendar.DAY_OF_MONTH)
            )
            datePickerDialog.show()
        }

        binding.layoutStartTime.setOnClickListener {
            val timePickerDialog = TimePickerDialog(
                requireContext(),
                { _, hourOfDay, minute ->
                    startTime.set(Calendar.HOUR_OF_DAY, hourOfDay)
                    startTime.set(Calendar.MINUTE, minute)
                    binding.tvStartTime.text = timeFormatter.format(startTime.time)
                },
                startTime.get(Calendar.HOUR_OF_DAY),
                startTime.get(Calendar.MINUTE),
                false
            )
            timePickerDialog.show()
        }

        binding.layoutEndTime.setOnClickListener {
            val timePickerDialog = TimePickerDialog(
                requireContext(),
                { _, hourOfDay, minute ->
                    endTime.set(Calendar.HOUR_OF_DAY, hourOfDay)
                    endTime.set(Calendar.MINUTE, minute)
                    binding.tvEndTime.text = timeFormatter.format(endTime.time)
                },
                endTime.get(Calendar.HOUR_OF_DAY),
                endTime.get(Calendar.MINUTE),
                false
            )
            timePickerDialog.show()
        }
    }

    private fun setupCategorySelection() {
        val database = AppDatabase.getDatabase(requireContext())
        lifecycleScope.launch {
            val categories = database.categoriesDao().getAll()
            binding.layoutCategories.removeAllViews()
            
            categories.forEach { category ->
                val categoryView = LayoutInflater.from(requireContext())
                    .inflate(R.layout.item_category_chip, binding.layoutCategories, false) as TextView
                
                categoryView.text = category.name
                categoryView.tag = category.id // Guardar ID para saber cuál está seleccionado
                
                // Color y fondo según el ID
                val (bgRes, colorHex) = when (category.id % 3) {
                    1 -> Pair(R.drawable.bg_category_purple, "#673AB7")
                    2 -> Pair(R.drawable.bg_category_green, "#4CAF50")
                    else -> Pair(R.drawable.bg_category_blue, "#2196F3")
                }
                
                categoryView.setBackgroundResource(bgRes)
                val colorInt = android.graphics.Color.parseColor(colorHex)
                categoryView.setTextColor(colorInt)
                categoryView.compoundDrawableTintList = android.content.res.ColorStateList.valueOf(colorInt)

                categoryView.alpha = if (selectedCategory == category.id) 1.0f else 0.5f
                
                categoryView.setOnClickListener {
                    selectedCategory = category.id
                    // Actualizar opacidad de todos los hijos
                    for (i in 0 until binding.layoutCategories.childCount) {
                        binding.layoutCategories.getChildAt(i).alpha = 0.5f
                    }
                    categoryView.alpha = 1.0f
                }
                
                binding.layoutCategories.addView(categoryView)
            }
        }
    }

    private fun saveTask() {
        val title = binding.etTaskName.text.toString()
        val note = binding.etTaskNote.text.toString()

        if (title.isBlank()) {
            Toast.makeText(requireContext(), "Please enter a task name", Toast.LENGTH_SHORT).show()
            return
        }

        // Combinar fecha seleccionada con horas de inicio/fin
        val finalStart = Calendar.getInstance().apply {
            timeInMillis = selectedDate.timeInMillis
            set(Calendar.HOUR_OF_DAY, startTime.get(Calendar.HOUR_OF_DAY))
            set(Calendar.MINUTE, startTime.get(Calendar.MINUTE))
        }

        val finalEnd = Calendar.getInstance().apply {
            timeInMillis = selectedDate.timeInMillis
            set(Calendar.HOUR_OF_DAY, endTime.get(Calendar.HOUR_OF_DAY))
            set(Calendar.MINUTE, endTime.get(Calendar.MINUTE))
        }

        // Validación: la fecha/hora inicial no debe ser mayor que la final
        if (finalStart.after(finalEnd)) {
            Toast.makeText(requireContext(), "La hora de inicio no puede ser posterior a la de fin", Toast.LENGTH_SHORT).show()
            return
        }

        val database = AppDatabase.getDatabase(requireContext())
        
        // Obtener el ID del usuario logeado desde SharedPreferences
        val sharedPref = requireContext().getSharedPreferences("clend_app_prefs", Context.MODE_PRIVATE)
        val loggedInUserId = sharedPref.getInt("user_id", -1)

        if (loggedInUserId == -1) {
            Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Normalizar la fecha al inicio del día (00:00:00) para facilitar el agrupamiento y búsqueda
        val normalizedDate = (selectedDate.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val newTask = Tasks(
            id = editingTaskId ?: 0,
            title = title,
            description = note,
            id_category = selectedCategory,
            date = normalizedDate.timeInMillis,
            startDate = finalStart.timeInMillis,
            dueDate = finalEnd.timeInMillis,
            repeat = binding.switchRepeat.isChecked,
            id_user = loggedInUserId
        )

        lifecycleScope.launch {
            if (editingTaskId == null) {
                database.tasksDao().insert(newTask)
            } else {
                database.tasksDao().update(newTask)
            }
            Toast.makeText(requireContext(), if (editingTaskId == null) "Task created successfully" else "Task updated successfully", Toast.LENGTH_SHORT).show()
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "AddTaskSheetFragment"
        private const val ARG_DATE = "arg_date"
        private const val ARG_TASK_ID = "arg_task_id"

        fun newInstance(dateMillis: Long): AddTaskSheetFragment {
            return AddTaskSheetFragment().apply {
                arguments = Bundle().apply {
                    putLong(ARG_DATE, dateMillis)
                }
            }
        }

        fun newEditInstance(taskId: Int): AddTaskSheetFragment {
            return AddTaskSheetFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_TASK_ID, taskId)
                }
            }
        }
    }
}
