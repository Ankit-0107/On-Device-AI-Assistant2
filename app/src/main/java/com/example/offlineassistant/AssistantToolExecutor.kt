package com.example.offlineassistant

import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.provider.AlarmClock
import android.provider.MediaStore
import android.provider.Settings
import android.widget.Toast
import java.util.Locale

class AssistantToolExecutor(
    private val context: Context
) {

    private val noteStorage = NoteStorage(context)
    private var isFlashlightOn = false

    fun execute(action: AssistantAction): String? {

        return when (action) {

            is AssistantAction.OpenApp -> executeOpenApp(action)

            is AssistantAction.CreateNote -> executeCreateNote(action)

            is AssistantAction.GetNotes -> executeGetNotes()

            is AssistantAction.DeleteNote -> executeDeleteNote(action)

            is AssistantAction.SetAlarm -> executeSetAlarm(action)

            is AssistantAction.SetTimer -> executeSetTimer(action)

            is AssistantAction.CreateReminder -> executeCreateReminder(action)

            is AssistantAction.MakeCall -> executeMakeCall(action)

            is AssistantAction.SendMessage -> executeSendMessage(action)

            is AssistantAction.PlayMusic -> executePlayMusic()

            is AssistantAction.GetDeviceInfo -> executeGetDeviceInfo()

            is AssistantAction.SearchWeb -> executeSearchWeb(action)

            is AssistantAction.ToggleFlashlight -> executeToggleFlashlight(action)

            is AssistantAction.TakePhoto -> executeTakePhoto()

            is AssistantAction.OpenSettings -> executeOpenSettings(action)

            is AssistantAction.SetBrightness -> executeSetBrightness(action)

            is AssistantAction.ToggleWifi -> executeToggleWifi(action)

            is AssistantAction.ToggleBluetooth -> executeToggleBluetooth(action)
        }
    }

    // ── Open App ────────────────────────────────────────────────

    private fun executeOpenApp(action: AssistantAction.OpenApp): String {

        return if (openApp(action.packageName)) {
            "Opening app..."
        } else {
            "I couldn't find that app on your device."
        }
    }

    private fun openApp(packageName: String): Boolean {

        return try {

            val intent = context.packageManager
                .getLaunchIntentForPackage(packageName)

            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                true
            } else {
                showToast("App not found")
                false
            }

        } catch (e: Exception) {
            showToast("Unable to open app")
            false
        }
    }

    // ── Notes ───────────────────────────────────────────────────

    private fun executeCreateNote(action: AssistantAction.CreateNote): String {

        noteStorage.saveNote(action.text)
        showToast("Note saved ✓")
        return "📝 Note saved: \"${action.text}\""
    }

    private fun executeGetNotes(): String {

        val notes = noteStorage.getNotes()

        return if (notes.isEmpty()) {
            "You don't have any saved notes."
        } else {
            "📋 Your notes:\n\n" +
                notes.mapIndexed { index, note ->
                    "${index + 1}. $note"
                }.joinToString("\n")
        }
    }

    private fun executeDeleteNote(action: AssistantAction.DeleteNote): String {

        val notes = noteStorage.getNotes()

        return if (action.index < 0 || action.index >= notes.size) {
            "Invalid note number. You have ${notes.size} note(s)."
        } else {
            val deleted = notes[action.index]
            noteStorage.deleteNote(action.index)
            showToast("Note deleted ✓")
            "🗑️ Deleted note: \"$deleted\""
        }
    }

    // ── Set Alarm ───────────────────────────────────────────────

    private fun executeSetAlarm(action: AssistantAction.SetAlarm): String {

        return try {

            val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                putExtra(AlarmClock.EXTRA_HOUR, action.hour)
                putExtra(AlarmClock.EXTRA_MINUTES, action.minute)
                putExtra(AlarmClock.EXTRA_SKIP_UI, true)
                action.label?.let {
                    putExtra(AlarmClock.EXTRA_MESSAGE, it)
                }
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(intent)

            val timeStr = formatTime(action.hour, action.minute)
            "⏰ Alarm set for $timeStr"

        } catch (e: Exception) {
            "Failed to set alarm: ${e.message}"
        }
    }

    private fun formatTime(hour: Int, minute: Int): String {

        val period = if (hour < 12) "AM" else "PM"
        val displayHour = when {
            hour == 0 -> 12
            hour > 12 -> hour - 12
            else -> hour
        }
        return String.format(
            Locale.getDefault(),
            "%d:%02d %s",
            displayHour,
            minute,
            period
        )
    }

    // ── Set Timer ───────────────────────────────────────────────

    private fun executeSetTimer(action: AssistantAction.SetTimer): String {

        return try {

            val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
                putExtra(AlarmClock.EXTRA_LENGTH, action.seconds)
                putExtra(AlarmClock.EXTRA_SKIP_UI, true)
                action.label?.let {
                    putExtra(AlarmClock.EXTRA_MESSAGE, it)
                }
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(intent)

            val timeStr = formatDuration(action.seconds)
            "⏱️ Timer set for $timeStr"

        } catch (e: Exception) {
            "Failed to set timer: ${e.message}"
        }
    }

    private fun formatDuration(totalSeconds: Int): String {

        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        return buildString {
            if (hours > 0) append("${hours}h ")
            if (minutes > 0) append("${minutes}m ")
            if (seconds > 0) append("${seconds}s")
        }.trim()
    }

    // ── Create Reminder ─────────────────────────────────────────

    private fun executeCreateReminder(
        action: AssistantAction.CreateReminder
    ): String {

        return try {

            // Use calendar intent to create a reminder event
            val intent = Intent(Intent.ACTION_INSERT).apply {
                data = android.provider.CalendarContract.Events.CONTENT_URI
                putExtra(
                    android.provider.CalendarContract.Events.TITLE,
                    action.text
                )
                putExtra(
                    android.provider.CalendarContract.EXTRA_EVENT_BEGIN_TIME,
                    System.currentTimeMillis() + 3600000 // 1 hour from now
                )
                putExtra(
                    android.provider.CalendarContract.EXTRA_EVENT_END_TIME,
                    System.currentTimeMillis() + 7200000 // 2 hours from now
                )
                putExtra(
                    android.provider.CalendarContract.Events.HAS_ALARM,
                    true
                )
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(intent)
            "🔔 Creating reminder: \"${action.text}\""

        } catch (e: Exception) {
            "Failed to create reminder: ${e.message}"
        }
    }

    // ── Make Call ────────────────────────────────────────────────

    private fun executeMakeCall(action: AssistantAction.MakeCall): String {

        return try {

            // Use ACTION_DIAL (doesn't require permission) to show the
            // number in dialer. ACTION_CALL would auto-dial but needs
            // CALL_PHONE permission.
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:${action.phoneNumber}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(intent)
            "📞 Dialing ${action.phoneNumber}..."

        } catch (e: Exception) {
            "Failed to make call: ${e.message}"
        }
    }

    // ── Send Message ────────────────────────────────────────────

    private fun executeSendMessage(
        action: AssistantAction.SendMessage
    ): String {

        return try {

            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:${action.phoneNumber}")
                putExtra("sms_body", action.message)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(intent)
            "💬 Opening SMS to ${action.phoneNumber}..."

        } catch (e: Exception) {
            "Failed to open messaging: ${e.message}"
        }
    }

    // ── Play Music ──────────────────────────────────────────────

    private fun executePlayMusic(): String {

        return try {

            // Try to launch YouTube Music first
            val ytMusicIntent = context.packageManager
                .getLaunchIntentForPackage(
                    "com.google.android.apps.youtube.music"
                )

            if (ytMusicIntent != null) {
                ytMusicIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(ytMusicIntent)
                return "🎵 Opening YouTube Music..."
            }

            // Try Spotify
            val spotifyIntent = context.packageManager
                .getLaunchIntentForPackage("com.spotify.music")

            if (spotifyIntent != null) {
                spotifyIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(spotifyIntent)
                return "🎵 Opening Spotify..."
            }

            // Fallback: generic music player intent
            val intent = Intent(
                MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH
            ).apply {
                putExtra(MediaStore.EXTRA_MEDIA_FOCUS, "audio/*")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(intent)
            "🎵 Opening music player..."

        } catch (e: Exception) {
            "I couldn't find a music player on your device."
        }
    }

    // ── Device Info ─────────────────────────────────────────────

    private fun executeGetDeviceInfo(): String {

        val batteryManager = context.getSystemService(
            Context.BATTERY_SERVICE
        ) as? BatteryManager

        val batteryLevel = batteryManager?.getIntProperty(
            BatteryManager.BATTERY_PROPERTY_CAPACITY
        ) ?: -1

        val isCharging = batteryManager?.isCharging ?: false

        val activityManager = context.getSystemService(
            Context.ACTIVITY_SERVICE
        ) as? android.app.ActivityManager

        val memInfo = android.app.ActivityManager.MemoryInfo()
        activityManager?.getMemoryInfo(memInfo)

        val totalRamGB = String.format(
            Locale.getDefault(),
            "%.1f",
            memInfo.totalMem / (1024.0 * 1024.0 * 1024.0)
        )

        val availRamGB = String.format(
            Locale.getDefault(),
            "%.1f",
            memInfo.availMem / (1024.0 * 1024.0 * 1024.0)
        )

        return buildString {
            appendLine("📱 Device Information:\n")
            appendLine("• Model: ${Build.MODEL}")
            appendLine("• Manufacturer: ${Build.MANUFACTURER}")
            appendLine("• Brand: ${Build.BRAND}")
            appendLine("• Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            appendLine("• Security Patch: ${Build.VERSION.SECURITY_PATCH}")
            appendLine()
            appendLine("🔋 Battery: ${batteryLevel}%${if (isCharging) " (Charging)" else ""}")
            appendLine()
            appendLine("💾 RAM: ${availRamGB}GB available / ${totalRamGB}GB total")
            appendLine("• Low Memory: ${if (memInfo.lowMemory) "Yes ⚠️" else "No ✅"}")
        }.trim()
    }

    // ── Search Web ──────────────────────────────────────────────

    private fun executeSearchWeb(action: AssistantAction.SearchWeb): String {

        return try {

            val searchUri = Uri.parse(
                "https://www.google.com/search?q=${Uri.encode(action.query)}"
            )

            val intent = Intent(Intent.ACTION_VIEW, searchUri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(intent)
            "🔍 Searching for: \"${action.query}\""

        } catch (e: Exception) {
            "Failed to open web search: ${e.message}"
        }
    }

    // ── Flashlight ──────────────────────────────────────────────

    private fun executeToggleFlashlight(
        action: AssistantAction.ToggleFlashlight
    ): String {

        return try {

            val cameraManager = context.getSystemService(
                Context.CAMERA_SERVICE
            ) as CameraManager

            val cameraId = cameraManager.cameraIdList.firstOrNull { id ->
                val characteristics = cameraManager.getCameraCharacteristics(id)
                characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }

            if (cameraId != null) {
                cameraManager.setTorchMode(cameraId, action.turnOn)
                isFlashlightOn = action.turnOn

                if (action.turnOn) {
                    "🔦 Flashlight turned ON"
                } else {
                    "🔦 Flashlight turned OFF"
                }
            } else {
                "This device doesn't have a flashlight."
            }

        } catch (e: Exception) {
            "Failed to toggle flashlight: ${e.message}"
        }
    }

    // ── Take Photo ──────────────────────────────────────────────

    private fun executeTakePhoto(): String {

        return try {

            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(intent)
            "📸 Opening camera..."

        } catch (e: Exception) {
            "Failed to open camera: ${e.message}"
        }
    }

    // ── Open Settings ───────────────────────────────────────────

    private val settingsSectionMap = mapOf(
        "wifi" to Settings.ACTION_WIFI_SETTINGS,
        "wi-fi" to Settings.ACTION_WIFI_SETTINGS,
        "bluetooth" to Settings.ACTION_BLUETOOTH_SETTINGS,
        "display" to Settings.ACTION_DISPLAY_SETTINGS,
        "brightness" to Settings.ACTION_DISPLAY_SETTINGS,
        "sound" to Settings.ACTION_SOUND_SETTINGS,
        "volume" to Settings.ACTION_SOUND_SETTINGS,
        "battery" to Settings.ACTION_BATTERY_SAVER_SETTINGS,
        "storage" to Settings.ACTION_INTERNAL_STORAGE_SETTINGS,
        "apps" to Settings.ACTION_APPLICATION_SETTINGS,
        "applications" to Settings.ACTION_APPLICATION_SETTINGS,
        "location" to Settings.ACTION_LOCATION_SOURCE_SETTINGS,
        "security" to Settings.ACTION_SECURITY_SETTINGS,
        "date" to Settings.ACTION_DATE_SETTINGS,
        "time" to Settings.ACTION_DATE_SETTINGS,
        "language" to Settings.ACTION_LOCALE_SETTINGS,
        "accessibility" to Settings.ACTION_ACCESSIBILITY_SETTINGS,
        "notification" to Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS,
        "notifications" to Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS,
        "nfc" to Settings.ACTION_NFC_SETTINGS,
        "airplane" to Settings.ACTION_AIRPLANE_MODE_SETTINGS,
        "data" to Settings.ACTION_DATA_USAGE_SETTINGS,
        "network" to Settings.ACTION_WIRELESS_SETTINGS,
        "developer" to Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS
    )

    private fun executeOpenSettings(
        action: AssistantAction.OpenSettings
    ): String {

        return try {

            val settingsAction = if (action.section != null) {
                settingsSectionMap[action.section.lowercase()]
                    ?: Settings.ACTION_SETTINGS
            } else {
                Settings.ACTION_SETTINGS
            }

            val intent = Intent(settingsAction).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(intent)

            if (action.section != null) {
                "⚙️ Opening ${action.section} settings..."
            } else {
                "⚙️ Opening Settings..."
            }

        } catch (e: Exception) {
            "Failed to open settings: ${e.message}"
        }
    }

    // ── Set Brightness ──────────────────────────────────────────

    private fun executeSetBrightness(
        action: AssistantAction.SetBrightness
    ): String {

        return try {

            if (!Settings.System.canWrite(context)) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_WRITE_SETTINGS
                ).apply {
                    data = Uri.parse(
                        "package:${context.packageName}"
                    )
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                return "Please grant permission to modify system settings, then try again."
            }

            // Map 0-100 to 0-255
            val brightnessValue = (action.level * 255) / 100

            // Disable auto-brightness
            Settings.System.putInt(
                context.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS_MODE,
                Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
            )

            // Set brightness
            Settings.System.putInt(
                context.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS,
                brightnessValue
            )

            "🔆 Brightness set to ${action.level}%"

        } catch (e: Exception) {
            "Failed to set brightness: ${e.message}"
        }
    }

    // ── WiFi Toggle ─────────────────────────────────────────────

    private fun executeToggleWifi(
        action: AssistantAction.ToggleWifi
    ): String {

        // On Android 10+ (API 29+), apps cannot directly toggle WiFi.
        // We open WiFi settings instead.
        return try {

            val intent = Intent(Settings.ACTION_WIFI_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(intent)

            val state = if (action.enable) "enable" else "disable"
            "📶 Opening WiFi settings to $state WiFi..."

        } catch (e: Exception) {
            "Failed to open WiFi settings: ${e.message}"
        }
    }

    // ── Bluetooth Toggle ────────────────────────────────────────

    private fun executeToggleBluetooth(
        action: AssistantAction.ToggleBluetooth
    ): String {

        // Similar to WiFi, direct toggle requires system app privileges
        // on newer Android versions. Open Bluetooth settings instead.
        return try {

            val intent = Intent(
                Settings.ACTION_BLUETOOTH_SETTINGS
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(intent)

            val state = if (action.enable) "enable" else "disable"
            "📶 Opening Bluetooth settings to $state Bluetooth..."

        } catch (e: Exception) {
            "Failed to open Bluetooth settings: ${e.message}"
        }
    }

    // ── Utility ─────────────────────────────────────────────────

    private fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}