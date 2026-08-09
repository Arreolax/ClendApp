package com.example.clendapp

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.clendapp.data.AppDatabase
import com.example.clendapp.data.Scores
import com.example.clendapp.databinding.ItemRankingUserBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RankingAdapter(private val isCalculator: Boolean) : RecyclerView.Adapter<RankingAdapter.RankingViewHolder>() {

    private var items: List<Scores> = emptyList()

    fun submitList(newItems: List<Scores>) {
        // Filtrar usuarios con 0 puntos
        items = newItems.filter { 
            val scoreValue = if (isCalculator) it.calculator_score else it.study_score
            scoreValue > 0
        }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RankingViewHolder {
        val binding = ItemRankingUserBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RankingViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RankingViewHolder, position: Int) {
        holder.bind(items[position], position + 1)
    }

    override fun getItemCount(): Int = items.size

    inner class RankingViewHolder(private val binding: ItemRankingUserBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(score: Scores, position: Int) {
            binding.tvRankPosition.text = position.toString()
            binding.tvUserScore.text = "${if (isCalculator) score.calculator_score else score.study_score} pts"
            
            // Lógica de medallas
            when (position) {
                1 -> {
                    binding.ivRankMedal.visibility = View.VISIBLE
                    binding.ivRankMedal.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(binding.root.context, R.color.medal_gold)) // Dorado
                    binding.tvRankPosition.visibility = View.GONE
                }
                2 -> {
                    binding.ivRankMedal.visibility = View.VISIBLE
                    binding.ivRankMedal.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(binding.root.context, R.color.medal_silver)) // Plateado
                    binding.tvRankPosition.visibility = View.GONE
                }
                3 -> {
                    binding.ivRankMedal.visibility = View.VISIBLE
                    binding.ivRankMedal.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(binding.root.context, R.color.medal_bronze)) // Bronce
                    binding.tvRankPosition.visibility = View.GONE
                }
                else -> {
                    binding.ivRankMedal.visibility = View.GONE
                    binding.tvRankPosition.visibility = View.VISIBLE
                }
            }

            // Fetch user info
            CoroutineScope(Dispatchers.IO).launch {
                val db = AppDatabase.getDatabase(binding.root.context)
                val user = db.userDao().getUserById(score.id_user)
                val rank = db.ranksDao().getRankById(score.id_rank)
                
                withContext(Dispatchers.Main) {
                    binding.tvUserUsername.text = "@${user?.username ?: "unknown"}"
                    binding.tvUserRankName.text = rank?.name ?: "Potato"
                    binding.tvUserRankName.setTextColor(ContextCompat.getColor(binding.root.context, R.color.primary))
                }
            }
        }
    }
}
