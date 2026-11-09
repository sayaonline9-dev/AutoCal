package com.chatkale.bot

import android.Manifest
import android.app.Activity
import android.app.TimePickerDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.*

class MainActivity : Activity() {
    private val BG = Color.parseColor("#F3F5F9")
    private val INK = Color.parseColor("#1B2333")
    private val MUTED = Color.parseColor("#6B7385")
    private val ACCENT = Color.parseColor("#4F46E5")
    private val GOOD = Color.parseColor("#16A34A")
    private val BAD = Color.parseColor("#DC2626")

    private val fields = LinkedHashMap<String, EditText>()
    private lateinit var swOn: Switch
    private lateinit var swMorning: Switch
    private lateinit var timeBtn: Button
    private lateinit var statusBox: LinearLayout
    private lateinit var logView: TextView
    private lateinit var resultView: TextView
    private var mh = 7
    private var mm = 0
    private var ready = false

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun bg(color: Int, r: Int, stroke: Int? = null) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(r).toFloat()
        if (stroke != null) setStroke(dp(1), stroke)
    }

    private fun card(parent: LinearLayout, title: String?): LinearLayout {
        val c = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = bg(Color.WHITE, 20)
            setPadding(dp(18), dp(16), dp(18), dp(18))
            elevation = dp(2).toFloat()
        }
        parent.addView(c, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(14) })
        if (title != null) c.addView(TextView(this).apply {
            text = title; textSize = 16f; setTypeface(null, Typeface.BOLD); setTextColor(INK)
        })
        return c
    }

    private fun lab(parent: LinearLayout, t: String) {
        parent.addView(TextView(this).apply {
            text = t; textSize = 12f; setTextColor(MUTED); setPadding(0, dp(12), 0, dp(4))
        })
    }

    private fun input(parent: LinearLayout, key: String, def: String, multi: Boolean = false, num: Boolean = false) {
        val e = EditText(this).apply {
            setText(Prefs.get(this@MainActivity, key, def))
            textSize = 15f
            setTextColor(INK)
            background = bg(Color.parseColor("#F7F8FB"), 12, Color.parseColor("#E1E5EE"))
            setPadding(dp(12), dp(10), dp(12), dp(10))
            gravity = Gravity.START or Gravity.TOP
            inputType = when {
                multi -> InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
                num -> InputType.TYPE_CLASS_NUMBER
                else -> InputType.TYPE_CLASS_TEXT
            }
            if (multi) { minLines = 5; maxLines = 12 }
        }
        fields[key] = e
        parent.addView(e, LinearLayout.LayoutParams(-1, -2))
    }

    private fun btn(parent: LinearLayout, label: String, fill: Int, txt: Int = Color.WHITE, f: () -> Unit) {
        val b = Button(this).apply {
            setText(label)
            isAllCaps = false
            textSize = 15f
            setTextColor(txt)
            background = bg(fill, 14)
            stateListAnimator = null
            setOnClickListener { f() }
        }
        parent.addView(b, LinearLayout.LayoutParams(-1, dp(50)).apply { topMargin = dp(10) })
    }

    private fun sw(parent: LinearLayout, label: String, on: Boolean): Switch {
        val s = Switch(this).apply {
            setText(label); textSize = 15f; setTextColor(INK); isChecked = on
            setPadding(0, dp(8), 0, dp(8))
        }
        parent.addView(s, LinearLayout.LayoutParams(-1, -2))
        return s
    }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        if (Build.VERSION.SDK_INT >= 33) requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        window.statusBarColor = BG
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR

        mh = Prefs.get(this, Prefs.MORNING_H, "7").toIntOrNull() ?: 7
        mm = Prefs.get(this, Prefs.MORNING_M, "0").toIntOrNull() ?: 0

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(12), dp(16), dp(40))
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }

        // ہیڈر
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(Color.parseColor("#4F46E5"), Color.parseColor("#7C3AED"))
            ).apply { cornerRadius = dp(24).toFloat() }
            setPadding(dp(22), dp(26), dp(22), dp(26))
        }
        header.addView(TextView(this).apply {
            text = "🤖 چٹکلے AI بوٹ"; textSize = 24f; setTextColor(Color.WHITE); setTypeface(null, Typeface.BOLD)
        })
        header.addView(TextView(this).apply {
            text = "آپ کے گروپ کا خودکار فلسفی ساتھی"; textSize = 13f
            setTextColor(Color.parseColor("#E0E7FF")); setPadding(0, dp(6), 0, 0)
        })
        root.addView(header)

        // کریش رپورٹ (اگر پچھلی بار ایپ بند ہوئی ہو)
        val crash = Prefs.get(this, Prefs.CRASH, "")
        if (crash.isNotEmpty()) {
            val c = card(root, "⚠️ پچھلی بار کی خرابی")
            c.addView(TextView(this).apply {
                text = crash; textSize = 10f; setTextColor(BAD); setTextIsSelectable(true)
                layoutDirection = View.LAYOUT_DIRECTION_LTR
            })
            btn(c, "خرابی حذف کریں", Color.parseColor("#FEE2E2"), BAD) {
                Prefs.put(this, Prefs.CRASH, ""); recreate()
            }
        }

        // حالت
        val st = card(root, "ایپ کی حالت")
        statusBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        st.addView(statusBox)

        // بوٹ
        val bot = card(root, "بوٹ")
        swOn = sw(bot, "گروپ میں خودکار جواب فعال", Prefs.getBool(this, Prefs.ENABLED, false))
        swOn.setOnCheckedChangeListener { _, _ -> if (ready) { save(); refreshStatus() } }

        // ترتیبات
        val cfg = card(root, "ترتیبات")
        lab(cfg, "Gemini API Key"); input(cfg, Prefs.API, "")
        lab(cfg, "ماڈل"); input(cfg, Prefs.MODEL, "gemini-2.5-flash")
        lab(cfg, "گروپ کا نام (بالکل واٹس ایپ جیسا)"); input(cfg, Prefs.GROUP, "چٹکلے")
        lab(cfg, "ہر جواب کے شروع کا نشان"); input(cfg, Prefs.TAG, "🤖")
        lab(cfg, "فی گھنٹہ زیادہ سے زیادہ جوابات"); input(cfg, Prefs.MAXH, "20", num = true)

        // شخصیت
        val per = card(root, "بوٹ کی شخصیت")
        lab(per, "ہدایات"); input(per, Prefs.PERSONA, Prefs.DEFAULT_PERSONA, multi = true)
        btn(per, "ڈیفالٹ شخصیت بحال کریں", Color.parseColor("#EEF0FF"), ACCENT) {
            fields[Prefs.PERSONA]?.setText(Prefs.DEFAULT_PERSONA)
        }

        // صبح
        val mor = card(root, "☀️ روزانہ صبح کی پوسٹ")
        swMorning = sw(mor, "ہر روز مسودہ تیار کریں", Prefs.getBool(this, Prefs.MORNING_ON, false))
        timeBtn = Button(this).apply {
            isAllCaps = false; textSize = 15f; setTextColor(ACCENT)
            background = bg(Color.parseColor("#EEF0FF"), 14); stateListAnimator = null
            setOnClickListener {
                TimePickerDialog(this@MainActivity, { _, h, m -> mh = h; mm = m; showTime() }, mh, mm, false).show()
            }
        }
        mor.addView(timeBtn, LinearLayout.LayoutParams(-1, dp(48)).apply { topMargin = dp(6) })
        showTime()
        btn(mor, "صبح کا مسودہ ابھی بنا کر دیکھیں", Color.parseColor("#EEF0FF"), ACCENT) {
            save()
            Thread {
                try { Morning.post(this) } catch (e: Throwable) { Prefs.log(this, "خرابی: ${e.message}") }
                runOnUiThread { refreshLog() }
            }.start()
        }

        // عمل
        val act = card(root, null)
        btn(act, "💾 محفوظ کریں", ACCENT) {
            if (save()) { refreshStatus(); Toast.makeText(this, "محفوظ ہو گیا ✓", Toast.LENGTH_SHORT).show() }
        }
        btn(act, "✨ AI ٹیسٹ کریں", Color.parseColor("#EEF0FF"), ACCENT) {
            save()
            resultView.text = "انتظار کریں…"
            Thread {
                val r = try {
                    Gemini.ask(this, Prefs.get(this, Prefs.PERSONA, Prefs.DEFAULT_PERSONA), "ایک مختصر سلام اور ایک فلسفیانہ جملہ")
                } catch (e: Throwable) { "❌ ${e.message}" }
                runOnUiThread { resultView.text = r }
            }.start()
        }
        resultView = TextView(this).apply { textSize = 14f; setTextColor(INK); setPadding(0, dp(10), 0, 0) }
        act.addView(resultView)

        // سرگرمی
        val lg = card(root, "آخری سرگرمی")
        logView = TextView(this).apply { textSize = 12f; setTextColor(MUTED); setPadding(0, dp(8), 0, 0) }
        lg.addView(logView)
        btn(lg, "ریفریش", Color.parseColor("#EEF0FF"), ACCENT) { refreshLog() }

        setContentView(ScrollView(this).apply { setBackgroundColor(BG); addView(root) })
        ready = true
    }

    private fun showTime() {
        timeBtn.text = "⏰ وقت: %02d:%02d".format(mh, mm)
    }

    private fun statusRow(t: String, ok: Boolean, tap: (() -> Unit)?) {
        val r = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(9), 0, dp(9))
        }
        val dot = View(this).apply {
            background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(if (ok) GOOD else BAD) }
        }
        r.addView(dot, LinearLayout.LayoutParams(dp(12), dp(12)).apply { marginEnd = dp(10) })
        r.addView(TextView(this).apply { text = t; textSize = 14f; setTextColor(INK) }, LinearLayout.LayoutParams(0, -2, 1f))
        r.addView(TextView(this).apply {
            text = if (ok) "✓" else if (tap != null) "ٹھیک کریں ‹" else "✗"
            textSize = 13f; setTextColor(if (ok) GOOD else BAD)
        })
        if (tap != null && !ok) r.setOnClickListener { tap() }
        statusBox.addView(r)
    }

    private fun refreshStatus() {
        statusBox.removeAllViews()
        val ln = Settings.Secure.getString(contentResolver, "enabled_notification_listeners") ?: ""
        val pm = getSystemService(PowerManager::class.java)
        statusRow("نوٹیفکیشن تک رسائی", ln.contains(packageName)) {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }
        statusRow("بیٹری پابندی سے آزاد", pm.isIgnoringBatteryOptimizations(packageName)) {
            startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
        }
        statusRow("Gemini API Key موجود", (fields[Prefs.API]?.text?.toString() ?: "").isNotBlank(), null)
        statusRow("بوٹ فعال", swOn.isChecked, null)
    }

    private fun refreshLog() { logView.text = Prefs.get(this, Prefs.LOG, "ابھی کچھ نہیں") }

    private fun save(): Boolean {
        try {
            for ((k, e) in fields) Prefs.put(this, k, e.text.toString().trim())
            Prefs.put(this, Prefs.MORNING_H, mh.toString())
            Prefs.put(this, Prefs.MORNING_M, mm.toString())
            Prefs.putBool(this, Prefs.ENABLED, swOn.isChecked)
            Prefs.putBool(this, Prefs.MORNING_ON, swMorning.isChecked)
        } catch (e: Throwable) {
            Toast.makeText(this, "محفوظ نہ ہو سکا: ${e.message}", Toast.LENGTH_LONG).show()
            return false
        }
        try { Morning.schedule(this) } catch (e: Throwable) {
            Prefs.log(this, "الارم کی خرابی: ${e.message}")
        }
        return true
    }

    override fun onResume() { super.onResume(); refreshStatus(); refreshLog() }
    override fun onPause() { super.onPause(); if (ready) save() }
}
