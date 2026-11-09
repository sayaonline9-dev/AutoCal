package com.chatkale.bot

import android.app.Notification
import android.app.RemoteInput
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import java.util.concurrent.Executors

class WhatsAppListener : NotificationListenerService() {
    private val exec = Executors.newSingleThreadExecutor()
    private val seen = HashMap<String, Long>()
    private val history = HashMap<String, ArrayList<String>>()
    private val sent = ArrayList<Long>()

    override fun onListenerDisconnected() {
        requestRebind(ComponentName(this, WhatsAppListener::class.java))
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        try { handle(sbn) } catch (e: Throwable) { Prefs.log(this, "خرابی: ${e.message}") }
    }

    private fun handle(sbn: StatusBarNotification) {
        if (sbn.packageName != "com.whatsapp" && sbn.packageName != "com.whatsapp.w4b") return
        if (!Prefs.getBool(this, Prefs.ENABLED, false)) return
        val n = sbn.notification
        if (n.flags and Notification.FLAG_GROUP_SUMMARY != 0) return
        val ex = n.extras
        val title = (ex.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE)
            ?: ex.getCharSequence(Notification.EXTRA_TITLE))?.toString() ?: return
        val wanted = Prefs.get(this, Prefs.GROUP, "").trim()
        if (wanted.isEmpty() || !title.contains(wanted)) {
            Prefs.log(this, "📩 «${title.take(25)}» گروپ نام سے میل نہیں کھایا")
            return
        }
        val action = n.actions?.firstOrNull { it.remoteInputs?.isNotEmpty() == true }
        if (action == null) { Prefs.log(this, "⚠️ جواب کا بٹن نہیں ملا (نوٹیفکیشن میں Reply نہیں)"); return }

        val now = System.currentTimeMillis()
        // (sender, text, time)
        val items = ArrayList<Triple<String, String, Long>>()
        @Suppress("DEPRECATION")
        val arr = ex.getParcelableArray(Notification.EXTRA_MESSAGES)
        if (arr != null && arr.isNotEmpty()) {
            for (p in arr) {
                val b = p as? Bundle ?: continue
                var t = b.getCharSequence("text")?.toString() ?: continue
                var s = b.getCharSequence("sender")?.toString() ?: ""
                if (s.isEmpty()) s = b.getBundle("sender_person")?.getCharSequence("name")?.toString() ?: ""
                if (s.isEmpty()) {
                    val i = t.indexOf(": ")
                    if (i in 1..40) { s = t.substring(0, i); t = t.substring(i + 2) }
                }
                items.add(Triple(s, t, b.getLong("time")))
            }
        } else {
            val txt = ex.getCharSequence(Notification.EXTRA_TEXT)?.toString()
            if (txt != null) {
                val i = txt.indexOf(": ")
                if (i in 1..40) items.add(Triple(txt.substring(0, i), txt.substring(i + 2), 0L))
                else items.add(Triple("", txt, 0L))
            }
        }
        if (items.isEmpty()) { Prefs.log(this, "⚠️ پیغام پڑھ نہ سکا"); return }

        val tag = Prefs.get(this, Prefs.TAG, "🤖")
        for (m in items) {
            var sender = m.first.trim()
            val text = m.second
            val t = m.third
            if (sender == "You" || sender == "آپ" || sender == "Me") continue
            if (tag.isNotEmpty() && text.startsWith(tag)) continue
            if (t > 0 && now - t > 300_000) continue
            if (sender.isEmpty()) sender = "ایک ممبر"
            val key = if (t > 0) "$title|$t|$sender|$text" else "$title|$sender|$text"
            val dup = synchronized(seen) {
                seen.entries.removeAll { now - it.value > 3_600_000 }
                val prev = seen[key]
                val isDup = prev != null && (t > 0 || now - prev < 600_000)
                if (!isDup) seen[key] = now
                isDup
            }
            if (dup) continue
            exec.execute { reply(action, title, sender, text) }
        }
    }

    private fun reply(action: Notification.Action, group: String, sender: String, text: String) {
        try {
            val max = Prefs.get(this, Prefs.MAXH, "20").toIntOrNull() ?: 20
            val now = System.currentTimeMillis()
            sent.removeAll { now - it > 3_600_000 }
            if (sent.size >= max) { Prefs.log(this, "گھنٹے کی حد پوری، جواب نہیں دیا"); return }
            val hist = history.getOrPut(group) { ArrayList() }
            val ctx = hist.takeLast(8).joinToString("\n")
            val prompt = "گروپ کی حالیہ گفتگو:\n$ctx\n\nنیا پیغام — $sender: $text\n\nاس پر گروپ کی طرف سے جواب لکھیں۔"
            hist.add("$sender: $text")
            val ans = Gemini.ask(this, Prefs.get(this, Prefs.PERSONA, Prefs.DEFAULT_PERSONA), prompt)
            if (ans.isEmpty()) { Prefs.log(this, "AI نے خالی جواب دیا"); return }
            send(action, Prefs.get(this, Prefs.TAG, "🤖") + " " + ans)
            hist.add("AI: $ans")
            if (hist.size > 30) hist.subList(0, 10).clear()
            sent.add(now)
            Prefs.log(this, "✅ $sender: ${text.take(35)} ← ${ans.take(60)}")
        } catch (e: Throwable) {
            Prefs.log(this, "خرابی: ${e.message}")
        }
    }

    private fun send(a: Notification.Action, text: String) {
        val ris = a.remoteInputs
        val intent = Intent()
        val b = Bundle()
        for (ri in ris) b.putCharSequence(ri.resultKey, text)
        RemoteInput.addResultsToIntent(ris, intent, b)
        a.actionIntent.send(this, 0, intent)
    }
}
