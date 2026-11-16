package com.example.depthwallpaper

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.view.MotionEvent
import android.view.View
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.hypot
import kotlin.math.max

class TouchEditorView(
    context: Context,
    private val settings: SettingsManager,
    private val bgBitmap: Bitmap?,
    private val fgBitmap: Bitmap?,
    private val clockTypeface: Typeface,
    private val dateTypeface: Typeface
) : View(context) {

    private var activeTarget = 0
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

        if (bgBitmap != null) {
            drawCenterCrop(canvas, bgBitmap, w, h)
        } else {
            canvas.drawColor(Color.parseColor("#1B1F27"))
        }

        val scaleRatio = w / 1080f
        val timePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = settings.timeColor
            textSize = settings.timeSize * scaleRatio
            typeface = clockTypeface
            textAlign = Paint.Align.CENTER
        }
        val datePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E0E0E0")
            textSize = 50f * scaleRatio
            typeface = dateTypeface
            textAlign = Paint.Align.CENTER
        }

        val now = Date()
        val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(now)
        val dateStr = SimpleDateFormat("EEE, d MMM", Locale.getDefault()).format(now)

        val clockX = w * settings.timeXPercent
        val clockY = h * settings.timeYPercent
        val dateX = w * settings.dateXPercent
        val dateY = h * settings.dateYPercent

        if (!settings.dateInFront) canvas.drawText(dateStr, dateX, dateY, datePaint)
        if (!settings.clockInFront) canvas.drawText(timeStr, clockX, clockY, timePaint)

        if (fgBitmap != null) drawCenterCrop(canvas, fgBitmap, w, h)

        if (settings.dateInFront) canvas.drawText(dateStr, dateX, dateY, datePaint)
        if (settings.clockInFront) canvas.drawText(timeStr, clockX, clockY, timePaint)

        val timeW = timePaint.measureText(timeStr)
        val timeH = timePaint.textSize * 0.75f
        canvas.drawRoundRect(
            RectF(clockX - timeW / 2f - 20f, clockY - timeH - 16f, clockX + timeW / 2f + 20f, clockY + 20f),
            16f, 16f, guidePaint
        )

        val dateW = datePaint.measureText(dateStr)
        val dateH = datePaint.textSize * 0.85f
        canvas.drawRoundRect(
            RectF(dateX - dateW / 2f - 16f, dateY - dateH - 12f, dateX + dateW / 2f + 16f, dateY + 14f),
            12f, 12f, guidePaint
        )
    }

    @SuppressLint("ClickableViewAccessibility")
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

    private fun drawCenterCrop(canvas: Canvas, bitmap: Bitmap, viewWidth: Int, viewHeight: Int) {
        val bw = bitmap.width.toFloat()
        val bh = bitmap.height.toFloat()
        if (bw <= 0 || bh <= 0) return
        val scale = max(viewWidth / bw, viewHeight / bh)
        val matrix = Matrix().apply {
            postScale(scale, scale)
            postTranslate((viewWidth - scale * bw) / 2f, (viewHeight - scale * bh) / 2f)
        }
        canvas.drawBitmap(bitmap, matrix, null)
    }
}
