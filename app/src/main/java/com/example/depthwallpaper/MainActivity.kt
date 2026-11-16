package com.example.depthwallpaper

import android.app.Dialog
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
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.checkbox.MaterialCheckBox
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
    private lateinit var tvDateFontStatus: TextView
    private lateinit var sliderTimeY: Slider
    private lateinit var tvTimeYVal: TextView

    private var previewBgBitmap: Bitmap? = null
    private var previewFgBitmap: Bitmap? = null
    private var previewClockTypeface: Typeface = Typeface.DEFAULT_BOLD
    private var previewDateTypeface: Typeface = Typeface.DEFAULT

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
                tvFontStatus.text = "Custom clock font ✓"
                tvFontStatus.setTextColor(Color.parseColor("#80D49E"))
                loadTypefaces()
                updatePreview()
            }
        }
    }

    private val pickDateFontLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            saveFileToInternal(it, "custom_date_font.ttf")?.let { path ->
                settings.dateFontPath = path
                tvDateFontStatus.text = "Custom date font ✓"
                tvDateFontStatus.setTextColor(Color.parseColor("#80D49E"))
                loadTypefaces()
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
        tvDateFontStatus = findViewById(R.id.tvDateFontStatus)

        ivPreview.setOnClickListener { openFullscreenTouchEditor() }

        if (settings.bgPath != null) { tvBgStatus.text = "Background set ✓"; tvBgStatus.setTextColor(Color.parseColor("#80D49E")) }
        if (settings.fgPath != null) { tvFgStatus.text = "Cutout loaded ✓"; tvFgStatus.setTextColor(Color.parseColor("#80D49E")) }
        if (settings.fontPath != null) { tvFontStatus.text = "Custom clock font ✓"; tvFontStatus.setTextColor(Color.parseColor("#80D49E")) }
        if (settings.dateFontPath != null) { tvDateFontStatus.text = "Custom date font ✓"; tvDateFontStatus.setTextColor(Color.parseColor("#80D49E")) }

        findViewById<MaterialCardView>(R.id.cardPickBg).setOnClickListener { pickBgLauncher.launch("image/*") }
        findViewById<MaterialCardView>(R.id.cardPickFg).setOnClickListener { pickFgLauncher.launch("image/png") }
        findViewById<MaterialCardView>(R.id.cardPickFont).setOnClickListener { pickFontLauncher.launch("*/*") }
        findViewById<MaterialCardView>(R.id.cardPickDateFont).setOnClickListener { pickDateFontLauncher.launch("*/*") }

        val cbClockInFront = findViewById<MaterialCheckBox>(R.id.cbClockInFront)
        val cbDateInFront = findViewById<MaterialCheckBox>(R.id.cbDateInFront)
        cbClockInFront.isChecked = settings.clockInFront
        cbDateInFront.isChecked = settings.dateInFront
        cbClockInFront.setOnCheckedChangeListener { _, isChecked -> settings.clockInFront = isChecked; updatePreview() }
        cbDateInFront.setOnCheckedChangeListener { _, isChecked -> settings.dateInFront = isChecked; updatePreview() }

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
            if (settings.timeColor == color) { findViewById<Chip>(chipId)?.isChecked = true; matchedPreset = true }
        }
        if (!matchedPreset) chipCustom.isChecked = true

        chipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isNotEmpty()) {
                val id = checkedIds[0]
                if (id == R.id.chipCustom) showCustomColorDialog(chipCustom)
                else presetColors[id]?.let { settings.timeColor = it; updatePreview() }
            }
        }
        chipCustom.setOnClickListener { chipCustom.isChecked = true; showCustomColorDialog(chipCustom) }

        tvTimeYVal = findViewById(R.id.tvTimeYVal)
        sliderTimeY = findViewById(R.id.sliderTimeY)
        val currentPercent = (settings.timeYPercent * 100f).coerceIn(10f, 80f)
        sliderTimeY.value = currentPercent
        tvTimeYVal.text = "${currentPercent.toInt()}%"
        sliderTimeY.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                val deltaY = (value / 100f) - settings.timeYPercent
                settings.timeYPercent = value / 100f
                settings.dateYPercent = (settings.dateYPercent + deltaY).coerceIn(0.05f, 0.95f)
                tvTimeYVal.text = "${value.toInt()}%"
                updatePreview()
            }
        }
        findViewById<TextView>(R.id.btnResetTimeY).setOnClickListener {
            sliderTimeY.value = 32f
            settings.timeXPercent = 0.50f; settings.timeYPercent = 0.32f
            settings.dateXPercent = 0.50f; settings.dateYPercent = 0.24f
            tvTimeYVal.text = "32%"
            updatePreview()
        }

        val tvTimeSizeVal = findViewById<TextView>(R.id.tvTimeSizeVal)
        val sliderTimeSize = findViewById<Slider>(R.id.sliderTimeSize)
        val currentSize = settings.timeSize.coerceIn(80f, 380f)
        sliderTimeSize.value = currentSize
        tvTimeSizeVal.text = "${currentSize.toInt()} px"
        sliderTimeSize.addOnChangeListener { _, value, fromUser ->
            if (fromUser) { settings.timeSize = value; tvTimeSizeVal.text = "${value.toInt()} px"; updatePreview() }
        }
        findViewById<TextView>(R.id.btnResetTimeSize).setOnClickListener {
            sliderTimeSize.value = 220f; settings.timeSize = 220f; tvTimeSizeVal.text = "220 px"; updatePreview()
        }

        val tvDateOffsetVal = findViewById<TextView>(R.id.tvDateOffsetVal)
        val sliderDateOffset = findViewById<Slider>(R.id.sliderDateOffset)
        val currentOffset = settings.dateOffset.coerceIn(40f, 260f)
        sliderDateOffset.value = currentOffset
        tvDateOffsetVal.text = "${currentOffset.toInt()} px"
        sliderDateOffset.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                settings.dateOffset = value
                settings.dateYPercent = (settings.timeYPercent - (value / 1920f)).coerceIn(0.05f, 0.95f)
                tvDateOffsetVal.text = "${value.toInt()} px"
                updatePreview()
            }
        }
        findViewById<TextView>(R.id.btnResetDateOffset).setOnClickListener {
            sliderDateOffset.value = 140f; settings.dateOffset = 140f
            settings.dateXPercent = settings.timeXPercent
            settings.dateYPercent = (settings.timeYPercent - (140f / 1920f)).coerceIn(0.05f, 0.95f)
            tvDateOffsetVal.text = "140 px"; updatePreview()
        }

        findViewById<ExtendedFloatingActionButton>(R.id.btnApplyWallpaper).setOnClickListener {
            startActivity(Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
                putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT, ComponentName(this@MainActivity, DepthWallpaperService::class.java))
            })
        }

        loadBitmaps()
        loadTypefaces()
        ivPreview.post { updatePreview() }
    }

    override fun onResume() {
        super.onResume()
        loadBitmaps(); loadTypefaces()
        ivPreview.post { updatePreview() }
    }

    private fun openFullscreenTouchEditor() {
        val dialog = Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        val root = FrameLayout(this).apply { setBackgroundColor(Color.parseColor("#0E1116")) }

        val editorView = TouchEditorView(this, settings, previewBgBitmap, previewFgBitmap, previewClockTypeface, previewDateTypeface)

        val hintBanner = TextView(this).apply {
            text = "Drag Clock or Date freely anywhere"
            setTextColor(Color.WHITE); textSize = 14f
            setBackgroundColor(Color.parseColor("#990E1116")); setPadding(36, 20, 36, 20)
            layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP or Gravity.CENTER_HORIZONTAL).apply { topMargin = 48 }
        }

        val doneBtn = MaterialButton(this).apply {
            text = "Done Positioning"
            setTextColor(Color.parseColor("#381E72")); setBackgroundColor(Color.parseColor("#D0BCFF")); cornerRadius = 56
            layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL).apply { bottomMargin = 56 }
            setOnClickListener {
                val clamped = (settings.timeYPercent * 100f).coerceIn(10f, 80f)
                sliderTimeY.value = clamped; tvTimeYVal.text = "${clamped.toInt()}%"
                updatePreview(); dialog.dismiss()
            }
        }

        root.addView(editorView); root.addView(hintBanner); root.addView(doneBtn)
        dialog.setContentView(root); dialog.show()
    }

    private fun showCustomColorDialog(customChip: Chip) {
        val current = settings.timeColor
        var r = Color.red(current); var g = Color.green(current); var b = Color.blue(current)
        val layout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(56, 36, 56, 16) }
        val swatch = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 110).apply { bottomMargin = 32 }
            background = GradientDrawable().apply { cornerRadius = 24f; setColor(current) }
        }
        layout.addView(swatch)

        fun addSlider(label: String, initial: Int, onChange: (Int) -> Unit) {
            val tv = TextView(this).apply { text = "$label: $initial"; setTextColor(Color.parseColor("#E0E2EC")); textSize = 13f }
            val sb = SeekBar(this).apply {
                max = 255; progress = initial
                setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(s: SeekBar?, p: Int, fromUser: Boolean) {
                        tv.text = "$label: $p"; onChange(p)
                        (swatch.background as GradientDrawable).setColor(Color.rgb(r, g, b))
                    }
                    override fun onStartTrackingTouch(s: SeekBar?) {}
                    override fun onStopTrackingTouch(s: SeekBar?) {}
                })
            }
            layout.addView(tv); layout.addView(sb)
        }
        addSlider("Red", r) { r = it }; addSlider("Green", g) { g = it }; addSlider("Blue", b) { b = it }

        MaterialAlertDialogBuilder(this).setTitle("Custom Clock Color").setView(layout)
            .setPositiveButton("Apply") { _, _ -> settings.timeColor = Color.rgb(r, g, b); customChip.isChecked = true; updatePreview() }
            .setNegativeButton("Cancel", null).show()
    }

    private fun loadBitmaps() {
        settings.bgPath?.let { File(it).takeIf { f -> f.exists() }?.let { f -> previewBgBitmap = BitmapFactory.decodeFile(f.absolutePath) } }
        settings.fgPath?.let { File(it).takeIf { f -> f.exists() }?.let { f -> previewFgBitmap = BitmapFactory.decodeFile(f.absolutePath) } }
    }

    private fun loadTypefaces() {
        settings.fontPath?.let { File(it).takeIf { f -> f.exists() }?.let { f -> try { previewClockTypeface = Typeface.createFromFile(f) } catch (_: Exception) {} } }
        settings.dateFontPath?.let { File(it).takeIf { f -> f.exists() }?.let { f -> try { previewDateTypeface = Typeface.createFromFile(f) } catch (_: Exception) {} } }
    }

    private fun updatePreview() {
        val width = 360; val height = 640
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)

        previewBgBitmap?.let { drawCenterCrop(canvas, it, width, height) } ?: canvas.drawColor(Color.parseColor("#1B1F27"))

        val scaleRatio = width / 1080f
        val timePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = settings.timeColor; textSize = settings.timeSize * scaleRatio; typeface = previewClockTypeface; textAlign = Paint.Align.CENTER }
        val datePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#E0E0E0"); textSize = 50f * scaleRatio; typeface = previewDateTypeface; textAlign = Paint.Align.CENTER }

        val now = Date()
        val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(now)
        val dateStr = SimpleDateFormat("EEE, d MMM", Locale.getDefault()).format(now)

        val clockX = width * settings.timeXPercent; val clockY = height * settings.timeYPercent
        val dateX = width * settings.dateXPercent; val dateY = height * settings.dateYPercent

        if (!settings.dateInFront) canvas.drawText(dateStr, dateX, dateY, datePaint)
        if (!settings.clockInFront) canvas.drawText(timeStr, clockX, clockY, timePaint)
        previewFgBitmap?.let { drawCenterCrop(canvas, it, width, height) }
        if (settings.dateInFront) canvas.drawText(dateStr, dateX, dateY, datePaint)
        if (settings.clockInFront) canvas.drawText(timeStr, clockX, clockY, timePaint)

        ivPreview.setImageBitmap(bmp)
    }

    private fun drawCenterCrop(canvas: Canvas, bitmap: Bitmap, viewWidth: Int, viewHeight: Int) {
        val bw = bitmap.width.toFloat(); val bh = bitmap.height.toFloat()
        if (bw <= 0 || bh <= 0) return
        val scale = max(viewWidth / bw, viewHeight / bh)
        val matrix = Matrix().apply { postScale(scale, scale); postTranslate((viewWidth - scale * bw) / 2f, (viewHeight - scale * bh) / 2f) }
        canvas.drawBitmap(bitmap, matrix, null)
    }

    private fun saveFileToInternal(uri: Uri, filename: String): String? {
        return try {
            val dest = File(filesDir, filename)
            contentResolver.openInputStream(uri)?.use { input -> FileOutputStream(dest).use { output -> input.copyTo(output) } }
            dest.absolutePath
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to load file", Toast.LENGTH_SHORT).show(); null
        }
    }
}
