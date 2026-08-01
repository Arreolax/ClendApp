package com.example.clendapp

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.clendapp.data.AppDatabase
import com.example.clendapp.data.Tasks
import com.example.clendapp.databinding.ItemDateHeaderBinding
import com.example.clendapp.databinding.ItemTaskCardBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class TasksAdapter(
    private val showHeaders: Boolean = true,
    private val onTaskClick: (Tasks) -> Unit,
    private val onEditClick: (Tasks) -> Unit,
    private val onDeleteClick: (Tasks) -> Unit,
    private val onDoneClick: (Tasks) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var allItems: List<TaskListItem> = emptyList()
    private var visibleItems: List<TaskListItem> = emptyList()
    private val collapsedDates = mutableSetOf<String>()
    private val lateHeaderDates = mutableSetOf<String>()

    sealed class TaskListItem {
        data class Header(val date: String, val isAllLate: Boolean) : TaskListItem()
        data class TaskItem(val task: Tasks, val headerDate: String) : TaskListItem()
    }

    override fun getItemViewType(position: Int): Int {
        return when (visibleItems[position]) {
            is TaskListItem.Header -> TYPE_HEADER
            is TaskListItem.TaskItem -> TYPE_TASK
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_HEADER) {
            HeaderViewHolder(ItemDateHeaderBinding.inflate(inflater, parent, false))
        } else {
            TaskViewHolder(ItemTaskCardBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = visibleItems[position]) {
            is TaskListItem.Header -> (holder as HeaderViewHolder).bind(item.date, item.isAllLate)
            is TaskListItem.TaskItem -> (holder as TaskViewHolder).bind(item.task)
        }
    }

    override fun getItemCount(): Int = visibleItems.size

    fun submitTasks(tasks: List<Tasks>) {
        val groupedList = mutableListOf<TaskListItem>()
        val formatter = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
        
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val tasksByDateStr = tasks.groupBy { formatter.format(Date(it.date)) }
        
        lateHeaderDates.clear()
        tasksByDateStr.forEach { (dateStr, dateTasks) ->
            val allLate = dateTasks.isNotEmpty() && dateTasks.all { it.late }
            if (allLate) {
                lateHeaderDates.add(dateStr)
            }
            
            val taskDate = dateTasks.first().date
            if (showHeaders && taskDate < today && allLate) {
                collapsedDates.add(dateStr)
            }
        }

        var lastDate = ""
        tasks.forEach { task ->
            val dateStr = formatter.format(Date(task.date))
            if (showHeaders && dateStr != lastDate) {
                val isAllLate = lateHeaderDates.contains(dateStr)
                groupedList.add(TaskListItem.Header(dateStr, isAllLate))
                lastDate = dateStr
            }
            groupedList.add(TaskListItem.TaskItem(task, dateStr))
        }
        allItems = groupedList
        updateVisibleItems()
    }

    private fun updateVisibleItems() {
        visibleItems = allItems.filter { item ->
            when (item) {
                is TaskListItem.Header -> true
                is TaskListItem.TaskItem -> !collapsedDates.contains(item.headerDate)
            }
        }
        notifyDataSetChanged()
    }

    inner class HeaderViewHolder(private val binding: ItemDateHeaderBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(date: String, isAllLate: Boolean) {
            binding.tvDateHeader.text = date
            
            binding.ivHeaderLateIndicator.visibility = if (isAllLate) android.view.View.VISIBLE else android.view.View.GONE
            
            val isCollapsed = collapsedDates.contains(date)
            binding.ivExpandCollapse.rotation = if (isCollapsed) 0f else 90f
            
            binding.root.setOnClickListener {
                if (isCollapsed) {
                    collapsedDates.remove(date)
                } else {
                    collapsedDates.add(date)
                }
                updateVisibleItems()
            }
        }
    }

    inner class TaskViewHolder(private val binding: ItemTaskCardBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(task: Tasks) {
            val isLate = task.late && !task.isCompleted
            
            binding.tvTaskTitle.text = task.title
            binding.tvTaskTitle.setTextColor(android.graphics.Color.parseColor("#2A2A2A"))

            when {
                task.isCompleted -> {
                    binding.root.setBackgroundResource(R.drawable.bg_section_outline_completed)
                    binding.tvTaskTime.setTextColor(android.graphics.Color.parseColor("#00C853"))
                    binding.ivStatusIcon.visibility = android.view.View.VISIBLE
                    binding.ivStatusIcon.setImageResource(R.drawable.ic_check)
                    binding.ivStatusIcon.imageTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#00C853"))
                }
                isLate -> {
                    binding.root.setBackgroundResource(R.drawable.bg_section_outline_late)
                    binding.tvTaskTime.setTextColor(android.graphics.Color.RED)
                    binding.ivStatusIcon.visibility = android.view.View.VISIBLE
                    binding.ivStatusIcon.setImageResource(R.drawable.ic_error)
                    binding.ivStatusIcon.imageTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.RED)
                }
                else -> {
                    binding.root.setBackgroundResource(R.drawable.bg_section_outline)
                    binding.tvTaskTime.setTextColor(android.graphics.Color.parseColor("#999999"))
                    binding.ivStatusIcon.visibility = android.view.View.VISIBLE
                    binding.ivStatusIcon.setImageResource(R.drawable.ic_clock)
                    binding.ivStatusIcon.imageTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#999999"))
                }
            }

            binding.tvTaskDescription.text = task.description ?: ""
            
            CoroutineScope(Dispatchers.IO).launch {
                val db = AppDatabase.getDatabase(binding.root.context)
                val category = db.categoriesDao().getCategoryById(task.id_category)
                withContext(Dispatchers.Main) {
                    binding.tvTaskCategory.text = category?.name ?: "Other"
                }
            }
            
            if (task.isCompleted) {
                binding.tvTaskTitle.paintFlags = binding.tvTaskTitle.paintFlags or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
                binding.root.alpha = 0.6f
            } else {
                binding.tvTaskTitle.paintFlags = binding.tvTaskTitle.paintFlags and android.graphics.Paint.STRIKE_THRU_TEXT_FLAG.inv()
                binding.root.alpha = 1.0f
            }

            val timeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())
            val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val timeStr = timeFormatter.format(Date(task.dueDate))
            val dateStr = dateFormatter.format(Date(task.date))
            
            binding.tvTaskTime.text = "$dateStr - $timeStr"

            binding.root.setOnClickListener { onTaskClick(task) }
            
            binding.btnTaskOptions.setOnClickListener { view ->
                val popup = android.widget.PopupMenu(view.context, view)
                val context = view.context
                if (task.isCompleted) {
                    popup.menu.add(context.getString(R.string.mark_as_pending))
                } else {
                    popup.menu.add(context.getString(R.string.mark_as_done))
                }
                popup.menu.add(context.getString(R.string.edit))
                popup.menu.add(context.getString(R.string.delete))
                
                popup.setOnMenuItemClickListener { menuItem ->
                    when (menuItem.title) {
                        context.getString(R.string.mark_as_done), 
                        context.getString(R.string.mark_as_pending) -> onDoneClick(task)
                        context.getString(R.string.edit) -> onEditClick(task)
                        context.getString(R.string.delete) -> onDeleteClick(task)
                    }
                    true
                }
                popup.show()
            }
            
            val color = when (task.id_category) {
                1 -> "#673AB7" // Purple
                2 -> "#4CAF50" // Green
                else -> "#2196F3" // Blue
            }
            binding.viewCategoryIndicator.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor(color))
        }
    }

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_TASK = 1
    }
}
