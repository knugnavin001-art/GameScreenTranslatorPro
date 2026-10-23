package com.knugnavin.gametranslator

import android.accessibilityservice.AccessibilityService
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import android.view.Gravity
import android.view.MotionEvent
import android.view.accessibility.AccessibilityEvent
import android.widget.TextView
import android.widget.ScrollView
import android.widget.LinearLayout
import android.view.View
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.Executors

class ScreenTranslatorService : AccessibilityService() {
    private lateinit var wm: WindowManager
    private var bubble: TextView? = null
    private var resultView: View? = null
    private var busy = false
    private val main = Handler(Looper.getMainLooper())
    private val worker = Executors.newSingleThreadExecutor()

    override fun onServiceConnected() {
        super.onServiceConnected()
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        showBubble()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}

    private fun showBubble() {
        if (bubble != null) return
        val b = TextView(this).apply {
            text = "🌐"
            textSize = 22f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(0xFF2563EB.toInt())
                setStroke(2, Color.WHITE)
            }
            elevation = 10f
            contentDescription = "แตะเพื่อแปลหน้าจอ"
        }
        val params = WindowManager.LayoutParams(
            dp(56), dp(56),
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP or Gravity.END; x = dp(16); y = dp(180) }
        var downX = 0f
        var downY = 0f
        var startX = 0
        var startY = 0
        var dragged = false
        b.setOnTouchListener { _, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> {
                    downX = e.rawX; downY = e.rawY
                    startX = params.x; startY = params.y; dragged = false; true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (e.rawX - downX).toInt()
                    val dy = (e.rawY - downY).toInt()
                    if (kotlin.math.abs(dx) + kotlin.math.abs(dy) > dp(8)) dragged = true
                    params.x = (startX - dx).coerceAtLeast(0)
                    params.y = (startY + dy).coerceAtLeast(0)
                    wm.updateViewLayout(b, params); true
                }
                MotionEvent.ACTION_UP -> {
                    if (!dragged) captureAndTranslate()
                    true
                }
                else -> false
            }
        }
        bubble = b
        try { wm.addView(b, params) } catch (_: Exception) { bubble = null }
    }

    private fun captureAndTranslate() {
        if (busy) return
        if (Build.VERSION.SDK_INT < 30) { showResult("ต้องใช้ Android 11 ขึ้นไป"); return }
        busy = true
        showResult("กำลังอ่านข้อความบนหน้าจอ…")
        takeScreenshot(0, main, object : TakeScreenshotCallback {
            override fun onSuccess(screenshot: AccessibilityService.ScreenshotResult) {
                var bitmap: Bitmap? = null
                try {
                    val hardware = screenshot.hardwareBuffer
                    val wrapped = Bitmap.wrapHardwareBuffer(hardware, screenshot.colorSpace)
                    bitmap = wrapped?.copy(Bitmap.Config.ARGB_8888, false)
                    hardware.close()
                } catch (_: Exception) {}
                val captured = bitmap
                if (captured == null) {
                    busy = false
                    showResult("จับภาพหน้าจอไม่ได้ ลองอีกครั้ง")
                    return
                }
                val image = InputImage.fromBitmap(captured, 0)
                val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                recognizer.process(image)
                    .addOnSuccessListener { result ->
                        val extracted = result.text.trim()
                        if (extracted.isBlank()) {
                            busy = false
                            showResult("ไม่พบข้อความ ลองเปิดหน้าที่มีข้อความ")
                        } else {
                            showResult("กำลังแปล…")
                            worker.execute {
                                val translated = translateToThai(extracted)
                                main.post {
                                    busy = false
                                    showResult(if (translated.isBlank()) "แปลไม่สำเร็จ ตรวจสอบอินเทอร์เน็ตแล้วลองใหม่\n\nข้อความที่อ่านได้:\n$extracted" else translated)
                                }
                            }
                        }
                        captured.recycle()
                        recognizer.close()
                    }
                    .addOnFailureListener {
                        busy = false
                        showResult("อ่านตัวอักษรไม่สำเร็จ ลองใหม่อีกครั้ง")
                        captured.recycle()
                        recognizer.close()
                    }
            }
            override fun onFailure(errorCode: Int) {
                busy = false
                showResult("จับภาพหน้าจอไม่สำเร็จ (รหัส $errorCode) ตรวจสอบสิทธิ์การเข้าถึง")
            }
        })
    }

    private fun translateToThai(text: String): String {
        return try {
            text.lines().filter { it.isNotBlank() }.joinToString("\n") { line ->
                val q = URLEncoder.encode(line.take(450), "UTF-8")
                val conn = URL("https://api.mymemory.translated.net/get?q=$q&langpair=auto%7Cth").openConnection() as HttpURLConnection
                conn.connectTimeout = 8000
                conn.readTimeout = 10000
                conn.setRequestProperty("User-Agent", "GameScreenTranslator/1.0")
                try {
                    val body = conn.inputStream.bufferedReader().use { it.readText() }
                    JSONObject(body).optJSONObject("responseData")?.optString("translatedText").orEmpty()
                } finally { conn.disconnect() }
            }
        } catch (_: Exception) { "" }
    }

    private fun showResult(message: String) {
        main.post {
            try {
                resultView?.let { wm.removeView(it) }
                val panel = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(dp(14), dp(12), dp(14), dp(8))
                    background = GradientDrawable().apply {
                        setColor(0xF51E293B.toInt())
                        cornerRadius = dp(14).toFloat()
                        setStroke(dp(1), 0xFF60A5FA.toInt())
                    }
                }
                val heading = TextView(this).apply {
                    text = "คำแปลภาษาไทย"
                    textSize = 15f
                    setTextColor(0xFF93C5FD.toInt())
                }
                val body = TextView(this).apply {
                    text = message
                    textSize = 15f
                    setTextColor(Color.WHITE)
                    setTextIsSelectable(true)
                }
                val close = TextView(this).apply {
                    text = "ปิด"
                    gravity = Gravity.END
                    textSize = 14f
                    setTextColor(0xFF93C5FD.toInt())
                    setPadding(0, dp(8), 0, 0)
                    setOnClickListener {
                        resultView?.let { v -> try { wm.removeView(v) } catch (_: Exception) {} }
                        resultView = null
                    }
                }
                val scroll = ScrollView(this).apply { addView(body) }
                panel.addView(heading)
                panel.addView(scroll, LinearLayout.LayoutParams(-1, dp(230)))
                panel.addView(close)
                val params = WindowManager.LayoutParams(
                    (resources.displayMetrics.widthPixels - dp(32)).coerceAtLeast(dp(240)),
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                    PixelFormat.TRANSLUCENT
                ).apply { gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL; y = dp(24) }
                wm.addView(panel, params)
                resultView = panel
            } catch (_: Exception) {}
        }
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
}
