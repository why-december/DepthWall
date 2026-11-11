package com.example.depthwallpaper

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.slider.Slider
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {

    private lateinit var settings: SettingsManager

    private lateinit var tvBgStatus: TextView
    private lateinit var tvFgStatus: TextView
    private lateinit var tvFontStatus: TextView

    private val pickBgLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            saveFileToInternal(it, "bg.png")?.let { path ->
                settings.bgPath = path
                tvBgStatus.text = "Background set ✓"
                tvBgStatus.setTextColor(Color.parseColor("#80D49E"))
            }
        }
    }

    private val pickFgLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            saveFileToInternal(it, "fg.png")?.let { path ->
                settings.fgPath = path
                tvFgStatus.text = "Cutout loaded ✓"
                tvFgStatus.setTextColor(Color.parseColor("#80D49E"))
            }
        }
    }

    private val pickFontLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            saveFileToInternal(it, "custom_font.ttf")?.let { path ->
                settings.fontPath = path
                tvFontStatus.text = "Custom font loaded ✓"
                tvFontStatus.setTextColor(Color.parseColor("#80D49E"))
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        settings = SettingsManager(this)

        tvBgStatus = findViewById(R.id.tvBgStatus)
        tvFgStatus = findViewById(R.id.tvFgStatus)
        tvFontStatus = findViewById(R.id.tvFontStatus)

        // Show status if assets are already chosen
        if (settings.bgPath != null) {
            tvBgStatus.text = "Background set ✓"
            tvBgStatus.setTextColor(Color.parseColor("#80D49E"))
        }
        if (settings.fgPath != null) {
            tvFgStatus.text = "Cutout loaded ✓"
            tvFgStatus.setTextColor(Color.parseColor("#80D49E"))
        }
        if (settings.fontPath != null) {
            tvFontStatus.text = "Custom font loaded ✓"
            tvFontStatus.setTextColor(Color.parseColor("#80D49E"))
        }

        // Card button click listeners
        findViewById<MaterialCardView>(R.id.cardPickBg).setOnClickListener { pickBgLauncher.launch("image/*") }
        findViewById<MaterialCardView>(R.id.cardPickFg).setOnClickListener { pickFgLauncher.launch("image/png") }
        findViewById<MaterialCardView>(R.id.cardPickFont).setOnClickListener { pickFontLauncher.launch("*/*") }

        // Color palette selection
        val chipGroup = findViewById<ChipGroup>(R.id.chipGroupColors)
        val colors = mapOf(
            R.id.chipWhite to Color.WHITE,
            R.id.chipSunset to Color.parseColor("#FF8A65"),
            R.id.chipLavender to Color.parseColor("#D0BCFF"),
            R.id.chipMint to Color.parseColor("#69F0AE"),
            R.id.chipGold to Color.parseColor("#FFD54F")
        )

        colors.forEach { (chipId, color) ->
            if (settings.timeColor == color) {
                findViewById<Chip>(chipId)?.isChecked = true
            }
        }

        chipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isNotEmpty()) {
                colors[checkedIds[0]]?.let { color ->
                    settings.timeColor = color
                }
            }
        }

        // Vertical height slider
        val tvTimeYVal = findViewById<TextView>(R.id.tvTimeYVal)
        val sliderTimeY = findViewById<Slider>(R.id.sliderTimeY)
        val currentPercent = (settings.timeYPercent * 100f).coerceIn(10f, 80f)
        sliderTimeY.value = currentPercent
        tvTimeYVal.text = "${currentPercent.toInt()}%"
        sliderTimeY.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                settings.timeYPercent = value / 100f
                tvTimeYVal.text = "${value.toInt()}%"
            }
        }

        // Size slider
        val tvTimeSizeVal = findViewById<TextView>(R.id.tvTimeSizeVal)
        val sliderTimeSize = findViewById<Slider>(R.id.sliderTimeSize)
        val currentSize = settings.timeSize.coerceIn(80f, 380f)
        sliderTimeSize.value = currentSize
        tvTimeSizeVal.text = "${currentSize.toInt()} px"
        sliderTimeSize.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                settings.timeSize = value
                tvTimeSizeVal.text = "${value.toInt()} px"
            }
        }

        // Date offset slider
        val tvDateOffsetVal = findViewById<TextView>(R.id.tvDateOffsetVal)
        val sliderDateOffset = findViewById<Slider>(R.id.sliderDateOffset)
        val currentOffset = settings.dateOffset.coerceIn(40f, 260f)
        sliderDateOffset.value = currentOffset
        tvDateOffsetVal.text = "${currentOffset.toInt()} px"
        sliderDateOffset.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                settings.dateOffset = value
                tvDateOffsetVal.text = "${value.toInt()} px"
            }
        }

        // Bottom CTA
        findViewById<ExtendedFloatingActionButton>(R.id.btnApplyWallpaper).setOnClickListener {
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
            Toast.makeText(this, "Loaded asset", Toast.LENGTH_SHORT).show()
            destination.absolutePath
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to load file", Toast.LENGTH_SHORT).show()
            null
        }
    }
}
