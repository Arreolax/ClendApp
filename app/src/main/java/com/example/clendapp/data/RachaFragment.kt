package com.example.clendapp

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.clendapp.data.AppDatabase
import com.example.clendapp.databinding.FragmentRachaBinding
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import java.util.*

class RachaFragment : Fragment() {

    private var _binding: FragmentRachaBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRachaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        loadStreakData()
    }

    private fun loadStreakData() {
        val sharedPref = requireContext().getSharedPreferences("clend_app_prefs", Context.MODE_PRIVATE)
        val userId = sharedPref.getInt("user_id", -1)
        if (userId == -1) return

        val db = AppDatabase.getDatabase(requireContext())
        viewLifecycleOwner.lifecycleScope.launch {
            val scores = db.scoresDao().getScoresByUserId(userId)
            val rank = db.ranksDao().getRankById(scores?.id_rank ?: 1)

            scores?.let {
                binding.tvStreakTitle.text = getString(R.string.streak_title, it.streak_count)
                binding.tvPointsBadge.text = (it.study_score + it.calculator_score).toString()
                binding.tvCurrentRankBadge.text = getString(R.string.rank_status, rank?.name ?: "Potato")

                // Update rank image
                val rankDrawable = when (it.id_rank) {
                    2 -> R.drawable.rank_brainy
                    3 -> R.drawable.rank_nerd
                    4 -> R.drawable.rank_master
                    5 -> R.drawable.rank_god
                    6 -> R.drawable.rank_devil
                    7 -> R.drawable.rank_top_global
                    else -> R.drawable.rank_papita
                }
                binding.ivRankImage.setImageResource(rankDrawable)

                // Weekly history logic (circles with check)
                updateWeeklyVisuals(it.weekly_bits)

                // Music reward logic
                if (it.music_unlocked) {
                    binding.layoutRewardMusicUnlocked.visibility = View.VISIBLE
                    binding.ivRewardMusicLocked.visibility = View.GONE
                    binding.cvRewardMusic.setCardBackgroundColor(0xFF7E70FF.toInt())
                    
                    // Show floating notification if first time
                    checkAndShowUnlockNotification()
                } else {
                    binding.layoutRewardMusicUnlocked.visibility = View.GONE
                    binding.ivRewardMusicLocked.visibility = View.VISIBLE
                    binding.cvRewardMusic.setCardBackgroundColor(0xFFE0E0E0.toInt())
                }

                // Visual logic for streak fire: If streak is from today, show vivid color, else gray
                val lastActivity = Calendar.getInstance().apply { timeInMillis = it.last_activity_date }
                val today = Calendar.getInstance()

                val isDoneToday = today.get(Calendar.YEAR) == lastActivity.get(Calendar.YEAR) &&
                        today.get(Calendar.DAY_OF_YEAR) == lastActivity.get(Calendar.DAY_OF_YEAR)

                val colorMatrix = ColorMatrix()
                if (!isDoneToday) {
                    colorMatrix.setSaturation(0f) // Grayscale
                    val filter = ColorMatrixColorFilter(colorMatrix)
                    binding.ivStreakIcon.colorFilter = filter
                    binding.ivStreakBadge.colorFilter = filter
                    binding.cvStreakStats.alpha = 0.8f
                } else {
                    binding.ivStreakIcon.colorFilter = null // Original colors
                    binding.ivStreakBadge.colorFilter = null
                    binding.cvStreakStats.alpha = 1.0f
                }
            }
        }
    }

    private fun checkAndShowUnlockNotification() {
        val sharedPref = requireContext().getSharedPreferences("clend_app_prefs", Context.MODE_PRIVATE)
        val shown = sharedPref.getBoolean("music_unlocked_notif_shown", false)
        
        if (!shown) {
            val snackbar = Snackbar.make(binding.root, getString(R.string.congrats_unlock), Snackbar.LENGTH_INDEFINITE)
            snackbar.setAction("OK") { snackbar.dismiss() }
            snackbar.show()
            
            with(sharedPref.edit()) {
                putBoolean("music_unlocked_notif_shown", true)
                apply()
            }
        }
    }

    private fun updateWeeklyVisuals(bits: Int) {
        val dayViews = arrayOf(
            binding.ivDayMon to binding.tvPointsMon,
            binding.ivDayTue to binding.tvPointsTue,
            binding.ivDayWed to binding.tvPointsWed,
            binding.ivDayThu to binding.tvPointsThu,
            binding.ivDayFri to binding.tvPointsFri,
            binding.ivDaySat to binding.tvPointsSat,
            binding.ivDaySun to binding.tvPointsSun
        )

        for (i in 0 until 7) {
            val (imageView, textView) = dayViews[i]
            val isChecked = (bits and (1 shl i)) != 0
            
            if (isChecked) {
                imageView.setImageResource(R.drawable.ic_check_circle)
                imageView.backgroundTintList = null // Remove semi-transparent white tint
                textView.visibility = View.VISIBLE
            } else {
                imageView.setImageDrawable(null)
                textView.visibility = View.INVISIBLE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
