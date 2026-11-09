package com.chatkale.bot

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object Gemini {
    fun ask(c: Context, system: String, user: String): String {
        val key = Prefs.get(c, Prefs.API, "").trim()
        if (key.isEmpty()) throw Exception("Gemini API Key درج نہیں ہے")
        val model = Prefs.get(c, Prefs.MODEL, "gemini-2.5-flash").trim()
        val conn = URL("https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent")
            .openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.connectTimeout = 15000
        conn.readTimeout = 40000
        conn.doOutput = true
        conn.setRequestProperty("Content-Type", "application/json")
        conn.setRequestProperty("x-goog-api-key", key)

        fun parts(t: String) = JSONArray().put(JSONObject().put("text", t))
        val body = JSONObject()
            .put("systemInstruction", JSONObject().put("parts", parts(system)))
            .put("contents", JSONArray().put(JSONObject().put("role", "user").put("parts", parts(user))))
            .put(
                "generationConfig", JSONObject()
                    .put("maxOutputTokens", 500)
                    .put("temperature", 0.8)
                    .put("thinkingConfig", JSONObject().put("thinkingBudget", 0))
            )
        conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }

        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val resp = stream.bufferedReader().use { it.readText() }
        if (code !in 200..299) throw Exception("HTTP $code: ${resp.take(200)}")
        val ps = JSONObject(resp).getJSONArray("candidates").getJSONObject(0)
            .getJSONObject("content").getJSONArray("parts")
        val sb = StringBuilder()
        for (i in 0 until ps.length()) sb.append(ps.getJSONObject(i).optString("text"))
        return sb.toString().trim()
    }
}
