package com.example.offlineassistant.widget

import androidx.glance.appwidget.GlanceAppWidgetReceiver

class AssistantWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = AssistantWidget()
}
