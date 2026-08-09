package com.example.clendapp

import android.content.Context
import android.content.res.ColorStateList
import android.media.MediaPlayer
import android.os.Bundle
import android.widget.ImageView
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.ImageViewCompat
import androidx.lifecycle.lifecycleScope
import com.example.clendapp.data.AppDatabase
import kotlinx.coroutines.launch

class AjustesActivity : AppCompatActivity() {

    private lateinit var rgMusicSelection: RadioGroup
    private lateinit var tvMusicStatus: TextView
    private lateinit var rbMusicDefault: RadioButton
    private lateinit var rbMusicSmetana: RadioButton
    private lateinit var rbMusicVivaldi: RadioButton
    private lateinit var rbMusicRingtone: RadioButton

    private lateinit var ivPreviewDefault: ImageView
    private lateinit var ivPreviewSmetana: ImageView
    private lateinit var ivPreviewVivaldi: ImageView
    private lateinit var ivPreviewRingtone: ImageView

    private var mediaPlayer: MediaPlayer? = null
    private var currentPlayingId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_ajustes)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        findViewById<ImageView>(R.id.btn_back).setOnClickListener {
            finish()
        }

        rgMusicSelection = findViewById(R.id.rg_music_selection)
        tvMusicStatus = findViewById(R.id.tv_music_status)
        rbMusicDefault = findViewById(R.id.rb_music_default)
        rbMusicSmetana = findViewById(R.id.rb_music_smetana)
        rbMusicVivaldi = findViewById(R.id.rb_music_vivaldi)
        rbMusicRingtone = findViewById(R.id.rb_music_ringtone)

        ivPreviewDefault = findViewById(R.id.iv_preview_default)
        ivPreviewSmetana = findViewById(R.id.iv_preview_smetana)
        ivPreviewVivaldi = findViewById(R.id.iv_preview_vivaldi)
        ivPreviewRingtone = findViewById(R.id.iv_preview_ringtone)

        checkMusicUnlockStatus()
        setupMusicSelection()
        setupPreviews()
    }

    private fun checkMusicUnlockStatus() {
        val sharedPref = getSharedPreferences("clend_app_prefs", Context.MODE_PRIVATE)
        val userId = sharedPref.getInt("user_id", -1)

        rbMusicDefault.isEnabled = true
        rbMusicSmetana.isEnabled = true
        rbMusicRingtone.isEnabled = true

        if (userId != -1) {
            val db = AppDatabase.getDatabase(this)
            lifecycleScope.launch {
                val scores = db.scoresDao().getScoresByUserId(userId)
                val isVivaldiUnlocked = scores?.music_unlocked ?: false

                if (isVivaldiUnlocked) {
                    rbMusicVivaldi.isEnabled = true
                    ivPreviewVivaldi.isEnabled = true
                    ivPreviewVivaldi.alpha = 1.0f
                    tvMusicStatus.text = getString(R.string.music_all_unlocked)
                    tvMusicStatus.setTextColor(0xFF7E70FF.toInt())
                } else {
                    rbMusicVivaldi.isEnabled = false
                    ivPreviewVivaldi.isEnabled = false
                    ivPreviewVivaldi.alpha = 0.5f
                    tvMusicStatus.text = getString(R.string.music_unlock_hint)
                }
            }
        }
    }

    private fun setupMusicSelection() {
        val sharedPref = getSharedPreferences("clend_app_prefs", Context.MODE_PRIVATE)
        val selectedMusic = sharedPref.getString("selected_music", "subwooferlullaby")

        when (selectedMusic) {
            "subwooferlullaby" -> rbMusicDefault.isChecked = true
            "smetana" -> rbMusicSmetana.isChecked = true
            "vivaldi" -> rbMusicVivaldi.isChecked = true
            "cuteringtone2" -> rbMusicRingtone.isChecked = true
        }

        val radioButtons = listOf(rbMusicDefault, rbMusicSmetana, rbMusicVivaldi, rbMusicRingtone)
        radioButtons.forEach { rb ->
            rb.setOnClickListener {
                radioButtons.forEach { it.isChecked = (it == rb) }
                saveSelection(rb.id)
            }
        }
    }

    private fun saveSelection(checkedId: Int) {
        val sharedPref = getSharedPreferences("clend_app_prefs", Context.MODE_PRIVATE)
        val musicName = when (checkedId) {
            R.id.rb_music_default -> "subwooferlullaby"
            R.id.rb_music_smetana -> "smetana"
            R.id.rb_music_vivaldi -> "vivaldi"
            R.id.rb_music_ringtone -> "cuteringtone2"
            else -> "subwooferlullaby"
        }

        with(sharedPref.edit()) {
            putString("selected_music", musicName)
            apply()
        }
        Toast.makeText(this, getString(R.string.music_updated), Toast.LENGTH_SHORT).show()
    }

    private fun setupPreviews() {
        ivPreviewDefault.setOnClickListener { playPreview(R.raw.subwooferlullaby, ivPreviewDefault) }
        ivPreviewSmetana.setOnClickListener { playPreview(R.raw.smetana, ivPreviewSmetana) }
        ivPreviewVivaldi.setOnClickListener { playPreview(R.raw.vivaldi, ivPreviewVivaldi) }
        ivPreviewRingtone.setOnClickListener { playPreview(R.raw.cuteringtone2, ivPreviewRingtone) }
    }

    private fun playPreview(resId: Int, imageView: ImageView) {
        if (currentPlayingId == resId && mediaPlayer?.isPlaying == true) {
            stopPreview()
            return
        }

        stopPreview()

        mediaPlayer = MediaPlayer.create(this, resId)
        mediaPlayer?.setOnCompletionListener { stopPreview() }
        mediaPlayer?.start()
        currentPlayingId = resId

        ImageViewCompat.setImageTintList(imageView, ColorStateList.valueOf(0xFF7E70FF.toInt()))
    }

    private fun stopPreview() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        currentPlayingId = -1

        val previews = listOf(ivPreviewDefault, ivPreviewSmetana, ivPreviewVivaldi, ivPreviewRingtone)
        previews.forEach { 
            ImageViewCompat.setImageTintList(it, ColorStateList.valueOf(0xFFBDBDBD.toInt()))
        }
    }

    override fun onPause() {
        super.onPause()
        stopPreview()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopPreview()
    }
}
