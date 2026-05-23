// PATH: nw-parent-app/app/src/main/java/com/nw/parentalcontrol/service/ConnectionListenerService.kt
package com.nw.parentalcontrol.service

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.nw.parentalcontrol.ui.MainActivity

class ConnectionListenerService : Service() {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(1, buildNotification())
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildNotification(): Notification {
        val channelId = "nw_parent_service"
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel(channelId, "NW Parental Service", NotificationManager.IMPORTANCE_LOW)
        )
        val pi = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("NW Parental Active")
            .setContentText("Monitoring child device")
            .setContentIntent(pi)
            .setOngoing(true)
            .build()
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val prefs     = context.getSharedPreferences("parent_prefs", Context.MODE_PRIVATE)
        val connected = prefs.getString("connected_child_device_id", null)
        if (connected != null) {
            context.startForegroundService(Intent(context, ConnectionListenerService::class.java))
        }
    }
}