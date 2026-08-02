package com.example.clendapp

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.clendapp.data.AppDatabase
import com.example.clendapp.data.Tasks
import com.example.clendapp.databinding.FragmentTasksBinding
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.launch

class TasksFragment : Fragment() {

    private var _binding: FragmentTasksBinding? = null
    private val binding get() = _binding!!

    private lateinit var tasksAdapter: TasksAdapter
    private var allTasks: List<Tasks> = emptyList()
    private var isAscending: Boolean = true

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
        setupTabs()
        setupSortButton()
        observeTasks()
        setupSwipeRefresh()
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            observeTasks()
            binding.swipeRefresh.isRefreshing = false
        }
    }

    private fun setupSortButton() {
        binding.btnSort.setOnClickListener {
            isAscending = !isAscending
            // Rotar el icono para dar feedback visual (opcional)
            binding.btnSort.animate().rotation(if (isAscending) 0f else 180f).setDuration(300).start()
            filterAndSubmitTasks()
        }
    }

    private fun setupRecyclerView() {
        val database = AppDatabase.getDatabase(requireContext())
        tasksAdapter = TasksAdapter(
            showHeaders = false,
            onTaskClick = { task ->
                Toast.makeText(requireContext(), "Task: ${task.title}", Toast.LENGTH_SHORT).show()
            },
            onEditClick = { task ->
                val editSheet = AddTaskSheetFragment.newEditInstance(task.id)
                editSheet.show(parentFragmentManager, AddTaskSheetFragment.TAG)
            },
            onDeleteClick = { task ->
                viewLifecycleOwner.lifecycleScope.launch {
                    database.tasksDao().delete(task)
                    NotificationHelper.cancelTaskNotification(requireContext(), task.id)
                    Toast.makeText(requireContext(), "Task deleted", Toast.LENGTH_SHORT).show()
                }
            },
            onDoneClick = { task ->
                viewLifecycleOwner.lifecycleScope.launch {
                    val updatedTask = task.copy(isCompleted = !task.isCompleted)
                    database.tasksDao().update(updatedTask)
                    
                    if (updatedTask.isCompleted) {
                        NotificationHelper.cancelTaskNotification(requireContext(), task.id)
                    } else {
                        NotificationHelper.scheduleTaskNotification(requireContext(), updatedTask)
                    }

                    val message = if (updatedTask.isCompleted) "Task completed" else "Task pending"
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                }
            }
        )
        binding.rvTasks.apply {
            adapter = tasksAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun setupTabs() {
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                filterAndSubmitTasks()
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun observeTasks() {
        val sharedPref = requireContext().getSharedPreferences("clend_app_prefs", Context.MODE_PRIVATE)
        val loggedInUserId = sharedPref.getInt("user_id", -1)
        
        if (loggedInUserId == -1) return

        val database = AppDatabase.getDatabase(requireContext())
        viewLifecycleOwner.lifecycleScope.launch {
            // Update late status
            database.tasksDao().updateLateTasks(System.currentTimeMillis())

            database.tasksDao().getAllTasks(loggedInUserId).collect { tasks ->
                if (_binding == null) return@collect
                allTasks = tasks
                filterAndSubmitTasks()
            }
        }
    }

    private fun filterAndSubmitTasks() {
        val isCompletedTab = binding.tabLayout.selectedTabPosition == 1
        var filteredTasks = if (isCompletedTab) {
            allTasks.filter { it.isCompleted }
        } else {
            allTasks.filter { !it.isCompleted }
        }

        // Aplicar ordenamiento
        filteredTasks = if (isAscending) {
            filteredTasks.sortedWith(compareBy({ it.date }, { it.dueDate }))
        } else {
            filteredTasks.sortedWith(compareByDescending<Tasks> { it.date }.thenByDescending { it.dueDate })
        }

        if (filteredTasks.isEmpty()) {
            binding.rvTasks.visibility = View.GONE
            binding.layoutEmptyState.visibility = View.VISIBLE
            binding.tvEmptyMessage.text = if (isCompletedTab) "No completed tasks" else "No pending tasks"
        } else {
            binding.rvTasks.visibility = View.VISIBLE
            binding.layoutEmptyState.visibility = View.GONE
            tasksAdapter.submitTasks(filteredTasks)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
