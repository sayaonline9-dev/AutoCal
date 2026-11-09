package com.chatkale.bot

import android.app.Application

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        val old = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            try {
                getSharedPreferences("bot", MODE_PRIVATE).edit()
                    .putString(Prefs.CRASH, android.util.Log.getStackTraceString(e).take(700)).commit()
            } catch (_: Throwable) {}
            old?.uncaughtException(t, e)
        }
    }
}
