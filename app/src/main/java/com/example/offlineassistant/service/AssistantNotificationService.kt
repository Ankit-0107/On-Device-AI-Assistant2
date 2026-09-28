package com.example.offlineassistant.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.RemoteInput
import com.example.offlineassistant.MainActivity
import com.example.offlineassistant.R

class AssistantNotificationService : Service() {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val channelId = "assistant_channel"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Offline Assistant",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        // 1. Quick Reply
        val replyLabel = "Type a command..."
        val remoteInput = RemoteInput.Builder("reply_text")
            .setLabel(replyLabel)
            .build()
            
        val replyIntent = Intent(this, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_QUICK_REPLY
        }
        val replyPendingIntent = PendingIntent.getBroadcast(
            this, 0, replyIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
        
        val actionReply = NotificationCompat.Action.Builder(
            R.drawable.ic_launcher_foreground,
            "Quick Reply",
            replyPendingIntent
        )
        .addRemoteInput(remoteInput)
        .build()

        // 2. Voice
        val voiceIntent = Intent(this, MainActivity::class.java).apply {
            putExtra("EXTRA_START_VOICE", true)
        }
        val voicePendingIntent = PendingIntent.getActivity(
            this, 1, voiceIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val actionVoice = NotificationCompat.Action.Builder(
            R.drawable.ic_launcher_foreground,
            "Voice",
            voicePendingIntent
        ).build()

        // 3. Open App
        val openIntent = Intent(this, MainActivity::class.java)
        val openPendingIntent = PendingIntent.getActivity(
            this, 2, openIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val actionOpen = NotificationCompat.Action.Builder(
            R.drawable.ic_launcher_foreground,
            "Open App",
            openPendingIntent
        ).build()

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Offline Assistant")
            .setContentText("Tap to open or use quick actions")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .addAction(actionReply)
            .addAction(actionVoice)
            .addAction(actionOpen)
            .setContentIntent(openPendingIntent)
            .build()

        if (Build.VERSION.SDK_INT >= 34) {
            try {
                // Using reflection or raw call if supported. For simplicity:
                startForeground(NOTIFICATION_ID, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
            } catch (e: NoSuchMethodError) {
                startForeground(NOTIFICATION_ID, notification)
            }
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val NOTIFICATION_ID = 1

        fun start(context: Context) {
            val intent = Intent(context, AssistantNotificationService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, AssistantNotificationService::class.java)
            context.stopService(intent)
        }
    }
}
