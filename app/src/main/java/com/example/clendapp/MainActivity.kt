package com.example.clendapp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.setupWithNavController
import com.example.clendapp.data.AppDatabase
import com.example.clendapp.databinding.ActivityMainBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var database: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        database = AppDatabase.getDatabase(this)
        loadUserData()
        updateLateTasksStatus()
        NotificationHelper.createNotificationChannel(this)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        binding.bottomNavigation.setupWithNavController(navController)

        // Manejar navegación desde extras (notificaciones, etc)
        if (intent.getBooleanExtra("OPEN_TASKS", false)) {
            binding.bottomNavigation.selectedItemId = R.id.nav_check
        }

        // Manejar el clic en nav_menu para abrir el menú lateral derecho
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_menu -> {
                    binding.drawerLayout.openDrawer(GravityCompat.END)
                    false
                }
                else -> {
                    // Usar NavigationUI para manejar el resto de los items (Home, Calendar, etc.)
                    // Esto asegura que la navegación sea consistente con setupWithNavController
                    NavigationUI.onNavDestinationSelected(item, navController)
                }
            }
        }

        // Configurar clics de los botones del menú lateral personalizado
        setupDrawerButtons()

        // Manejar el botón atrás de forma moderna
        val callback = object : OnBackPressedCallback(false) {
            override fun handleOnBackPressed() {
                binding.drawerLayout.closeDrawer(GravityCompat.END)
            }
        }
        onBackPressedDispatcher.addCallback(this, callback)

        binding.drawerLayout.addDrawerListener(object : androidx.drawerlayout.widget.DrawerLayout.SimpleDrawerListener() {
            override fun onDrawerOpened(drawerView: View) {
                callback.isEnabled = true
            }
            override fun onDrawerClosed(drawerView: View) {
                callback.isEnabled = false
            }
        })
    }

    private fun loadUserData() {
        val sharedPref = getSharedPreferences("clend_app_prefs", Context.MODE_PRIVATE)
        val userId = sharedPref.getInt("user_id", -1)

        if (userId != -1) {
            lifecycleScope.launch {
                val user = database.userDao().getUserById(userId)
                user?.let {
                    val scores = database.scoresDao().getScoresByUserId(userId)
                    val rankId = scores?.id_rank ?: 1
                    val rank = database.ranksDao().getRankById(rankId)
                    binding.navHeader.tvHeaderName.text = it.fullName
                    binding.navHeader.tvHeaderRank.text = "Rank: ${rank?.name ?: "Unknown"}"
                    binding.navHeader.tvHeaderUsername.text = "@${it.username}"
                }
            }
        }
    }

    private fun updateLateTasksStatus() {
        lifecycleScope.launch {
            database.tasksDao().updateLateTasks(System.currentTimeMillis())
        }
    }

    private fun setupDrawerButtons() {
        binding.btnPerfil.setOnClickListener {
            closeDrawerAndNavigate()
        }
        binding.btnNotas.setOnClickListener {
            binding.drawerLayout.closeDrawer(GravityCompat.END)
            val intent = Intent(this, MisNotas::class.java)
            startActivity(intent)
        }
        binding.btnCalculadora.setOnClickListener {
            binding.drawerLayout.closeDrawer(GravityCompat.END)
            val intent = Intent(this, calculadora::class.java)
            startActivity(intent)
        }
        binding.btnUnknown.setOnClickListener {
            closeDrawerAndNavigate()
        }
        binding.btnRanking.setOnClickListener {
            closeDrawerAndNavigate()
        }
        binding.btnPremium.setOnClickListener {
            closeDrawerAndNavigate()
        }
        binding.btnSupport.setOnClickListener {
            closeDrawerAndNavigate()
        }
        binding.btnAjustes.setOnClickListener {
            closeDrawerAndNavigate()
        }
        binding.btnLogout.setOnClickListener {
            logout()
        }
    }

    private fun logout() {
        val sharedPref = getSharedPreferences("clend_app_prefs", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            remove("user_id")
            apply()
        }
        // Redirigir a LoginActivity
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun closeDrawerAndNavigate() {
        binding.drawerLayout.closeDrawer(GravityCompat.END)
        // Aquí se puede añadir la lógica de navegación
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra("OPEN_TASKS", false)) {
            binding.bottomNavigation.selectedItemId = R.id.nav_check
        }
    }
}
