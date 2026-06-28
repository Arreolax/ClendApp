package com.example.clendapp

import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.setupWithNavController
import com.example.clendapp.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        binding.bottomNavigation.setupWithNavController(navController)

        // Manejar el clic en nav_menu para abrir el menú lateral derecho
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_menu -> {
                    binding.drawerLayout.openDrawer(GravityCompat.END)
                    false
                }
                else -> {
                    NavigationUI.onNavDestinationSelected(item, navController)
                }
            }
        }

        // Configurar clics de los botones del menú lateral personalizado
        setupDrawerButtons()

        // Manejar el botón atrás para cerrar el menú si está abierto
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (binding.drawerLayout.isDrawerOpen(GravityCompat.END)) {
                    binding.drawerLayout.closeDrawer(GravityCompat.END)
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    private fun setupDrawerButtons() {
        binding.btnPerfil.setOnClickListener {
            closeDrawerAndNavigate()
        }
        binding.btnNotas.setOnClickListener {
            closeDrawerAndNavigate()
        }
        binding.btnCalculadora.setOnClickListener {
            closeDrawerAndNavigate()
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
            closeDrawerAndNavigate()
        }
    }

    private fun closeDrawerAndNavigate() {
        binding.drawerLayout.closeDrawer(GravityCompat.END)
        // Aquí se puede añadir la lógica de navegación
    }
}