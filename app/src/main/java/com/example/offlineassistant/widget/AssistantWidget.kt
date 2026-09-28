package com.example.offlineassistant.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.Button
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.example.offlineassistant.MainActivity

class AssistantWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            AssistantWidgetContent()
        }
    }
}

@Composable
fun AssistantWidgetContent() {
    val context = LocalContext.current
    
    val noteIntent = Intent(context, MainActivity::class.java).apply {
        putExtra("shortcut_action", "take_note")
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    }
    val alarmIntent = Intent(context, MainActivity::class.java).apply {
        putExtra("shortcut_action", "set_alarm")
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    }
    val voiceIntent = Intent(context, MainActivity::class.java).apply {
        putExtra("EXTRA_START_VOICE", true)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    }
    val callIntent = Intent(context, MainActivity::class.java).apply {
        putExtra("shortcut_action", "make_call")
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    }

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .padding(12.dp)
            .background(GlanceTheme.colors.surface)
            .cornerRadius(16.dp)
    ) {
        Text(
            text = "🤖 Assistant",
            style = TextStyle(
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        )
        
        Row(
            modifier = GlanceModifier.fillMaxWidth().padding(top = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button("📝 Note", onClick = actionStartActivity(noteIntent), modifier = GlanceModifier.defaultWeight())
            Button("⏰ Alarm", onClick = actionStartActivity(alarmIntent), modifier = GlanceModifier.defaultWeight())
            Button("🎤 Voice", onClick = actionStartActivity(voiceIntent), modifier = GlanceModifier.defaultWeight())
            Button("📞 Call", onClick = actionStartActivity(callIntent), modifier = GlanceModifier.defaultWeight())
        }
    }
}
