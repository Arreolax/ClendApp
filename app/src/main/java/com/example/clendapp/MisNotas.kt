package com.example.clendapp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.clendapp.data.AppDatabase
import com.example.clendapp.data.Folders
import com.example.clendapp.data.Notes
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MisNotas : AppCompatActivity() {

    private lateinit var containerNotas: LinearLayout
    private var currentFolder = "All"
    private lateinit var database: AppDatabase
    private var notesJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Verify session
        val sharedPref = getSharedPreferences("clend_app_prefs", Context.MODE_PRIVATE)
        val userId = sharedPref.getInt("user_id", -1)
        if (userId == -1) {
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
            return
        }

        enableEdgeToEdge()
        setContentView(R.layout.misnotas)
        database = AppDatabase.getDatabase(this)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val basePadding = (24 * resources.displayMetrics.density).toInt()
            v.setPadding(
                systemBars.left + basePadding,
                systemBars.top + basePadding,
                systemBars.right + basePadding,
                systemBars.bottom + basePadding
            )
            insets
        }

        containerNotas = findViewById(R.id.container_notas)
        findViewById<View>(R.id.btn_back).setOnClickListener { finish() }

        setupNewNoteCard()
        refreshNotes()
    }

    override fun onResume() {
        super.onResume()
        refreshNotes()
    }

    private fun setupFolders() {
        val sharedPref = getSharedPreferences("clend_app_prefs", Context.MODE_PRIVATE)
        val userId = sharedPref.getInt("user_id", -1)
        if (userId == -1) return

        lifecycleScope.launch {
            val dbFoldersList = database.foldersDao().getFoldersForUser(userId)
            val foldersNames = listOf("All") + dbFoldersList.map { it.name }
            val container = findViewById<LinearLayout>(R.id.container_tags)
            val typeface = ResourcesCompat.getFont(this@MisNotas, R.font.hey_comic)
            
            container.removeAllViews()
            foldersNames.forEach { folderName ->
                val tv = TextView(this@MisNotas).apply {
                    text = folderName
                    this.typeface = typeface
                    setPadding(40, 20, 40, 20)
                    textSize = 13f
                    val lp = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { setMargins(0, 0, 20, 0) }
                    layoutParams = lp

                    if (folderName == currentFolder) {
                        setBackgroundResource(R.drawable.bg_pill_categoria_selected)
                        setTextColor(ContextCompat.getColor(context, R.color.white))
                    } else {
                        setBackgroundResource(R.drawable.bg_pill_categoria)
                        setTextColor(ContextCompat.getColor(context, R.color.text_medium))
                    }

                    setOnClickListener {
                        currentFolder = folderName
                        setupFolders()
                        refreshNotes()
                    }
                }
                container.addView(tv)
            }
        }
    }

    private fun setupNewNoteCard() {
        val cardNuevaNota = findViewById<View>(R.id.card_nueva_nota)
        val etNuevaNota = findViewById<EditText>(R.id.et_nueva_nota)
        val btnAgregar = findViewById<View>(R.id.btn_agregar)

        btnAgregar.setOnClickListener {
            val content = etNuevaNota.text.toString()
            val intent = Intent(this, HacerNota::class.java)
            if (content.isNotEmpty()) {
                intent.putExtra("CONTENT", content)
            }
            startActivity(intent)
            etNuevaNota.setText("")
        }
        
        cardNuevaNota.setOnClickListener {
             startActivity(Intent(this, HacerNota::class.java))
        }
    }

    private fun refreshNotes() {
        val sharedPref = getSharedPreferences("clend_app_prefs", Context.MODE_PRIVATE)
        val userId = sharedPref.getInt("user_id", -1)
        if (userId == -1) return

        setupFolders()
        
        notesJob?.cancel()
        notesJob = lifecycleScope.launch {
            val notesFlow = if (currentFolder == "All") {
                database.notesDao().getAllNotes(userId)
            } else {
                val folderObj = database.foldersDao().getFolderByName(currentFolder, userId)
                if (folderObj != null) {
                    database.notesDao().getNotesByFolder(userId, folderObj.id)
                } else {
                    database.notesDao().getAllNotes(userId)
                }
            }

            notesFlow.collectLatest { notesList ->
                containerNotas.removeAllViews()
                
                val tvEmptyNotes = findViewById<TextView>(R.id.tv_empty_notes)
                if (notesList.isEmpty()) {
                    tvEmptyNotes.visibility = View.VISIBLE
                    tvEmptyNotes.text = if (currentFolder == "All") "No notes found" else "No notes in folder: $currentFolder"
                } else {
                    tvEmptyNotes.visibility = View.GONE
                }

                for (note in notesList) {
                    val noteView = LayoutInflater.from(this@MisNotas).inflate(R.layout.bg_card_nota_item, containerNotas, false)
                    val tvTitulo = noteView.findViewById<TextView>(R.id.tv_nota_titulo)
                    val tvCuerpo = noteView.findViewById<TextView>(R.id.tv_nota_cuerpo)
                    val tvBadge = noteView.findViewById<TextView>(R.id.tv_badge)

                    tvTitulo.text = if (note.title.isEmpty()) "Untitled" else note.title
                    tvCuerpo.text = note.description
                    
                    val sdf = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
                    tvBadge.text = sdf.format(Date(note.date))

                    noteView.setOnClickListener {
                        val intent = Intent(this@MisNotas, HacerNota::class.java)
                        intent.putExtra("NOTE_ID", note.id)
                        startActivity(intent)
                    }

                    noteView.findViewById<View>(R.id.btn_menu_item).setOnClickListener {
                        showMenuNota(note)
                    }

                    noteView.setOnLongClickListener {
                        showMenuNota(note)
                        true
                    }

                    containerNotas.addView(noteView)
                }
            }
        }
    }

    private fun showMenuNota(note: Notes) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.menunota, null)
        val dialog = AlertDialog.Builder(this, R.style.CustomDialogTheme)
            .setView(dialogView)
            .create()

        dialogView.findViewById<TextView>(R.id.tv_menu_titulo).text = note.title
        
        dialogView.findViewById<View>(R.id.opcion_eliminar).setOnClickListener {
            dialog.dismiss()
            showDeleteConfirmationDialog(note)
        }

        dialogView.findViewById<View>(R.id.opcion_editar).setOnClickListener {
            val intent = Intent(this, HacerNota::class.java)
            intent.putExtra("NOTE_ID", note.id)
            startActivity(intent)
            dialog.dismiss()
        }

        dialog.show()

        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.9).toInt(),
            WindowManager.LayoutParams.WRAP_CONTENT
        )
    }

    private fun showDeleteConfirmationDialog(note: Notes) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_calculadora_game, null)
        val dialog = AlertDialog.Builder(this, R.style.CustomDialogTheme)
            .setView(dialogView)
            .create()

        dialogView.findViewById<TextView>(R.id.tv_dialog_title).text = "Delete Note?"
        dialogView.findViewById<TextView>(R.id.tv_dialog_message).text = "Are you sure you want to delete this note? This action cannot be undone."
        
        val btnConfirm = dialogView.findViewById<TextView>(R.id.btn_dialog_accept)
        val btnCancel = dialogView.findViewById<TextView>(R.id.btn_dialog_cancel)
        
        btnConfirm.text = "Delete"
        btnCancel.text = "Cancel"
        btnCancel.visibility = View.VISIBLE

        btnConfirm.setOnClickListener {
            lifecycleScope.launch {
                database.notesDao().delete(note)
                dialog.dismiss()
                Toast.makeText(this@MisNotas, "Note deleted", Toast.LENGTH_SHORT).show()
            }
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.85).toInt(),
            WindowManager.LayoutParams.WRAP_CONTENT
        )
    }
}
