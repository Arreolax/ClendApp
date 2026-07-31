package com.example.clendapp

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.content.res.ResourcesCompat
import java.text.SimpleDateFormat
import java.util.*

class MisNotas : AppCompatActivity() {

    private lateinit var containerNotas: LinearLayout
    private var currentFolder = "All"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.misnotas)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        NoteManager.loadNotes(this)
        containerNotas = findViewById(R.id.container_notas)

        findViewById<View>(R.id.btn_back).setOnClickListener { finish() }

        setupFolders()
        setupNewNoteCard()
        refreshNotes()
    }

    override fun onResume() {
        super.onResume()
        NoteManager.loadNotes(this)
        refreshNotes()
    }

    private fun setupFolders() {
        val container = findViewById<LinearLayout>(R.id.container_tags)
        val folders = listOf("All") + NoteManager.getFolders()
        val typeface = ResourcesCompat.getFont(this, R.font.hey_comic)
        
        container.removeAllViews()
        folders.forEach { folderName ->
            val tv = TextView(this).apply {
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
                    setTextColor(Color.WHITE)
                } else {
                    setBackgroundResource(R.drawable.bg_pill_categoria)
                    setTextColor(Color.parseColor("#6B6B76"))
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
        setupFolders()
        containerNotas.removeAllViews()
        val notes = NoteManager.getNotesByFolder(currentFolder)

        // If current folder was deleted because it became empty, switch to All
        if (currentFolder != "All" && currentFolder !in NoteManager.getFolders()) {
            currentFolder = "All"
            refreshNotes()
            return
        }

        for (note in notes) {
            val noteView = LayoutInflater.from(this).inflate(R.layout.bg_card_nota_item, containerNotas, false)
            val tvTitulo = noteView.findViewById<TextView>(R.id.tv_nota_titulo)
            val tvCuerpo = noteView.findViewById<TextView>(R.id.tv_nota_cuerpo)
            val tvBadge = noteView.findViewById<TextView>(R.id.tv_badge)

            tvTitulo.text = if (note.title.isEmpty()) "Untitled" else note.title
            tvCuerpo.text = note.content
            
            val sdf = SimpleDateFormat("MMM dd, HH:mm", Locale.ENGLISH)
            tvBadge.text = sdf.format(Date(note.lastOpened))

            noteView.setOnClickListener {
                note.lastOpened = System.currentTimeMillis()
                NoteManager.updateNote(this, note)
                val intent = Intent(this, HacerNota::class.java)
                intent.putExtra("NOTE", note)
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

    private fun showMenuNota(note: Note) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.menunota, null)
        val dialog = AlertDialog.Builder(this, R.style.CustomDialogTheme)
            .setView(dialogView)
            .create()

        dialogView.findViewById<TextView>(R.id.tv_menu_titulo).text = note.title
        
        dialogView.findViewById<View>(R.id.opcion_eliminar).setOnClickListener {
            NoteManager.deleteNote(this, note.id)
            refreshNotes()
            dialog.dismiss()
        }

        dialogView.findViewById<View>(R.id.opcion_editar).setOnClickListener {
            val intent = Intent(this, HacerNota::class.java)
            intent.putExtra("NOTE", note)
            startActivity(intent)
            dialog.dismiss()
        }

        dialog.show()

        // Adjust dialog width to be 90% of screen width
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.9).toInt(),
            WindowManager.LayoutParams.WRAP_CONTENT
        )
    }
}
