package com.example.depthwallpaper

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DepthWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine = DepthEngine()

    inner class DepthEngine : Engine() {
        private val handler = Handler(Looper.getMainLooper())
        private var visible = false
        private lateinit var settings: SettingsManager

        private var bgBitmap: Bitmap? = null
        private var fgBitmap: Bitmap? = null
        private var customTypeface: Typeface = Typeface.DEFAULT_BOLD

        private val timePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
        }
        private val datePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E0E0E0")
            textSize = 50f
            textAlign = Paint.Align.CENTER
        }

        private val drawRunner = Runnable {
            drawFrame()
            scheduleNextTick()
        }

        override fun onCreate(surfaceHolder: SurfaceHolder?) {
            super.onCreate(surfaceHolder)
            settings = SettingsManager(applicationContext)
            loadResources()
        }

        override fun onVisibilityChanged(visible: Boolean) {
            this.visible = visible
            if (visible) {
                loadResources()
                drawFrame()
                scheduleNextTick()
            } else {
                handler.removeCallbacks(drawRunner)
            }
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder?) {
            super.onSurfaceDestroyed(holder)
            visible = false
            handler.removeCallbacks(drawRunner)
        }

        private fun loadResources() {
            settings.bgPath?.let { path ->
                val file = File(path)
                if (file.exists()) bgBitmap = BitmapFactory.decodeFile(file.absolutePath)
            }
            settings.fgPath?.let { path ->
                val file = File(path)
                if (file.exists()) fgBitmap = BitmapFactory.decodeFile(file.absolutePath)
            }
            settings.fontPath?.let { path ->
                val file = File(path)
                if (file.exists()) {
                    try {
                        customTypeface = Typeface.createFromFile(file)
                    } catch (_: Exception) {}
                }
            }
        }

        private fun scheduleNextTick() {
            handler.removeCallbacks(drawRunner)
            if (visible) {
                handler.postDelayed(drawRunner, 1000L)
            }
        }

        private fun drawFrame() {
            val holder = surfaceHolder
            var canvas: Canvas? = null
            try {
                canvas = holder.lockCanvas()
                if (canvas != null) {
                    val width = canvas.width
                    val height = canvas.height
                    val destRect = Rect(0, 0, width, height)

                    bgBitmap?.let {
                        canvas.drawBitmap(it, null, destRect, null)
                    } ?: canvas.drawColor(Color.DKGRAY)

                    timePaint.color = settings.timeColor
                    timePaint.textSize = settings.timeSize
                    timePaint.typeface = customTypeface

                    val now = Date()
                    val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(now)
                    val dateStr = SimpleDateFormat("EEE, d MMM", Locale.getDefault()).format(now)

                    val centerX = width / 2f
                    val timeY = height * settings.timeYPercent
                    val dateY = timeY - settings.dateOffset

                    canvas.drawText(dateStr, centerX, dateY, datePaint)
                    canvas.drawText(timeStr, centerX, timeY, timePaint)

                    fgBitmap?.let {
                        canvas.drawBitmap(it, null, destRect, null)
                    }
                }
            } finally {
                if (canvas != null) {
                    holder.unlockCanvasAndPost(canvas)
                }
            }
        }
    }
}
