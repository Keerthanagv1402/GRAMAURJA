package com.gramaUrja

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class GramaUrjaApp : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)

            // Power alert channel
            val powerChannel = NotificationChannel(
                CHANNEL_POWER_ALERTS,
                "Power Status Alerts",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "Notifies when power is restored in your zone"
                enableVibration(true)
                enableLights(true)
            }

            // Pump timer channel
            val timerChannel = NotificationChannel(
                CHANNEL_PUMP_TIMER,
                "Pump Timer",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows ongoing pump timer countdown"
            }

            manager.createNotificationChannel(powerChannel)
            manager.createNotificationChannel(timerChannel)
        }
    }

    companion object {
        const val CHANNEL_POWER_ALERTS = "power_alerts"
        const val CHANNEL_PUMP_TIMER   = "pump_timer"
    }
}
