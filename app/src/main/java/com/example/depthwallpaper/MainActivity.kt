package com.example.depthwallpaper

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {

    private lateinit var settings: SettingsManager

    private val pickBgLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { saveFileToInternal(it, "bg.png")?.let { path -> settings.bgPath = path } }
    }

    private val pickFgLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { saveFileToInternal(it, "fg.png")?.let { path -> settings.fgPath = path } }
    }

    private val pickFontLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { saveFileToInternal(it, "custom_font.ttf")?.let { path -> settings.fontPath = path } }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        settings = SettingsManager(this)

        findViewById<Button>(R.id.btnPickBg).setOnClickListener { pickBgLauncher.launch("image/*") }
        findViewById<Button>(R.id.btnPickFg).setOnClickListener { pickFgLauncher.launch("image/png") }
        findViewById<Button>(R.id.btnPickFont).setOnClickListener { pickFontLauncher.launch("*/*") }

        val sbTimeY = findViewById<SeekBar>(R.id.sbTimeY)
        sbTimeY.progress = (settings.timeYPercent * 100).toInt()
        sbTimeY.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) settings.timeYPercent = progress / 100f
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        val sbTimeSize = findViewById<SeekBar>(R.id.sbTimeSize)
        sbTimeSize.progress = settings.timeSize.toInt()
        sbTimeSize.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) settings.timeSize = progress.coerceAtLeast(40).toFloat()
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        val sbDateOffset = findViewById<SeekBar>(R.id.sbDateOffset)
        sbDateOffset.progress = settings.dateOffset.toInt()
        sbDateOffset.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) settings.dateOffset = progress.toFloat()
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        findViewById<Button>(R.id.btnSetWallpaper).setOnClickListener {
            val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
                putExtra(
                    WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                    ComponentName(this@MainActivity, DepthWallpaperService::class.java)
                )
            }
            startActivity(intent)
        }
    }

    private fun saveFileToInternal(uri: Uri, filename: String): String? {
        return try {
            val destination = File(filesDir, filename)
            contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destination).use { output ->
                    input.copyTo(output)
                }
            }
            Toast.makeText(this, "Loaded $filename", Toast.LENGTH_SHORT).show()
            destination.absolutePath
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to load file", Toast.LENGTH_SHORT).show()
            null
        }
    }
}
