package com.example.clendapp

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.clendapp.data.Tasks
import com.example.clendapp.databinding.ItemReminderCardBinding
import java.text.SimpleDateFormat
import java.util.*

class ReminderAdapter(
    private val onTaskClick: (Tasks) -> Unit
) : RecyclerView.Adapter<ReminderAdapter.ReminderViewHolder>() {

    private var tasks: List<Tasks> = emptyList()

    fun submitTasks(newTasks: List<Tasks>) {
        tasks = newTasks
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReminderViewHolder {
        val binding = ItemReminderCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ReminderViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ReminderViewHolder, position: Int) {
        holder.bind(tasks[position])
    }

    override fun getItemCount(): Int = tasks.size

    inner class ReminderViewHolder(private val binding: ItemReminderCardBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(task: Tasks) {
            binding.tvReminderTitle.text = task.title
            binding.tvReminderDescription.text = task.description ?: ""
            
            val timeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())
            binding.tvReminderTime.text = timeFormatter.format(Date(task.dueDate))

            // Update icon background based on category
            val color = when (task.id_category) {
                1 -> "#673AB7" // Purple
                2 -> "#4CAF50" // Green
                else -> "#2196F3" // Blue
            }
            
            // Note: The parent FrameLayout doesn't have an ID in the layout, 
            // but we can color the light blue icon background or just keep the style

            binding.root.setOnClickListener { onTaskClick(task) }
        }
    }
}
