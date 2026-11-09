package com.chatkale.bot

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Prefs {
    const val API = "api"
    const val MODEL = "model"
    const val GROUP = "group"
    const val PERSONA = "persona"
    const val TAG = "tag"
    const val MAXH = "maxh"
    const val ENABLED = "enabled"
    const val MORNING_ON = "morning_on"
    const val MORNING_H = "morning_h"
    const val MORNING_M = "morning_m"
    const val LOG = "log"
    const val CRASH = "crash"

    const val DEFAULT_PERSONA =
        "آپ 'چٹکلے' واٹس ایپ گروپ کے دوستانہ AI فلسفی ہیں۔ ہر پیغام کا جواب اسی زبان اور رسم الخط میں دیں جس میں پیغام ہو (اردو ہو تو اردو)۔ " +
        "جواب 2 سے 3 مختصر جملوں میں، گرم جوش، سوچنے پر مجبور کرنے والا اور ہر مذہب اور سوچ کے لیے قابلِ قبول ہو۔ " +
        "مذہبی، سیاسی یا متنازع معاملات میں کسی کی طرف داری نہ کریں، نرمی سے مختلف زاویے یا عمومی انسانی حکمت بیان کریں۔ " +
        "کسی کی دل آزاری، طنز، گالی یا بحث سے مکمل پرہیز کریں۔ طبی، قانونی یا مالی مشورہ نہ دیں۔ " +
        "مناسب ہو تو پیغام بھیجنے والے کا نام لیں۔ اگر پیغام صرف تصویر، ویڈیو یا اسٹیکر کا اشارہ ہو تو مختصر خوش دلی کا جواب دیں۔"

    private fun sp(c: Context) = c.getSharedPreferences("bot", Context.MODE_PRIVATE)
    fun get(c: Context, k: String, d: String): String = sp(c).getString(k, d) ?: d
    fun put(c: Context, k: String, v: String) = sp(c).edit().putString(k, v).apply()
    fun getBool(c: Context, k: String, d: Boolean) = sp(c).getBoolean(k, d)
    fun putBool(c: Context, k: String, v: Boolean) = sp(c).edit().putBoolean(k, v).apply()

    @Synchronized
    fun log(c: Context, line: String) {
        val t = SimpleDateFormat("dd/MM HH:mm", Locale.US).format(Date())
        val old = get(c, LOG, "").split("\n").filter { it.isNotBlank() }
        put(c, LOG, (listOf("$t  $line") + old).take(25).joinToString("\n"))
    }
}
