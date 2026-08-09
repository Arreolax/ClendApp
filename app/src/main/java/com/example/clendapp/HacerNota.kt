package com.example.clendapp

import android.content.Context
import android.content.Intent
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
import android.graphics.Color
import androidx.core.content.ContextCompat
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.clendapp.data.AppDatabase
import com.example.clendapp.data.Folders
import com.example.clendapp.data.Notes
import kotlinx.coroutines.launch
import kotlin.toString

class HacerNota : AppCompatActivity() {

    private lateinit var etTitulo: EditText
    private lateinit var etCuerpo: EditText
    private lateinit var tvContador: TextView
    private lateinit var btnListMode: TextView
    private lateinit var tvCurrentFolder: TextView
    private lateinit var cardCuerpo: View
    private lateinit var database: AppDatabase

    private var currentNote: Notes? = null
    private var isListMode = false
    private var selectedFolderName: String = "Personal"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Verificar sesión
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
        tvCurrentFolder = findViewById(R.id.tv_current_folder)
        cardCuerpo = findViewById(R.id.card_cuerpo)

        // Cargar nota si viene de edición
        val noteId = intent.getIntExtra("NOTE_ID", -1)
        val initialContent = intent.getStringExtra("CONTENT")

        if (noteId != -1) {
            loadNote(noteId)
        } else if (initialContent != null) {
            etCuerpo.setText(initialContent)
        }

        onBackPressedDispatcher.addCallback(this) {
            handleSave(isManualClick = false)
        }

