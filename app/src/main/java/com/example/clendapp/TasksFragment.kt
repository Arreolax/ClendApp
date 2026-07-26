package com.example.clendapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.clendapp.data.AppDatabase
import com.example.clendapp.databinding.FragmentTasksBinding
import kotlinx.coroutines.launch

class TasksFragment : Fragment() {

    private var _binding: FragmentTasksBinding? = null
    private val binding get() = _binding!!

    private lateinit var tasksAdapter: TasksAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTasksBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        observeTasks()
    }

    private fun setupRecyclerView() {
        val database = AppDatabase.getDatabase(requireContext())
        tasksAdapter = TasksAdapter(
            onTaskClick = { task ->
                Toast.makeText(requireContext(), "Tarea: ${task.title}", Toast.LENGTH_SHORT).show()
            },
            onEditClick = { task ->
                val editSheet = AddTaskSheetFragment.newEditInstance(task.id)
                editSheet.show(parentFragmentManager, AddTaskSheetFragment.TAG)
            },
            onDeleteClick = { task ->
                lifecycleScope.launch {
                    database.tasksDao().delete(task)
                    Toast.makeText(requireContext(), "Tarea eliminada", Toast.LENGTH_SHORT).show()
                }
            },
            onDoneClick = { task ->
                lifecycleScope.launch {
                    val updatedTask = task.copy(isCompleted = !task.isCompleted)
                    database.tasksDao().update(updatedTask)
                    val message = if (updatedTask.isCompleted) "Tarea completada" else "Tarea pendiente"
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                }
            }
        )
        binding.rvTasks.apply {
            adapter = tasksAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun observeTasks() {
        val database = AppDatabase.getDatabase(requireContext())
        lifecycleScope.launch {
            database.tasksDao().getAllTasks().collect { tasks ->
                if (tasks.isEmpty()) {
                    binding.rvTasks.visibility = View.GONE
                    binding.layoutEmptyState.visibility = View.VISIBLE
                } else {
                    binding.rvTasks.visibility = View.VISIBLE
                    binding.layoutEmptyState.visibility = View.GONE
                    tasksAdapter.submitTasks(tasks)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
