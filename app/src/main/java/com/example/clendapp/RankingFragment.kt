package com.example.clendapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.clendapp.data.AppDatabase
import com.example.clendapp.data.Scores
import com.example.clendapp.databinding.FragmentRankingBinding
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.launch

class RankingFragment : Fragment() {

    private var _binding: FragmentRankingBinding? = null
    private val binding get() = _binding!!

    private lateinit var rankingAdapter: RankingAdapter
    private var allCalculatorScores: List<Scores> = emptyList()
    private var allStudyScores: List<Scores> = emptyList()
    private var currentUsername: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRankingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupNavigation()
        setupRecyclerView(true) // Start with calculator
        setupTabs()
        loadCurrentUsername()
        observeScores()
        setupSwipeRefresh()
    }

    private fun loadCurrentUsername() {
        val sharedPref = requireContext().getSharedPreferences("clend_app_prefs", android.content.Context.MODE_PRIVATE)
        val userId = sharedPref.getInt("user_id", -1)
        if (userId != -1) {
            val database = AppDatabase.getDatabase(requireContext())
            viewLifecycleOwner.lifecycleScope.launch {
                val user = database.userDao().getUserById(userId)
                currentUsername = user?.username ?: "Me"
                binding.tvMyRankName.text = currentUsername
            }
        }
    }

    private fun setupNavigation() {
        // Ocultar BottomNavigationView al entrar
        activity?.findViewById<View>(R.id.bottom_navigation)?.visibility = View.GONE

        binding.btnBackRanking.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun setupRecyclerView(isCalculator: Boolean) {
        rankingAdapter = RankingAdapter(isCalculator)
        binding.rvRanking.apply {
            adapter = rankingAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
        updateList()
    }

    private fun setupTabs() {
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                val isCalculator = binding.tabLayout.selectedTabPosition == 0
                setupRecyclerView(isCalculator)
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun observeScores() {
        val database = AppDatabase.getDatabase(requireContext())
        
        // Observe calculator scores
        viewLifecycleOwner.lifecycleScope.launch {
            database.scoresDao().getAllScoresByCalculator().collect { scores ->
                if (_binding == null) return@collect
                allCalculatorScores = scores
                if (binding.tabLayout.selectedTabPosition == 0) updateList()
            }
        }

        // Observe study scores
        viewLifecycleOwner.lifecycleScope.launch {
            database.scoresDao().getAllScoresByStudy().collect { scores ->
                if (_binding == null) return@collect
                allStudyScores = scores
                if (binding.tabLayout.selectedTabPosition == 1) updateList()
            }
        }
    }

    private fun updateList() {
        val isCalculator = binding.tabLayout.selectedTabPosition == 0
        val rawList = if (isCalculator) allCalculatorScores else allStudyScores

        // Filter users with 0 points to check if we should show empty state
        val filteredList = rawList.filter { 
            val scoreValue = if (isCalculator) it.calculator_score else it.study_score
            scoreValue > 0
        }

        if (filteredList.isEmpty()) {
            binding.rvRanking.visibility = View.GONE
            binding.layoutEmptyState.visibility = View.VISIBLE
        } else {
            binding.rvRanking.visibility = View.VISIBLE
            binding.layoutEmptyState.visibility = View.GONE
            rankingAdapter.submitList(filteredList)
        }
        updateMyRankCard()
    }

    private fun updateMyRankCard() {
        val sharedPref = requireContext().getSharedPreferences("clend_app_prefs", android.content.Context.MODE_PRIVATE)
        val loggedInUserId = sharedPref.getInt("user_id", -1)
        if (loggedInUserId == -1) return

        val isCalculator = binding.tabLayout.selectedTabPosition == 0
        val rawList = if (isCalculator) allCalculatorScores else allStudyScores
        
        // Find user in the full list (sorted)
        val userRankIndex = rawList.indexOfFirst { it.id_user == loggedInUserId }
        
        if (userRankIndex != -1) {
            val userScore = rawList[userRankIndex]
            val position = userRankIndex + 1
            val scoreValue = if (isCalculator) userScore.calculator_score else userScore.study_score
            
            binding.tvMyRankPos.text = "#$position"
            binding.tvMyRankScore.text = "$scoreValue pts"
            binding.tvMyRankName.text = "@$currentUsername" ?: "Me"
        } else {
            binding.tvMyRankPos.text = "--"
            binding.tvMyRankScore.text = "0 pts"
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            observeScores()
            binding.swipeRefresh.isRefreshing = false
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Mostrar BottomNavigationView al salir
        activity?.findViewById<View>(R.id.bottom_navigation)?.visibility = View.VISIBLE
        _binding = null
    }
}
