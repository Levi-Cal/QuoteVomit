package com.aliah.quotevomit

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * The pretty, completely unrelated background.
 * Downloads a random photo from Lorem Picsum; if offline, uses a pastel gradient.
 */
object BackgroundRepository {

    private const val PREFS = "bg_prefs"
    private const val FILE = "bg.jpg"
    private const val PHOTO_URL = "https://picsum.photos/640/360"

    private val gradients = intArrayOf(
        R.drawable.bg_sunset,
        R.drawable.bg_lavender,
        R.drawable.bg_mint,
        R.drawable.bg_peach
    )

    /** Picks a new background. Call this whenever the quote changes. */
    fun refresh(context: Context) {
        val downloaded = try {
            download(context)
        } catch (_: Exception) {
            false
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean("useImage", downloaded)
            .putInt("gradient", gradients.indices.random())
            .apply()
    }

    private fun download(context: Context): Boolean {
        val conn = URL(PHOTO_URL).openConnection() as HttpURLConnection
        conn.instanceFollowRedirects = true
        conn.connectTimeout = 10_000
        conn.readTimeout = 15_000
        conn.setRequestProperty("User-Agent", "QuoteVomit/1.0 (personal hobby Android widget)")
        val bytes = try {
            conn.inputStream.use { it.readBytes() }
        } finally {
            conn.disconnect()
        }
        val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return false
        File(context.filesDir, FILE).outputStream().use {
            bmp.compress(Bitmap.CompressFormat.JPEG, 85, it)
        }
        return true
    }

    /** The downloaded photo with rounded corners, or null if we should use a gradient. */
    fun loadPhoto(context: Context): Bitmap? {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (!prefs.getBoolean("useImage", false)) return null
        val file = File(context.filesDir, FILE)
        if (!file.exists()) return null
        val src = BitmapFactory.decodeFile(file.path) ?: return null
        return roundCorners(src, radius = src.width * 0.07f)
    }

    fun gradientRes(context: Context): Int {
        val i = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt("gradient", 0)
        return gradients[i.coerceIn(gradients.indices)]
    }

    private fun roundCorners(src: Bitmap, radius: Float): Bitmap {
        val out = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = BitmapShader(src, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        }
        Canvas(out).drawRoundRect(
            RectF(0f, 0f, src.width.toFloat(), src.height.toFloat()), radius, radius, paint
        )
        return out
    }
}
