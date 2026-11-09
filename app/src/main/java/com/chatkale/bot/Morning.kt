package com.chatkale.bot

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import java.util.Calendar

object Morning {
    private const val CH = "morning"

    fun post(c: Context) {
        val body = Gemini.ask(
            c, Prefs.get(c, Prefs.PERSONA, Prefs.DEFAULT_PERSONA),
            "گروپ کے لیے آج صبح کی ایک خوبصورت اور مختصر گڈ مارننگ پوسٹ لکھیں: خوش دلی کا سلام اور ایک گہرا مگر آسان فلسفیانہ خیال۔ ہر دن نیا انداز ہو۔"
        )
        val text = Prefs.get(c, Prefs.TAG, "🤖") + " " + body
        val nm = c.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CH, "Morning post", NotificationManager.IMPORTANCE_HIGH))
        val share = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
            setPackage("com.whatsapp")
        }
        val pi = PendingIntent.getActivity(c, 9, share, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val n = Notification.Builder(c, CH)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("☀️ گڈ مارننگ پوسٹ تیار ہے، ٹیپ کریں")
            .setContentText(text)
            .setStyle(Notification.BigTextStyle().bigText(text))
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build()
        nm.notify(9, n)
        Prefs.log(c, "صبح کا مسودہ تیار")
    }

    fun schedule(c: Context) {
        val am = c.getSystemService(AlarmManager::class.java)
        val pi = PendingIntent.getBroadcast(
            c, 7, Intent(c, MorningReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        am.cancel(pi)
        if (!Prefs.getBool(c, Prefs.MORNING_ON, false)) return
        val h = Prefs.get(c, Prefs.MORNING_H, "7").toIntOrNull() ?: 7
        val m = Prefs.get(c, Prefs.MORNING_M, "0").toIntOrNull() ?: 0
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, h); set(Calendar.MINUTE, m); set(Calendar.SECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
        }
        val show = PendingIntent.getActivity(c, 8, Intent(c, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        am.setAlarmClock(AlarmManager.AlarmClockInfo(cal.timeInMillis, show), pi)
    }
}

class MorningReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
        Morning.schedule(c)
        val pr = goAsync()
        Thread {
            try { Morning.post(c) } catch (e: Exception) { Prefs.log(c, "صبح کی خرابی: ${e.message}") }
            finally { pr.finish() }
        }.start()
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) { Morning.schedule(c) }
}
