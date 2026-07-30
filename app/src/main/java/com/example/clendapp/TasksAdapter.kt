package com.example.clendapp

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.clendapp.data.Tasks
import com.example.clendapp.databinding.ItemDateHeaderBinding
import com.example.clendapp.databinding.ItemTaskCardBinding
import java.text.SimpleDateFormat
import java.util.*

class TasksAdapter(
    private val onTaskClick: (Tasks) -> Unit,
    private val onEditClick: (Tasks) -> Unit,
    private val onDeleteClick: (Tasks) -> Unit,
    private val onDoneClick: (Tasks) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var items: List<TaskListItem> = emptyList()

    sealed class TaskListItem {
        data class Header(val date: String) : TaskListItem()
        data class TaskItem(val task: Tasks) : TaskListItem()
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
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
        when (val item = items[position]) {
            is TaskListItem.Header -> (holder as HeaderViewHolder).bind(item.date)
            is TaskListItem.TaskItem -> (holder as TaskViewHolder).bind(item.task)
        }
    }

    override fun getItemCount(): Int = items.size

    fun submitTasks(tasks: List<Tasks>) {
        val groupedList = mutableListOf<TaskListItem>()
        val formatter = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
        
        var lastDate = ""
        tasks.forEach { task ->
            val dateStr = formatter.format(Date(task.date))
            if (dateStr != lastDate) {
                groupedList.add(TaskListItem.Header(dateStr))
                lastDate = dateStr
            }
            groupedList.add(TaskListItem.TaskItem(task))
        }
        items = groupedList
        notifyDataSetChanged()
    }

    class HeaderViewHolder(private val binding: ItemDateHeaderBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(date: String) {
            binding.tvDateHeader.text = date
        }
    }

    inner class TaskViewHolder(private val binding: ItemTaskCardBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(task: Tasks) {
            binding.tvTaskTitle.text = task.title
            binding.tvTaskDescription.text = task.description ?: ""
            
            // Tachado si está completada
            if (task.isCompleted) {
                binding.tvTaskTitle.paintFlags = binding.tvTaskTitle.paintFlags or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
                binding.root.alpha = 0.6f
            } else {
                binding.tvTaskTitle.paintFlags = binding.tvTaskTitle.paintFlags and android.graphics.Paint.STRIKE_THRU_TEXT_FLAG.inv()
                binding.root.alpha = 1.0f
            }

            val timeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())
            val end = timeFormatter.format(Date(task.dueDate))
            binding.tvTaskTime.text = end

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
            
            // Color de categoría según el ID
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
