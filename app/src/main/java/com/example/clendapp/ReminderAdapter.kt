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
            // Title
            binding.tvReminderTitle.text = task.title
            binding.tvReminderTitle.setTextColor(android.graphics.Color.WHITE)

            // Status logic for reminders
            when {
                task.late -> {
                    binding.root.setBackgroundResource(R.drawable.bg_blue_card_late)
                    binding.tvReminderTime.setTextColor(android.graphics.Color.RED)
                    binding.ivStatusIcon.visibility = android.view.View.VISIBLE
                    binding.ivStatusIcon.setImageResource(R.drawable.ic_error)
                    binding.ivStatusIcon.imageTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.RED)
                }
                else -> {
                    binding.root.setBackgroundResource(R.drawable.bg_blue_card)
                    binding.tvReminderTime.setTextColor(android.graphics.Color.WHITE)
                    binding.ivStatusIcon.visibility = android.view.View.VISIBLE
                    binding.ivStatusIcon.setImageResource(R.drawable.ic_clock)
                    binding.ivStatusIcon.imageTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.WHITE)
                }
            }

            binding.tvReminderDescription.text = task.description ?: ""
            
            val timeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())
            binding.tvReminderTime.text = timeFormatter.format(Date(task.dueDate))

            binding.root.setOnClickListener { onTaskClick(task) }
        }
    }
}
