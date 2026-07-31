package com.example.clendapp

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
import android.view.Gravity
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.content.res.ResourcesCompat

class HacerNota : AppCompatActivity() {

    private lateinit var etTitulo: EditText
    private lateinit var etCuerpo: EditText
    private lateinit var tvContador: TextView
    private lateinit var btnListMode: TextView
    private lateinit var cardCuerpo: View
    
    private var currentNote: Note? = null
    private var isListMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.hacernota)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        NoteManager.loadNotes(this)
        etTitulo = findViewById(R.id.et_titulo)
        etCuerpo = findViewById(R.id.et_cuerpo)
        tvContador = findViewById(R.id.tv_contador)
        btnListMode = findViewById(R.id.btn_list_mode)
        cardCuerpo = findViewById(R.id.card_cuerpo)

        currentNote = intent.getSerializableExtra("NOTE") as? Note
        val initialContent = intent.getStringExtra("CONTENT")

        if (currentNote != null) {
            etTitulo.setText(currentNote!!.title)
            etCuerpo.setText(currentNote!!.content)
            isListMode = currentNote!!.isList
        } else if (initialContent != null) {
            etCuerpo.setText(initialContent)
        }

        onBackPressedDispatcher.addCallback(this) {
            handleSave()
        }

        updateListModeUI()
        updateContador()
        setupListeners()
    }

    private fun setupListeners() {
        findViewById<View>(R.id.btn_back).setOnClickListener {
            handleSave()
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
            btnListMode.setTextColor(Color.parseColor("#7A4A00"))
            etCuerpo.setTextColor(Color.BLACK)
            
            // If starting a new list, add first star
            if (etCuerpo.text.isEmpty()) {
                etCuerpo.setText("★ ")
                etCuerpo.setSelection(2)
            }
        } else {
            btnListMode.text = "List Mode"
            btnListMode.setTextColor(Color.parseColor("#6C63FF"))
            etCuerpo.setTextColor(Color.parseColor("#3B78E7"))
            cardCuerpo.setBackgroundResource(R.drawable.bg_card_nota)
        }
    }

    private fun updateContador() {
        tvContador.text = etCuerpo.text.length.toString()
    }

    private fun handleSave() {
        if (currentNote != null) {
            saveNote(currentNote!!.folder)
            finish()
        } else {
            showSaveModal()
        }
    }

    private fun showSaveModal() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.guardarnota, null)
        val dialog = AlertDialog.Builder(this, R.style.CustomDialogTheme)
            .setView(dialogView)
            .create()

        val container = dialogView.findViewById<LinearLayout>(R.id.container_modal_carpetas)
        val typeface = ResourcesCompat.getFont(this, R.font.hey_comic)
        var selectedFolder = "Personal"

        fun refreshFolders() {
            container.removeAllViews()
            val folders = NoteManager.getFolders()
            folders.forEach { folderName ->
                val tv = TextView(this).apply {
                    text = folderName
                    this.typeface = typeface
                    gravity = Gravity.CENTER
                    textSize = 14f
                    setPadding(0, 30, 0, 30)
                    val lp = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { setMargins(0, 10, 0, 10) }
                    layoutParams = lp
                    
                    if (folderName == selectedFolder) {
                        setBackgroundResource(R.drawable.bg_pill_categoria_selected)
                        setTextColor(Color.WHITE)
                    } else {
                        setBackgroundResource(R.drawable.bg_pill_opcion)
                        setTextColor(Color.parseColor("#3A3A45"))
                    }

                    setOnClickListener {
                        selectedFolder = folderName
                        refreshFolders()
                    }
                }
                container.addView(tv)
            }
        }

        refreshFolders()

        dialogView.findViewById<View>(R.id.btn_guardar).setOnClickListener {
            saveNote(selectedFolder)
            dialog.dismiss()
            finish()
        }

        dialogView.findViewById<View>(R.id.btn_agregar_carpeta).setOnClickListener {
            val et = dialogView.findViewById<EditText>(R.id.et_nueva_carpeta)
            val newFolder = et.text.toString()
            if (newFolder.isNotEmpty()) {
                NoteManager.addFolder(this, newFolder)
                selectedFolder = newFolder
                et.setText("")
                refreshFolders()
            }
        }

        dialog.show()

        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.9).toInt(),
            WindowManager.LayoutParams.WRAP_CONTENT
        )
    }

    private fun saveNote(folder: String) {
        val title = etTitulo.text.toString()
        val content = etCuerpo.text.toString()

        if (currentNote == null) {
            val newNote = Note(
                title = title,
                content = content,
                folder = folder,
                isList = isListMode
            )
            NoteManager.addNote(this, newNote)
        } else {
            currentNote!!.title = title
            currentNote!!.content = content
            currentNote!!.folder = folder
            currentNote!!.isList = isListMode
            currentNote!!.lastOpened = System.currentTimeMillis()
            NoteManager.updateNote(this, currentNote!!)
        }
    }
}
