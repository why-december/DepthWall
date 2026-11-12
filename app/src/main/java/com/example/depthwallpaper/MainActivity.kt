package com.example.depthwallpaper

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.slider.Slider
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max

class MainActivity : AppCompatActivity() {

    private lateinit var settings: SettingsManager

    private lateinit var ivPreview: ImageView
    private lateinit var tvBgStatus: TextView
    private lateinit var tvFgStatus: TextView
    private lateinit var tvFontStatus: TextView

    private var previewBgBitmap: Bitmap? = null
    private var previewFgBitmap: Bitmap? = null
    private var previewTypeface: Typeface = Typeface.DEFAULT_BOLD

    private val pickBgLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            saveFileToInternal(it, "bg.png")?.let { path ->
                settings.bgPath = path
                tvBgStatus.text = "Background set ✓"
                tvBgStatus.setTextColor(Color.parseColor("#80D49E"))
                loadBitmaps()
                updatePreview()
            }
        }
    }

    private val pickFgLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            saveFileToInternal(it, "fg.png")?.let { path ->
                settings.fgPath = path
                tvFgStatus.text = "Cutout loaded ✓"
                tvFgStatus.setTextColor(Color.parseColor("#80D49E"))
                loadBitmaps()
                updatePreview()
            }
        }
    }

    private val pickFontLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            saveFileToInternal(it, "custom_font.ttf")?.let { path ->
                settings.fontPath = path
                tvFontStatus.text = "Custom font loaded ✓"
                tvFontStatus.setTextColor(Color.parseColor("#80D49E"))
                loadTypeface()
                updatePreview()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        settings = SettingsManager(this)

        ivPreview = findViewById(R.id.ivPreview)
        tvBgStatus = findViewById(R.id.tvBgStatus)
        tvFgStatus = findViewById(R.id.tvFgStatus)
        tvFontStatus = findViewById(R.id.tvFontStatus)

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

        findViewById<MaterialCardView>(R.id.cardPickBg).setOnClickListener { pickBgLauncher.launch("image/*") }
        findViewById<MaterialCardView>(R.id.cardPickFg).setOnClickListener { pickFgLauncher.launch("image/png") }
        findViewById<MaterialCardView>(R.id.cardPickFont).setOnClickListener { pickFontLauncher.launch("*/*") }

        // Color Chips + Custom Color Picker
        val chipGroup = findViewById<ChipGroup>(R.id.chipGroupColors)
        val chipCustom = findViewById<Chip>(R.id.chipCustom)
        val presetColors = mapOf(
            R.id.chipWhite to Color.WHITE,
            R.id.chipSunset to Color.parseColor("#FF8A65"),
            R.id.chipLavender to Color.parseColor("#D0BCFF"),
            R.id.chipMint to Color.parseColor("#69F0AE"),
            R.id.chipGold to Color.parseColor("#FFD54F")
        )

        var matchedPreset = false
        presetColors.forEach { (chipId, color) ->
            if (settings.timeColor == color) {
                findViewById<Chip>(chipId)?.isChecked = true
                matchedPreset = true
            }
        }
        if (!matchedPreset) {
            chipCustom.isChecked = true
        }

        chipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isNotEmpty()) {
                val id = checkedIds[0]
                if (id == R.id.chipCustom) {
                    showCustomColorDialog(chipCustom)
                } else {
                    presetColors[id]?.let { color ->
                        settings.timeColor = color
                        updatePreview()
                    }
                }
            }
        }

        // Allow re-tapping the Custom chip even when it's already selected
        chipCustom.setOnClickListener {
            chipCustom.isChecked = true
            showCustomColorDialog(chipCustom)
        }

        // Vertical Slider + Reset
        val tvTimeYVal = findViewById<TextView>(R.id.tvTimeYVal)
        val sliderTimeY = findViewById<Slider>(R.id.sliderTimeY)
        val btnResetTimeY = findViewById<TextView>(R.id.btnResetTimeY)

        val currentPercent = (settings.timeYPercent * 100f).coerceIn(10f, 80f)
        sliderTimeY.value = currentPercent
        tvTimeYVal.text = "${currentPercent.toInt()}%"
        sliderTimeY.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                settings.timeYPercent = value / 100f
                tvTimeYVal.text = "${value.toInt()}%"
                updatePreview()
            }
        }
        btnResetTimeY.setOnClickListener {
            sliderTimeY.value = 32f
            settings.timeYPercent = 0.32f
            tvTimeYVal.text = "32%"
            updatePreview()
        }

        // Size Slider + Reset
        val tvTimeSizeVal = findViewById<TextView>(R.id.tvTimeSizeVal)
        val sliderTimeSize = findViewById<Slider>(R.id.sliderTimeSize)
        val btnResetTimeSize = findViewById<TextView>(R.id.btnResetTimeSize)

        val currentSize = settings.timeSize.coerceIn(80f, 380f)
        sliderTimeSize.value = currentSize
        tvTimeSizeVal.text = "${currentSize.toInt()} px"
        sliderTimeSize.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                settings.timeSize = value
                tvTimeSizeVal.text = "${value.toInt()} px"
                updatePreview()
            }
        }
        btnResetTimeSize.setOnClickListener {
            sliderTimeSize.value = 220f
            settings.timeSize = 220f
            tvTimeSizeVal.text = "220 px"
            updatePreview()
        }

        // Date Offset Slider + Reset
        val tvDateOffsetVal = findViewById<TextView>(R.id.tvDateOffsetVal)
        val sliderDateOffset = findViewById<Slider>(R.id.sliderDateOffset)
        val btnResetDateOffset = findViewById<TextView>(R.id.btnResetDateOffset)

        val currentOffset = settings.dateOffset.coerceIn(40f, 260f)
        sliderDateOffset.value = currentOffset
        tvDateOffsetVal.text = "${currentOffset.toInt()} px"
        sliderDateOffset.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                settings.dateOffset = value
                tvDateOffsetVal.text = "${value.toInt()} px"
                updatePreview()
            }
        }
        btnResetDateOffset.setOnClickListener {
            sliderDateOffset.value = 140f
            settings.dateOffset = 140f
            tvDateOffsetVal.text = "140 px"
            updatePreview()
        }

        findViewById<ExtendedFloatingActionButton>(R.id.btnApplyWallpaper).setOnClickListener {
            val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
                putExtra(
                    WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                    ComponentName(this@MainActivity, DepthWallpaperService::class.java)
                )
            }
            startActivity(intent)
        }

        loadBitmaps()
        loadTypeface()
        ivPreview.post { updatePreview() }
    }

    override fun onResume() {
        super.onResume()
        loadBitmaps()
        loadTypeface()
        ivPreview.post { updatePreview() }
    }

    private fun showCustomColorDialog(customChip: Chip) {
        val currentColor = settings.timeColor
        var r = Color.red(currentColor)
        var g = Color.green(currentColor)
        var b = Color.blue(currentColor)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(56, 36, 56, 16)
        }

        val swatch = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                110
            ).apply { bottomMargin = 32 }
            background = GradientDrawable().apply {
                cornerRadius = 24f
                setColor(currentColor)
            }
        }
        layout.addView(swatch)

        fun updateSwatch() {
            val newColor = Color.rgb(r, g, b)
            (swatch.background as GradientDrawable).setColor(newColor)
        }

        fun addChannelSlider(label: String, initial: Int, onChange: (Int) -> Unit) {
            val tv = TextView(this).apply {
                text = "$label: $initial"
                setTextColor(Color.parseColor("#E0E2EC"))
                textSize = 13f
            }
            val sb = SeekBar(this).apply {
                max = 255
                progress = initial
                setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(s: SeekBar?, p: Int, fromUser: Boolean) {
                        tv.text = "$label: $p"
                        onChange(p)
                        updateSwatch()
                    }
                    override fun onStartTrackingTouch(s: SeekBar?) {}
                    override fun onStopTrackingTouch(s: SeekBar?) {}
                })
            }
            layout.addView(tv)
            layout.addView(sb)
        }

        addChannelSlider("Red", r) { r = it }
        addChannelSlider("Green", g) { g = it }
        addChannelSlider("Blue", b) { b = it }

        MaterialAlertDialogBuilder(this)
            .setTitle("Custom Clock Color")
            .setView(layout)
            .setPositiveButton("Apply") { _, _ ->
                val chosen = Color.rgb(r, g, b)
                settings.timeColor = chosen
                customChip.isChecked = true
                updatePreview()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun loadBitmaps() {
        settings.bgPath?.let { path ->
            val file = File(path)
            if (file.exists()) {
                previewBgBitmap = BitmapFactory.decodeFile(file.absolutePath)
            }
        }
        settings.fgPath?.let { path ->
            val file = File(path)
            if (file.exists()) {
                previewFgBitmap = BitmapFactory.decodeFile(file.absolutePath)
            }
        }
    }

    private fun loadTypeface() {
        settings.fontPath?.let { path ->
            val file = File(path)
            if (file.exists()) {
                try {
                    previewTypeface = Typeface.createFromFile(file)
                } catch (_: Exception) {}
            }
        }
    }

    private fun updatePreview() {
        val width = 360
        val height = 640
        val previewBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(previewBitmap)

        val bg = previewBgBitmap
        if (bg != null) {
            drawCenterCrop(canvas, bg, width, height)
        } else {
            canvas.drawColor(Color.parseColor("#1B1F27"))
        }

        val scaleRatio = width / 1080f
        val timePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = settings.timeColor
            textSize = settings.timeSize * scaleRatio
            typeface = previewTypeface
            textAlign = Paint.Align.CENTER
        }

        val datePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E0E0E0")
            textSize = 50f * scaleRatio
            textAlign = Paint.Align.CENTER
        }

        val now = Date()
        val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(now)
        val dateStr = SimpleDateFormat("EEE, d MMM", Locale.getDefault()).format(now)

        val centerX = width / 2f
        val timeY = height * settings.timeYPercent
        val dateY = timeY - (settings.dateOffset * scaleRatio)

        canvas.drawText(dateStr, centerX, dateY, datePaint)
        canvas.drawText(timeStr, centerX, timeY, timePaint)

        val fg = previewFgBitmap
        if (fg != null) {
            drawCenterCrop(canvas, fg, width, height)
        }

        ivPreview.setImageBitmap(previewBitmap)
    }

    private fun drawCenterCrop(canvas: Canvas, bitmap: Bitmap, viewWidth: Int, viewHeight: Int) {
        val bitmapWidth = bitmap.width.toFloat()
        val bitmapHeight = bitmap.height.toFloat()
        if (bitmapWidth <= 0 || bitmapHeight <= 0) return

        val scale = max(viewWidth / bitmapWidth, viewHeight / bitmapHeight)
        val scaledWidth = scale * bitmapWidth
        val scaledHeight = scale * bitmapHeight

        val dx = (viewWidth - scaledWidth) / 2f
        val dy = (viewHeight - scaledHeight) / 2f

        val matrix = Matrix().apply {
            postScale(scale, scale)
            postTranslate(dx, dy)
        }
        canvas.drawBitmap(bitmap, matrix, null)
    }

    private fun saveFileToInternal(uri: Uri, filename: String): String? {
        return try {
            val destination = File(filesDir, filename)
            contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destination).use { output ->
                    input.copyTo(output)
                }
            }
            destination.absolutePath
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to load file", Toast.LENGTH_SHORT).show()
            null
        }
    }
}
