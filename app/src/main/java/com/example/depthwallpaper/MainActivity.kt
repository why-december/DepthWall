package com.example.depthwallpaper

import android.annotation.SuppressLint
import android.app.Dialog
import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.MotionEvent
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
import kotlin.math.hypot
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
    private lateinit var sliderDateOffset: Slider
    private lateinit var tvDateOffsetVal: TextView

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

        // Tap preview box to launch enlarged touch-drag editor
        ivPreview.setOnClickListener {
            openFullscreenTouchEditor()
        }

        if (settings.bgPath != null) {
            tvBgStatus.text = "Background set ✓"
            tvBgStatus.setTextColor(Color.parseColor("#80D49E"))
        }
        if (settings.fgPath != null) {
            tvFgStatus.text = "Cutout loaded ✓"
            tvFgStatus.setTextColor(Color.parseColor("#80D49E"))
        }
        if (settings.fontPath != null) {
            tvFontStatus.text = "Custom clock font ✓"
            tvFontStatus.setTextColor(Color.parseColor("#80D49E"))
        }
        if (settings.dateFontPath != null) {
            tvDateFontStatus.text = "Custom date font ✓"
            tvDateFontStatus.setTextColor(Color.parseColor("#80D49E"))
        }

        findViewById<MaterialCardView>(R.id.cardPickBg).setOnClickListener { pickBgLauncher.launch("image/*") }
        findViewById<MaterialCardView>(R.id.cardPickFg).setOnClickListener { pickFgLauncher.launch("image/png") }
        findViewById<MaterialCardView>(R.id.cardPickFont).setOnClickListener { pickFontLauncher.launch("*/*") }
        findViewById<MaterialCardView>(R.id.cardPickDateFont).setOnClickListener { pickDateFontLauncher.launch("*/*") }

        val cbClockInFront = findViewById<MaterialCheckBox>(R.id.cbClockInFront)
        val cbDateInFront = findViewById<MaterialCheckBox>(R.id.cbDateInFront)

        cbClockInFront.isChecked = settings.clockInFront
        cbDateInFront.isChecked = settings.dateInFront

        cbClockInFront.setOnCheckedChangeListener { _, isChecked ->
            settings.clockInFront = isChecked
            updatePreview()
        }

        cbDateInFront.setOnCheckedChangeListener { _, isChecked ->
            settings.dateInFront = isChecked
            updatePreview()
        }

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

        chipCustom.setOnClickListener {
            chipCustom.isChecked = true
            showCustomColorDialog(chipCustom)
        }

        // Vertical Slider + Reset (Also resets X back to center)
        tvTimeYVal = findViewById(R.id.tvTimeYVal)
        sliderTimeY = findViewById(R.id.sliderTimeY)
        val btnResetTimeY = findViewById<TextView>(R.id.btnResetTimeY)

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
        btnResetTimeY.setOnClickListener {
            sliderTimeY.value = 32f
            settings.timeXPercent = 0.50f
            settings.timeYPercent = 0.32f
            settings.dateXPercent = 0.50f
            settings.dateYPercent = 0.24f
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

        // Date Spacing Slider + Reset
        tvDateOffsetVal = findViewById(R.id.tvDateOffsetVal)
        sliderDateOffset = findViewById(R.id.sliderDateOffset)
        val btnResetDateOffset = findViewById<TextView>(R.id.btnResetDateOffset)

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
        btnResetDateOffset.setOnClickListener {
            sliderDateOffset.value = 140f
            settings.dateOffset = 140f
            settings.dateXPercent = settings.timeXPercent
            settings.dateYPercent = (settings.timeYPercent - (140f / 1920f)).coerceIn(0.05f, 0.95f)
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
        loadTypefaces()
        ivPreview.post { updatePreview() }
    }

    override fun onResume() {
        super.onResume()
        loadBitmaps()
        loadTypefaces()
        ivPreview.post { updatePreview() }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun openFullscreenTouchEditor() {
        val dialog = Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen)

        val root = FrameLayout(this).apply {
            setBackgroundColor(Color.parseColor("#0E1116"))
        }

        val interactiveView = object : View(this) {
            private var activeTarget = 0 // 0 = none, 1 = clock, 2 = date
            private var lastTouchX = 0f
            private var lastTouchY = 0f

            private val guidePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#80D0BCFF")
                style = Paint.Style.STROKE
                strokeWidth = 3f
                pathEffect = DashPathEffect(floatArrayOf(14f, 10f), 0f)
            }

            override fun onDraw(canvas: Canvas) {
                super.onDraw(canvas)
                val w = width
                val h = height
                if (w <= 0 || h <= 0) return

                // 1. Background
                val bg = previewBgBitmap
                if (bg != null) {
                    drawCenterCrop(canvas, bg, w, h)
                } else {
                    canvas.drawColor(Color.parseColor("#1B1F27"))
                }

                val scaleRatio = w / 1080f
                val timePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = settings.timeColor
                    textSize = settings.timeSize * scaleRatio
                    typeface = previewClockTypeface
                    textAlign = Paint.Align.CENTER
                }
                val datePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#E0E0E0")
                    textSize = 50f * scaleRatio
                    typeface = previewDateTypeface
                    textAlign = Paint.Align.CENTER
                }

                val now = Date()
                val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(now)
                val dateStr = SimpleDateFormat("EEE, d MMM", Locale.getDefault()).format(now)

                val clockX = w * settings.timeXPercent
                val clockY = h * settings.timeYPercent
                val dateX = w * settings.dateXPercent
                val dateY = h * settings.dateYPercent

                // 2. Behind Cutout
                if (!settings.dateInFront) canvas.drawText(dateStr, dateX, dateY, datePaint)
                if (!settings.clockInFront) canvas.drawText(timeStr, clockX, clockY, timePaint)

                // 3. Foreground Cutout
                val fg = previewFgBitmap
                if (fg != null) drawCenterCrop(canvas, fg, w, h)

                // 4. In Front of Cutout
                if (settings.dateInFront) canvas.drawText(dateStr, dateX, dateY, datePaint)
                if (settings.clockInFront) canvas.drawText(timeStr, clockX, clockY, timePaint)

                // 5. Draw subtle dashed bounding boxes so elements are easy to grab
                val timeWidth = timePaint.measureText(timeStr)
                val timeHeight = timePaint.textSize * 0.75f
                canvas.drawRoundRect(
                    RectF(clockX - timeWidth / 2f - 20f, clockY - timeHeight - 16f, clockX + timeWidth / 2f + 20f, clockY + 20f),
                    16f, 16f, guidePaint
                )

                val dateWidth = datePaint.measureText(dateStr)
                val dateHeight = datePaint.textSize * 0.85f
                canvas.drawRoundRect(
                    RectF(dateX - dateWidth / 2f - 16f, dateY - dateHeight - 12f, dateX + dateWidth / 2f + 16f, dateY + 14f),
                    12f, 12f, guidePaint
                )
            }

            override fun onTouchEvent(event: MotionEvent): Boolean {
                val w = width.toFloat()
                val h = height.toFloat()
                if (w <= 0f || h <= 0f) return false

                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        lastTouchX = event.x
                        lastTouchY = event.y

                        val clockX = w * settings.timeXPercent
                        val clockY = h * settings.timeYPercent - 40f
                        val dateX = w * settings.dateXPercent
                        val dateY = h * settings.dateYPercent - 15f

                        val distClock = hypot(event.x - clockX, event.y - clockY)
                        val distDate = hypot(event.x - dateX, event.y - dateY)

                        activeTarget = when {
                            distDate < 150f && distDate < distClock -> 2
                            distClock < 260f -> 1
                            else -> 0
                        }
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = (event.x - lastTouchX) / w
                        val dy = (event.y - lastTouchY) / h
                        lastTouchX = event.x
                        lastTouchY = event.y

                        if (activeTarget == 1) {
                            settings.timeXPercent = (settings.timeXPercent + dx).coerceIn(0.05f, 0.95f)
                            settings.timeYPercent = (settings.timeYPercent + dy).coerceIn(0.08f, 0.95f)
                            invalidate()
                        } else if (activeTarget == 2) {
                            settings.dateXPercent = (settings.dateXPercent + dx).coerceIn(0.05f, 0.95f)
                            settings.dateYPercent = (settings.dateYPercent + dy).coerceIn(0.05f, 0.95f)
                            invalidate()
                        }
                        return true
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        activeTarget = 0
                        return true
                    }
                }
                return super.onTouchEvent(event)
            }
        }

        // Top instructional banner
        val hintBanner = TextView(this).apply {
            text = "Drag Clock or Date freely anywhere"
            setTextColor(Color.WHITE)
            textSize = 14f
            setBackgroundColor(Color.parseColor("#990E1116"))
            setPadding(36, 20, 36, 20)
            gravity = Gravity.CENTER
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.TOP or Gravity.CENTER_HORIZONTAL
            ).apply { topMargin = 48 }
        }

        // Done Button at bottom
        val doneBtn = MaterialButton(this).apply {
            text = "Done Positioning"
            setTextColor(Color.parseColor("#381E72"))
            setBackgroundColor(Color.parseColor("#D0BCFF"))
            cornerRadius = 56
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            ).apply { bottomMargin = 56 }
            setOnClickListener {
                val clampedPercent = (settings.timeYPercent * 100f).coerceIn(10f, 80f)
                sliderTimeY.value = clampedPercent
                tvTimeYVal.text = "${clampedPercent.toInt()}%"
                updatePreview()
                dialog.dismiss()
            }
        }

        root.addView(interactiveView)
        root.addView(hintBanner)
        root.addView(doneBtn)

        dialog.setContentView(root)
        dialog.show()
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
            layoutParams = Lin
