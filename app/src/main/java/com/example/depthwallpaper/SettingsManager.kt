package com.example.depthwallpaper

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color

class SettingsManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("depth_wallpaper_prefs", Context.MODE_PRIVATE)

    var timeYPercent: Float
        get() = prefs.getFloat("time_y_percent", 0.32f)
        set(value) = prefs.edit().putFloat("time_y_percent", value).apply()

    var timeSize: Float
        get() = prefs.getFloat("time_size", 220f)
        set(value) = prefs.edit().putFloat("time_size", value).apply()

    var dateOffset: Float
        get() = prefs.getFloat("date_offset", 140f)
        set(value) = prefs.edit().putFloat("date_offset", value).apply()

    var timeColor: Int
        get() = prefs.getInt("time_color", Color.WHITE)
        set(value) = prefs.edit().putInt("time_color", value).apply()

    var bgPath: String?
        get() = prefs.getString("bg_path", null)
        set(value) = prefs.edit().putString("bg_path", value).apply()

    var fgPath: String?
        get() = prefs.getString("fg_path", null)
        set(value) = prefs.edit().putString("fg_path", value).apply()

    var fontPath: String?
        get() = prefs.getString("font_path", null)
        set(value) = prefs.edit().putString("font_path", value).apply()
}
