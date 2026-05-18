package com.gramaUrja.ui.timer

import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.gramaUrja.GramaUrjaApp
import com.gramaUrja.R
import com.gramaUrja.ui.MainActivity

/**
 * Foreground service that keeps the pump timer alive when the screen is locked.
 * Bind from PumpTimerFragment to get countdown updates via callback.
 */
class PumpTimerService : Service() {

    inner class LocalBinder : Binder() { fun getService() = this@PumpTimerService }
    private val binder = LocalBinder()

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val pi = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(this, GramaUrjaApp.CHANNEL_PUMP_TIMER)
            .setSmallIcon(R.drawable.ic_pump)
            .setContentTitle("Pump Timer Running")
            .setContentText("Tap to view countdown")
            .setOngoing(true)
            .setContentIntent(pi)
            .build()
        startForeground(1001, notification)
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        stopForeground(STOP_FOREGROUND_REMOVE)
    }
}
