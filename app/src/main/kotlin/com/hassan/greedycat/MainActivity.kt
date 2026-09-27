package com.hassan.greedycat

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {
    private val requestCode = 501
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(28), dp(30), dp(28), dp(24))
            setBackgroundColor(Color.rgb(247, 248, 252))
        }

        val logo = TextView(this).apply {
            text = "🐱"
            textSize = 42f
            gravity = Gravity.CENTER
        }

        val title = TextView(this).apply {
            text = "GreedyCat"
            textSize = 30f
            gravity = Gravity.CENTER
            setTextColor(Color.rgb(17, 24, 39))
        }

        val owner = TextView(this).apply {
            text = "حسن منصور  •  نسخة جديدة من الصفر"
            textSize = 14f
            gravity = Gravity.CENTER
            setTextColor(Color.rgb(79, 70, 229))
        }

        val desc = TextView(this).apply {
            text = "مراقبة إحصائية للشاشة مع لوحة عائمة فوق Zaina Live.\nتقرأ النتائج الظاهرة وتعرض ملخصاً حياً؛ لا توجد ضمانات."
            textSize = 16f
            gravity = Gravity.CENTER
            setTextColor(Color.rgb(55, 65, 81))
            setPadding(0, dp(28), 0, dp(20))
        }

        status = TextView(this).apply {
            text = "● جاهز"
            textSize = 16f
            gravity = Gravity.CENTER
            setTextColor(Color.rgb(22, 163, 74))
            setPadding(0, dp(10), 0, dp(18))
        }

        val start = Button(this).apply {
            text = "بدء المراقبة"
            isAllCaps = false
            setOnClickListener { begin() }
        }

        val stop = Button(this).apply {
            text = "إيقاف المراقبة"
            isAllCaps = false
            setOnClickListener {
                stopService(Intent(this@MainActivity, CaptureService::class.java))
                status.text = "● تم الإيقاف"
            }
        }

        root.addView(logo)
        root.addView(title)
        root.addView(owner)
        root.addView(desc)
        root.addView(status)
        root.addView(start, LinearLayout.LayoutParams(-1, dp(52)).apply { bottomMargin = dp(12) })
        root.addView(stop, LinearLayout.LayoutParams(-1, dp(52)))

        setContentView(root)
    }

    private fun begin() {
        if (!Settings.canDrawOverlays(this)) {
            status.text = "● امنح إذن الظهور فوق التطبيقات"
            startActivity(
                Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
            )
            return
        }
        val manager = getSystemService(MediaProjectionManager::class.java)
        startActivityForResult(manager.createScreenCaptureIntent(), requestCode)
    }

    @Deprecated("Legacy activity result API retained for broad Android compatibility")
    override fun onActivityResult(request: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(request, resultCode, data)
        if (request != requestCode) return

        if (resultCode != RESULT_OK || data == null) {
            status.text = "● لم يبدأ تسجيل الشاشة"
            return
        }

        startForegroundService(
            Intent(this, CaptureService::class.java).apply {
                putExtra(CaptureService.EXTRA_RESULT_CODE, resultCode)
                putExtra(CaptureService.EXTRA_DATA, data)
            }
        )
        status.text = "● المراقبة تعمل — افتح Zaina Live"
        moveTaskToBack(true)
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density + 0.5f).toInt()
}
