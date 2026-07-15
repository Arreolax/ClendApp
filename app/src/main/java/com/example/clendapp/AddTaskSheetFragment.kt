package com.example.clendapp

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.example.clendapp.data.AppDatabase
import com.example.clendapp.data.Tasks
import com.example.clendapp.databinding.BottomSheetAddTaskBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
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

        binding.btnCreateTask.setOnClickListener {
            saveTask()
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
        val categories = listOf(binding.category1, binding.category2, binding.category3)
        
        categories.forEachIndexed { index, textView ->
            textView.setOnClickListener {
                selectedCategory = index + 1
                // Visual feedback (optional but recommended)
                categories.forEach { it.alpha = 0.5f }
                textView.alpha = 1.0f
            }
        }
        // Default selection
        binding.category1.alpha = 1.0f
        binding.category2.alpha = 0.5f
        binding.category3.alpha = 0.5f
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

        val database = AppDatabase.getDatabase(requireContext())
        val newTask = Tasks(
            title = title,
            description = note,
            category = selectedCategory,
            date = selectedDate.timeInMillis,
            startDate = finalStart.timeInMillis,
            dueDate = finalEnd.timeInMillis,
            repeat = binding.switchRepeat.isChecked
        )

        lifecycleScope.launch {
            database.tasksDao().insert(newTask)
            Toast.makeText(requireContext(), "Task created successfully", Toast.LENGTH_SHORT).show()
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "AddTaskSheetFragment"
    }
}
