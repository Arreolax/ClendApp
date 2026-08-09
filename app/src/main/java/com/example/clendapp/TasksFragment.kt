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
import com.example.clendapp.data.StreakManager
import com.example.clendapp.data.Tasks
import com.example.clendapp.databinding.FragmentTasksBinding
import com.google.android.material.snackbar.Snackbar
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
        setupAddTaskButton()
        observeTasks()
        setupSwipeRefresh()
    }

    private fun setupAddTaskButton() {
        binding.fabAddTask.setOnClickListener {
            val addSheet = AddTaskSheetFragment.newInstance(System.currentTimeMillis())
            addSheet.show(parentFragmentManager, AddTaskSheetFragment.TAG)
        }
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
                showDeleteConfirmationDialog(task)
            },
            onDoneClick = { task ->
                viewLifecycleOwner.lifecycleScope.launch {
                    val updatedTask = task.copy(isCompleted = !task.isCompleted)
                    database.tasksDao().update(updatedTask)

                    if (updatedTask.isCompleted) {
                        val points = StreakManager.completeActivity(requireContext())
                        if (points > 0) {
                            Snackbar.make(binding.root, getString(R.string.points_congrats_generic, points), Snackbar.LENGTH_LONG).show()
                        }
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
                updateTabTitles()
                filterAndSubmitTasks()
            }
        }
    }

    private fun updateTabTitles() {
        val pendingCount = allTasks.count { !it.isCompleted }
        val completedCount = allTasks.count { it.isCompleted }

        binding.tabLayout.getTabAt(0)?.text = if (pendingCount > 0) {
            "Pending ($pendingCount)"
        } else {
            "Pending"
        }

        binding.tabLayout.getTabAt(1)?.text = if (completedCount > 0) {
            "Completed ($completedCount)"
        } else {
            "Completed"
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

    private fun showDeleteConfirmationDialog(task: Tasks) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_calculadora_game, null)
        val dialog = androidx.appcompat.app.AlertDialog.Builder(requireContext(), R.style.CustomDialogTheme)
            .setView(dialogView)
            .create()

        dialogView.findViewById<android.widget.TextView>(R.id.tv_dialog_title).text = "Delete Task?"
        dialogView.findViewById<android.widget.TextView>(R.id.tv_dialog_message).text = "Are you sure you want to delete this task? This action cannot be undone."
        
        val btnConfirm = dialogView.findViewById<android.widget.TextView>(R.id.btn_dialog_accept)
        val btnCancel = dialogView.findViewById<android.widget.TextView>(R.id.btn_dialog_cancel)
        
        btnConfirm.text = "Delete"
        btnCancel.text = "Cancel"
        btnCancel.visibility = View.VISIBLE

        btnConfirm.setOnClickListener {
            val database = AppDatabase.getDatabase(requireContext())
            viewLifecycleOwner.lifecycleScope.launch {
                database.tasksDao().delete(task)
                NotificationHelper.cancelTaskNotification(requireContext(), task.id)
                dialog.dismiss()
                Toast.makeText(requireContext(), "Task deleted", Toast.LENGTH_SHORT).show()
            }
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.85).toInt(),
            android.view.WindowManager.LayoutParams.WRAP_CONTENT
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
