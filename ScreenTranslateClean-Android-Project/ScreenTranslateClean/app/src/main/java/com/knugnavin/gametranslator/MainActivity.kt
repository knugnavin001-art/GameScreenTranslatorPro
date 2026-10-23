package com.knugnavin.gametranslator

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class MainActivity : Activity() {
    private val ink = Color.rgb(27, 39, 56)
    private val muted = Color.rgb(101, 116, 139)
    private val blue = Color.rgb(39, 103, 210)
    private val page = Color.rgb(246, 248, 252)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = page
        window.navigationBarColor = page
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or
            View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(18), dp(20), dp(20))
            setBackgroundColor(page)
        }

        val scroll = ScrollView(this).apply {
            isFillViewport = true
            addView(content)
        }

        val brand = TextView(this).apply {
            text = "SCREEN TRANSLATE"
            textSize = 12f
            letterSpacing = 0.14f
            setTextColor(blue)
            typeface = Typeface.DEFAULT_BOLD
        }
        content.addView(brand)

        val title = TextView(this).apply {
            text = "แปลหน้าจอ\nให้เข้าใจง่าย"
            textSize = 30f
            setTextColor(ink)
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
            setPadding(0, dp(10), 0, dp(8))
        }
        content.addView(title)

        val subtitle = TextView(this).apply {
            text = "แตะปุ่มลอยขณะเล่นเกม เพื่ออ่านข้อความและแปลเป็นภาษาไทย"
            textSize = 15f
            setTextColor(muted)
            setLineSpacing(dp(3).toFloat(), 1f)
        }
        content.addView(subtitle)

        val setupCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(8))
            background = rounded(Color.WHITE, dp(18))
        }
        val cardTitle = TextView(this).apply {
            text = "ตั้งค่าเริ่มต้น"
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(ink)
            setPadding(0, 0, 0, dp(8))
        }
        setupCard.addView(cardTitle)
        setupCard.addView(step("01", "อนุญาตปุ่มลอย", "เพื่อให้ปุ่มแปลอยู่เหนือเกม") )
        setupCard.addView(step("02", "เปิดบริการแปลหน้าจอ", "ใช้จับภาพเมื่อคุณแตะปุ่มแปลเท่านั้น") )
        setupCard.addView(step("03", "กลับไปที่เกม", "แตะปุ่ม 🌐 เพื่อเริ่มอ่านข้อความ") )
        content.addView(setupCard, marginParams(top = 24, bottom = 16))

        val overlayButton = primaryButton("อนุญาตปุ่มลอย")
        overlayButton.setOnClickListener {
            if (!Settings.canDrawOverlays(this)) {
                startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
            } else {
                toast("อนุญาตปุ่มลอยแล้ว")
            }
        }
        content.addView(overlayButton, marginParams(bottom = 10))

        val accessibilityButton = secondaryButton("เปิดการตั้งค่าบริการแปล")
        accessibilityButton.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
        content.addView(accessibilityButton, marginParams(bottom = 10))

        val statusButton = TextView(this).apply {
            text = "ตรวจสอบสถานะการตั้งค่า"
            textSize = 14f
            gravity = Gravity.CENTER
            setTextColor(blue)
            setPadding(dp(12), dp(12), dp(12), dp(12))
            setOnClickListener {
                val overlay = if (Settings.canDrawOverlays(this@MainActivity)) "พร้อม" else "ยังไม่ได้อนุญาต"
                toast("ปุ่มลอย: $overlay\nตรวจสอบบริการแปลในเมนูการช่วยเหลือพิเศษ")
            }
        }
        content.addView(statusButton, marginParams(bottom = 14))

        val privacy = TextView(this).apply {
            text = "ไม่มีโฆษณา • ไม่มีระบบจ่ายเงิน • ไม่มีการสมัครสมาชิก\nต้องใช้อินเทอร์เน็ตเพื่อแปลข้อความ"
            textSize = 12f
            gravity = Gravity.CENTER
            setTextColor(muted)
            setPadding(dp(8), dp(10), dp(8), dp(8))
        }
        content.addView(privacy)

        setContentView(scroll)
    }

    private fun step(number: String, heading: String, detail: String): View {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.TOP
            setPadding(0, dp(10), 0, dp(10))
        }
        val badge = TextView(this).apply {
            text = number
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setTextColor(blue)
            background = rounded(Color.rgb(232, 240, 255), dp(10))
        }
        row.addView(badge, LinearLayout.LayoutParams(dp(42), dp(42)))
        val textBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), 0, 0, 0)
        }
        textBox.addView(TextView(this).apply {
            text = heading
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(ink)
        })
        textBox.addView(TextView(this).apply {
            text = detail
            textSize = 12f
            setTextColor(muted)
            setPadding(0, dp(3), 0, 0)
        })
        row.addView(textBox, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        return row
    }

    private fun primaryButton(label: String) = Button(this).apply {
        text = label
        textSize = 15f
        isAllCaps = false
        setTextColor(Color.WHITE)
        background = rounded(blue, dp(14))
        minHeight = dp(52)
        stateListAnimator = null
    }

    private fun secondaryButton(label: String) = Button(this).apply {
        text = label
        textSize = 15f
        isAllCaps = false
        setTextColor(blue)
        background = rounded(Color.WHITE, dp(14), Color.rgb(220, 228, 240))
        minHeight = dp(52)
        stateListAnimator = null
    }

    private fun rounded(color: Int, radius: Int, stroke: Int? = null) =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = radius.toFloat()
            if (stroke != null) setStroke(dp(1), stroke)
        }

    private fun marginParams(top: Int = 0, bottom: Int = 0) =
        LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            topMargin = dp(top)
            bottomMargin = dp(bottom)
        }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    private fun toast(message: String) =
        android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_LONG).show()
}