        updateListModeUI()
        updateContador()
        setupListeners()
    }

    private fun loadNote(id: Int) {
        lifecycleScope.launch {
            database.notesDao().getNote(id).collect { note ->
                note?.let {
                    currentNote = it
                    etTitulo.setText(it.title)
                    etCuerpo.setText(it.description)
                    isListMode = it.isList
                    updateListModeUI()
                    updateContador()
                    
                    // Mostrar carpeta actual
                    val folder = database.foldersDao().getFolderById(it.id_folder)
                    folder?.let { f ->
                        selectedFolderName = f.name
                        tvCurrentFolder.text = "Folder: ${f.name}"
                        tvCurrentFolder.visibility = View.VISIBLE
                    }
                }
            }
        }
    }

    private fun showFolderSelectionModal() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.guardarnota, null)
        val dialog = AlertDialog.Builder(this, R.style.CustomDialogTheme)
            .setView(dialogView)
            .create()

        val container = dialogView.findViewById<LinearLayout>(R.id.container_modal_carpetas)
        val tvTituloModal = dialogView.findViewById<TextView>(R.id.tv_modal_titulo)
        tvTituloModal.text = "Move to folder"

        val sharedPref = getSharedPreferences("clend_app_prefs", Context.MODE_PRIVATE)
        val userId = sharedPref.getInt("user_id", -1)

        fun refreshFolders() {
            if (userId == -1) return
            lifecycleScope.launch {
                val dbFolders = database.foldersDao().getFoldersForUser(userId).map { it.name }
                container.removeAllViews()
                dbFolders.forEach { folderName ->
                    val tv = TextView(this@HacerNota).apply {
                        text = folderName
                        gravity = android.view.Gravity.CENTER
                        textSize = 14f
                        setPadding(0, 30, 0, 30)
                        val lp = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply { setMargins(0, 10, 0, 10) }
                        layoutParams = lp

                        if (folderName == selectedFolderName) {
                            setBackgroundResource(R.drawable.bg_pill_categoria_selected)
                            setTextColor(ContextCompat.getColor(context, R.color.white))
                        } else {
                            setBackgroundResource(R.drawable.bg_pill_opcion)
                            setTextColor(ContextCompat.getColor(context, R.color.text_dark_variant))
                        }

                        setOnClickListener {
                            selectedFolderName = folderName
                            tvCurrentFolder.text = "Folder: $folderName"
                            dialog.dismiss()
                        }
                    }
                    container.addView(tv)
                }
            }
        }

        refreshFolders()

        dialogView.findViewById<View>(R.id.btn_guardar).visibility = View.GONE
        dialogView.findViewById<View>(R.id.btn_salir).visibility = View.GONE
        dialogView.findViewById<View>(R.id.btn_close_modal).setOnClickListener { dialog.dismiss() }

        dialogView.findViewById<View>(R.id.btn_agregar_carpeta).setOnClickListener {
            val et = dialogView.findViewById<EditText>(R.id.et_nueva_carpeta)
            val newFolder = et.text.toString().trim()
            if (newFolder.isNotEmpty() && userId != -1) {
                lifecycleScope.launch {
                    database.foldersDao().insert(Folders(name = newFolder, id_user = userId))
                    selectedFolderName = newFolder
                    tvCurrentFolder.text = "Folder: $newFolder"
                    et.setText("")
                    refreshFolders()
                }
            }
        }

        dialog.show()
        dialog.window?.setLayout((resources.displayMetrics.widthPixels * 0.9).toInt(), WindowManager.LayoutParams.WRAP_CONTENT)
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

        tvCurrentFolder.setOnClickListener {
            showFolderSelectionModal()
        }
    }

    private fun updateListModeUI() {
        if (isListMode) {
            btnListMode.text = "★ List Mode: ON"
            btnListMode.setTextColor(ContextCompat.getColor(this, R.color.warning_dark))
            etCuerpo.setTextColor(ContextCompat.getColor(this, R.color.black))

            // If starting a new list, add first star
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
            // Existing notes: Save automatically
            lifecycleScope.launch {
                saveNote(selectedFolderName)
                Toast.makeText(this@HacerNota, "Note updated", Toast.LENGTH_SHORT).show()
                finish()
            }
        } else {
            // New notes
            val contentValue = if (isListMode && content.startsWith("★")) {
                content.replace("★", "").trim()
            } else {
                content
            }

            if (title.isNotEmpty() && contentValue.isNotEmpty()) {
                // If both are filled, ask where to save
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
        var selectedFolder = "Personal"

        val sharedPref = getSharedPreferences("clend_app_prefs", Context.MODE_PRIVATE)
        val userId = sharedPref.getInt("user_id", -1)

        fun refreshFolders() {
            if (userId == -1) return
            
            lifecycleScope.launch {
                val dbFolders = database.foldersDao().getFoldersForUser(userId).map { it.name }
                container.removeAllViews()
                
                dbFolders.forEach { folderName ->
                    val tv = TextView(this@HacerNota).apply {
                        text = folderName
                        gravity = android.view.Gravity.CENTER
                        textSize = 14f
                        setPadding(0, 30, 0, 30)
                        val lp = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply { setMargins(0, 10, 0, 10) }
                        layoutParams = lp

                        if (folderName == selectedFolder) {
                            setBackgroundResource(R.drawable.bg_pill_categoria_selected)
                            setTextColor(ContextCompat.getColor(context, R.color.white))
                        } else {
                            setBackgroundResource(R.drawable.bg_pill_opcion)
                            setTextColor(ContextCompat.getColor(context, R.color.text_dark_variant))
                        }

                        setOnClickListener {
                            selectedFolder = folderName
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
                saveNote(selectedFolder)
                dialog.dismiss()
                Toast.makeText(this@HacerNota, "Note saved in $selectedFolder", Toast.LENGTH_SHORT).show()
                finish()
            }
        }

        dialogView.findViewById<View>(R.id.btn_salir).setOnClickListener {
            showExitWarningDialog(dialog)
        }

        dialogView.findViewById<View>(R.id.btn_close_modal).setOnClickListener {
            dialog.dismiss()
        }

        dialogView.findViewById<View>(R.id.btn_agregar_carpeta).setOnClickListener {
            val et = dialogView.findViewById<EditText>(R.id.et_nueva_carpeta)
            val newFolder = et.text.toString().trim()
            if (newFolder.isNotEmpty() && userId != -1) {
                lifecycleScope.launch {
                    database.foldersDao().insert(Folders(name = newFolder, id_user = userId))
                    selectedFolder = newFolder
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

    private fun showExitWarningDialog(saveModal: AlertDialog) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_calculadora_game, null)
        val dialog = AlertDialog.Builder(this, R.style.CustomDialogTheme)
            .setView(dialogView)
            .create()

        dialogView.findViewById<TextView>(R.id.tv_dialog_title).text = "Discard Changes?"
        dialogView.findViewById<TextView>(R.id.tv_dialog_message).text = "Your note has not been saved. Are you sure you want to exit?"
        
        val btnConfirm = dialogView.findViewById<TextView>(R.id.btn_dialog_accept)
        val btnCancel = dialogView.findViewById<TextView>(R.id.btn_dialog_cancel)
        
        btnConfirm.text = "Discard"
        btnCancel.text = "Keep Editing"
        btnCancel.visibility = View.VISIBLE

        btnConfirm.setOnClickListener {
            dialog.dismiss()
            saveModal.dismiss()
            finish()
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

    private suspend fun saveNote(folderName: String) {
        val title = etTitulo.text.toString()
        val content = etCuerpo.text.toString()
        
        val sharedPref = getSharedPreferences("clend_app_prefs", Context.MODE_PRIVATE)
        val userId = sharedPref.getInt("user_id", -1)
        if (userId == -1) return

        // 1. Obtener o crear el ID de la carpeta
        var folder = database.foldersDao().getFolderByName(folderName, userId)
        if (folder == null) {
            val newFolderId = database.foldersDao().insert(Folders(name = folderName, id_user = userId))
            folder = Folders(id = newFolderId.toInt(), name = folderName, id_user = userId)
        }

        if (currentNote == null) {
            val newNote = Notes(
                title = title,
                description = content,
                id_folder = folder.id,
                isList = isListMode,
                date = System.currentTimeMillis(),
                id_user = userId
            )
            database.notesDao().insert(newNote)
        } else {
            val updatedNote = currentNote!!.copy(
                title = title,
                description = content,
                id_folder = folder.id,
                isList = isListMode,
                date = System.currentTimeMillis()
            )
            database.notesDao().update(updatedNote)
        }
    }
}
