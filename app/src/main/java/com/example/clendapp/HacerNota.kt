package com.example.clendapp

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.clendapp.data.AppDatabase
import com.example.clendapp.data.Folders
import com.example.clendapp.data.Notes
import com.example.clendapp.data.StreakManager
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class HacerNota : AppCompatActivity() {

    private lateinit var etTitulo: EditText
    private lateinit var etCuerpo: EditText
    private lateinit var tvContador: TextView
    private lateinit var btnListMode: TextView
    private lateinit var cardCuerpo: View

    private var currentNote: Notes? = null
    private var isListMode = false
    private lateinit var database: AppDatabase
    private var userId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val sharedPref = getSharedPreferences("clend_app_prefs", Context.MODE_PRIVATE)
        userId = sharedPref.getInt("user_id", -1)
        if (userId == -1) {
            finish()
            return
        }

        enableEdgeToEdge()
        setContentView(R.layout.hacernota)
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

        etTitulo = findViewById(R.id.et_titulo)
        etCuerpo = findViewById(R.id.et_cuerpo)
        tvContador = findViewById(R.id.tv_contador)
        btnListMode = findViewById(R.id.btn_list_mode)
        cardCuerpo = findViewById(R.id.card_cuerpo)

        val noteId = intent.getIntExtra("NOTE_ID", -1)
        val initialContent = intent.getStringExtra("CONTENT")

        if (noteId != -1) {
            lifecycleScope.launch {
                val note = database.notesDao().getNote(noteId).first()
                note?.let {
                    currentNote = it
                    etTitulo.setText(it.title)
                    etCuerpo.setText(it.description)
                    isListMode = it.isList
                    updateListModeUI()
                    updateContador()
                }
            }
        } else if (initialContent != null) {
            etCuerpo.setText(initialContent)
            updateContador()
        }

        onBackPressedDispatcher.addCallback(this) {
            handleSave(isManualClick = false)
        }

        updateListModeUI()
        updateContador()
        setupListeners()
    }

    private fun setupListeners() {
        findViewById<View>(R.id.btn_back).setOnClickListener {
            handleSave(isManualClick = false)
        }

        findViewById<View>(R.id.btn_guardar).setOnClickListener {
            handleSave(isManualClick = true)
        }

        etCuerpo.addTextChangedListener(object : TextWatcher {
            private var isInternalChange = false
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (isInternalChange) return
                updateContador()

                if (isListMode && count == 1 && s?.get(start) == '\n') {
                    isInternalChange = true
                    val currentText = etCuerpo.text.toString()
                    val beforeCursor = currentText.substring(0, start + 1)
                    val afterCursor = currentText.substring(start + 1)
                    etCuerpo.setText("${beforeCursor}★ ${afterCursor}")
                    etCuerpo.setSelection(start + 3)
                    isInternalChange = false
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        btnListMode.setOnClickListener {
            isListMode = !isListMode
            updateListModeUI()
        }
    }

    private fun updateListModeUI() {
        if (isListMode) {
            btnListMode.text = "★ List Mode: ON"
            btnListMode.setTextColor(ContextCompat.getColor(this, R.color.warning_dark))
            etCuerpo.setTextColor(ContextCompat.getColor(this, R.color.black))

            if (etCuerpo.text.isEmpty()) {
                etCuerpo.setText("★ ")
                etCuerpo.setSelection(2)
            }
        } else {
            btnListMode.text = "List Mode"
            btnListMode.setTextColor(ContextCompat.getColor(this, R.color.primary_light))
            etCuerpo.setTextColor(ContextCompat.getColor(this, R.color.info_variant))
            cardCuerpo.setBackgroundResource(R.drawable.bg_card_nota)
        }
    }

    private fun updateContador() {
        tvContador.text = etCuerpo.text.length.toString()
    }

    private fun handleSave(isManualClick: Boolean) {
        val title = etTitulo.text.toString().trim()
        val content = etCuerpo.text.toString().trim()

        if (currentNote != null) {
            lifecycleScope.launch {
                val folderId = currentNote!!.id_folder
                saveNoteToDb(folderId)
                Toast.makeText(this@HacerNota, "Note updated", Toast.LENGTH_SHORT).show()
                finish()
            }
        } else {
            val contentValue = if (isListMode && content.startsWith("★")) {
                content.replace("★", "").trim()
            } else {
                content
            }

            if (title.isNotEmpty() && contentValue.isNotEmpty()) {
                showSaveModal()
            } else {
                if (isManualClick) {
                    if (title.isEmpty()) {
                        Toast.makeText(this, "Please add a title", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Please add content", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    finish()
                }
            }
        }
    }

    private fun showSaveModal() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.guardarnota, null)
        val dialog = AlertDialog.Builder(this, R.style.CustomDialogTheme)
            .setView(dialogView)
            .create()

        val container = dialogView.findViewById<LinearLayout>(R.id.container_modal_carpetas)
        var selectedFolderId = -1

        fun refreshFolders() {
            lifecycleScope.launch {
                container.removeAllViews()
                val folders = database.foldersDao().getFoldersForUser(userId)
                
                if (selectedFolderId == -1 && folders.isNotEmpty()) {
                    selectedFolderId = folders[0].id
                }

                folders.forEach { folder ->
                    val tv = TextView(this@HacerNota).apply {
                        text = folder.name
                        gravity = android.view.Gravity.CENTER
                        textSize = 14f
                        setPadding(0, 30, 0, 30)
                        val lp = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply { setMargins(0, 10, 0, 10) }
                        layoutParams = lp

                        if (folder.id == selectedFolderId) {
                            setBackgroundResource(R.drawable.bg_pill_categoria_selected)
                            setTextColor(ContextCompat.getColor(context, R.color.white))
                        } else {
                            setBackgroundResource(R.drawable.bg_pill_opcion)
                            setTextColor(ContextCompat.getColor(context, R.color.text_dark_variant))
                        }

                        setOnClickListener {
                            selectedFolderId = folder.id
                            refreshFolders()
                        }
                    }
                    container.addView(tv)
                }
            }
        }

        refreshFolders()

        dialogView.findViewById<View>(R.id.btn_guardar).setOnClickListener {
            lifecycleScope.launch {
                saveNoteToDb(selectedFolderId)
                dialog.dismiss()
                Toast.makeText(this@HacerNota, "Note saved", Toast.LENGTH_SHORT).show()
                finish()
            }
        }

        dialogView.findViewById<View>(R.id.btn_agregar_carpeta).setOnClickListener {
            val et = dialogView.findViewById<EditText>(R.id.et_nueva_carpeta)
            val newFolderName = et.text.toString().trim()
            if (newFolderName.isNotEmpty()) {
                lifecycleScope.launch {
                    val newFolderId = database.foldersDao().insert(Folders(name = newFolderName, id_user = userId))
                    selectedFolderId = newFolderId.toInt()
                    et.setText("")
                    refreshFolders()
                }
            }
        }

        dialog.show()
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.9).toInt(),
            WindowManager.LayoutParams.WRAP_CONTENT
        )
    }

    private suspend fun saveNoteToDb(folderId: Int) {
        val title = etTitulo.text.toString().trim()
        val content = etCuerpo.text.toString().trim()

        val points = StreakManager.completeActivity(this@HacerNota)
        if (points > 0) {
            Snackbar.make(findViewById(android.R.id.content), getString(R.string.points_congrats_generic, points), Snackbar.LENGTH_LONG).show()
        }

        if (currentNote == null) {
            val newNote = Notes(
                title = title,
                description = content,
                id_folder = folderId,
                date = System.currentTimeMillis(),
                isList = isListMode,
                id_user = userId
            )
            database.notesDao().insert(newNote)
        } else {
            val updatedNote = currentNote!!.copy(
                title = title,
                description = content,
                id_folder = folderId,
                date = System.currentTimeMillis(),
                isList = isListMode
            )
            database.notesDao().update(updatedNote)
        }
    }
}
