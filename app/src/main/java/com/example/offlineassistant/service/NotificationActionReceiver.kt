package com.example.offlineassistant.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.RemoteInput
import com.example.offlineassistant.AssistantCommandParser
import com.example.offlineassistant.AssistantToolExecutor

class NotificationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_QUICK_REPLY) {
            val remoteInput = RemoteInput.getResultsFromIntent(intent)
            val replyText = remoteInput?.getCharSequence("reply_text")?.toString()

            if (!replyText.isNullOrEmpty()) {
                val parser = AssistantCommandParser()
                val parsedAction = parser.parse(replyText)
                
                if (parsedAction != null) {
                    val executor = AssistantToolExecutor(context)
                    executor.execute(parsedAction)
                    Toast.makeText(context, "Executed: $replyText", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Command not understood", Toast.LENGTH_SHORT).show()
                }
            }

            // Cancel and re-post notification to clear the reply field
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.cancel(AssistantNotificationService.NOTIFICATION_ID)
            
            AssistantNotificationService.start(context)
        }
    }

    companion object {
        const val ACTION_QUICK_REPLY = "com.example.offlineassistant.ACTION_QUICK_REPLY"
    }
}
