package com.example.offlineassistant

class AssistantCommandParser {

    fun parse(message: String): AssistantAction? {

        val command = message.trim().lowercase()

        return parseOpenApp(command)
            ?: parseSetAlarm(command, message)
            ?: parseSetTimer(command, message)
            ?: parseCreateNote(command, message)
            ?: parseDeleteNote(command)
            ?: parseGetNotes(command)
            ?: parseCreateReminder(command, message)
            ?: parseMakeCall(command)
            ?: parseSendMessage(command, message)
            ?: parsePlayMusic(command)
            ?: parseGetDeviceInfo(command)
            ?: parseSearchWeb(command, message)
            ?: parseFlashlight(command)
            ?: parseTakePhoto(command)
            ?: parseOpenSettings(command)
            ?: parseBrightness(command)
            ?: parseWifi(command)
            ?: parseBluetooth(command)
    }

    // ── Open App ────────────────────────────────────────────────

    private val appPackageMap = mapOf(
        "youtube" to "com.google.android.youtube",
        "whatsapp" to "com.whatsapp",
        "instagram" to "com.instagram.android",
        "chrome" to "com.android.chrome",
        "camera" to "com.android.camera2",
        "maps" to "com.google.android.apps.maps",
        "google maps" to "com.google.android.apps.maps",
        "calculator" to "com.google.android.calculator",
        "calendar" to "com.google.android.calendar",
        "clock" to "com.google.android.deskclock",
        "contacts" to "com.google.android.contacts",
        "gallery" to "com.google.android.apps.photos",
        "photos" to "com.google.android.apps.photos",
        "phone" to "com.google.android.dialer",
        "dialer" to "com.google.android.dialer",
        "messages" to "com.google.android.apps.messaging",
        "gmail" to "com.google.android.gm",
        "spotify" to "com.spotify.music",
        "twitter" to "com.twitter.android",
        "x" to "com.twitter.android",
        "telegram" to "org.telegram.messenger",
        "facebook" to "com.facebook.katana",
        "snapchat" to "com.snapchat.android",
        "music" to "com.google.android.apps.youtube.music",
        "youtube music" to "com.google.android.apps.youtube.music",
        "settings" to "com.android.settings",
        "files" to "com.google.android.documentsui",
        "file manager" to "com.google.android.documentsui",
        "notes" to "com.google.android.keep",
        "keep" to "com.google.android.keep",
        "drive" to "com.google.android.apps.docs",
        "play store" to "com.android.vending",
        "store" to "com.android.vending"
    )

    private val openAppRegex =
        Regex("^(?:open|launch|start|run)\\s+(.+?)\\s*$")

    private fun parseOpenApp(command: String): AssistantAction? {

        val match = openAppRegex.find(command) ?: return null
        val appName = match.groupValues[1].trim()

        // Try exact match first, then partial match
        val packageName = appPackageMap[appName]
            ?: appPackageMap.entries.firstOrNull { (key, _) ->
                appName.contains(key) || key.contains(appName)
            }?.value
            ?: return null

        return AssistantAction.OpenApp(packageName)
    }

    // ── Set Alarm ───────────────────────────────────────────────

    // Matches: "set alarm for 7:30", "set alarm at 7:30 am",
    // "wake me up at 6 am", "alarm 7 30", "set alarm 19:30"
    private val alarmPatterns = listOf(
        Regex(
            """(?:set\s+(?:an?\s+)?alarm|alarm|wake\s+me\s+up)""" +
                """(?:\s+(?:for|at|to))?\s+(\d{1,2})[:\s](\d{2})""" +
                """(?:\s*(am|pm))?"""
        ),
        Regex(
            """(?:set\s+(?:an?\s+)?alarm|alarm|wake\s+me\s+up)""" +
                """(?:\s+(?:for|at|to))?\s+(\d{1,2})\s*(am|pm)"""
        )
    )

    private fun parseSetAlarm(
        command: String,
        original: String
    ): AssistantAction? {

        // Pattern 1: "set alarm for 7:30 am"
        alarmPatterns[0].find(command)?.let { match ->

            var hour = match.groupValues[1].toIntOrNull() ?: return null
            val minute = match.groupValues[2].toIntOrNull() ?: return null
            val period = match.groupValues[3].lowercase()

            hour = convertTo24Hour(hour, period)

            if (hour !in 0..23 || minute !in 0..59) return null

            return AssistantAction.SetAlarm(hour, minute)
        }

        // Pattern 2: "set alarm for 7 am" (no minutes)
        alarmPatterns[1].find(command)?.let { match ->

            var hour = match.groupValues[1].toIntOrNull() ?: return null
            val period = match.groupValues[2].lowercase()

            hour = convertTo24Hour(hour, period)

            if (hour !in 0..23) return null

            return AssistantAction.SetAlarm(hour, 0)
        }

        return null
    }

    private fun convertTo24Hour(hour: Int, period: String): Int {
        return when {
            period == "am" && hour == 12 -> 0
            period == "am" -> hour
            period == "pm" && hour == 12 -> 12
            period == "pm" -> hour + 12
            else -> hour // No am/pm specified, treat as 24-hour
        }
    }

    // ── Set Timer ───────────────────────────────────────────────

    // Matches: "set timer for 5 minutes", "timer 30 seconds",
    // "set a timer for 1 hour 30 minutes", "countdown 10 minutes"
    private val timerRegex = Regex(
        """(?:set\s+(?:a\s+)?timer|timer|countdown)""" +
            """(?:\s+(?:for|of))?\s+""" +
            """(?:(\d+)\s*(?:hours?|hrs?|h)\s*)?""" +
            """(?:(\d+)\s*(?:minutes?|mins?|m)\s*)?""" +
            """(?:(\d+)\s*(?:seconds?|secs?|s))?"""
    )

    private fun parseSetTimer(
        command: String,
        original: String
    ): AssistantAction? {

        val match = timerRegex.find(command) ?: return null

        val hours = match.groupValues[1].toIntOrNull() ?: 0
        val minutes = match.groupValues[2].toIntOrNull() ?: 0
        val seconds = match.groupValues[3].toIntOrNull() ?: 0

        val totalSeconds = hours * 3600 + minutes * 60 + seconds

        if (totalSeconds <= 0) return null

        return AssistantAction.SetTimer(totalSeconds)
    }

    // ── Create Note ─────────────────────────────────────────────

    private val noteRegex = Regex(
        """^(?:make\s+a\s+note|take\s+a\s+note|save\s+(?:a\s+)?note|""" +
            """note\s+down|write\s+(?:a\s+)?note|jot\s+down|memo|""" +
            """save\s+(?:a\s+)?memo|new\s+note)""" +
            """[\s:]*(.+)$"""
    )

    private fun parseCreateNote(
        command: String,
        original: String
    ): AssistantAction? {

        val match = noteRegex.find(command) ?: return null
        val noteText = match.groupValues[1].trim()

        if (noteText.isBlank()) return null

        // Use original casing for the note text
        val startIndex = original.length - noteText.length
        val originalText = if (startIndex >= 0) {
            original.substring(startIndex).trim()
        } else {
            noteText
        }

        return AssistantAction.CreateNote(originalText)
    }

    // ── Delete Note ─────────────────────────────────────────────

    private val deleteNoteRegex = Regex(
        """^(?:delete|remove|erase)\s+note\s+(?:#?\s*)?(\d+)$"""
    )

    private fun parseDeleteNote(command: String): AssistantAction? {

        val match = deleteNoteRegex.find(command) ?: return null
        val index = match.groupValues[1].toIntOrNull() ?: return null

        if (index < 1) return null

        return AssistantAction.DeleteNote(index - 1) // Convert to 0-indexed
    }

    // ── Get Notes ───────────────────────────────────────────────

    private val getNotesRegex = Regex(
        """^(?:show|list|get|display|view|read)\s+""" +
            """(?:my\s+|all\s+)?(?:notes|memos|note|memo)$"""
    )

    private fun parseGetNotes(command: String): AssistantAction? {
        return if (getNotesRegex.matches(command)) {
            AssistantAction.GetNotes
        } else {
            null
        }
    }

    // ── Create Reminder ─────────────────────────────────────────

    private val reminderRegex = Regex(
        """^(?:remind\s+me\s+(?:to\s+)?|set\s+(?:a\s+)?reminder\s+""" +
            """(?:to\s+|for\s+)?|reminder[\s:]*)(.+)$"""
    )

    private fun parseCreateReminder(
        command: String,
        original: String
    ): AssistantAction? {

        val match = reminderRegex.find(command) ?: return null
        val text = match.groupValues[1].trim()

        if (text.isBlank()) return null

        return AssistantAction.CreateReminder(text)
    }

    // ── Make Call ────────────────────────────────────────────────

    // Matches: "call 9876543210", "phone 555-1234",
    // "make a call to 9876543210", "dial 1234567890"
    private val callRegex = Regex(
        """^(?:call|phone|dial|ring|make\s+(?:a\s+)?call\s+(?:to\s+)?)""" +
            """\s*([\d\s\-+()]+)$"""
    )

    private fun parseMakeCall(command: String): AssistantAction? {

        val match = callRegex.find(command) ?: return null
        val number = match.groupValues[1]
            .replace(Regex("[\\s\\-()]+"), "")
            .trim()

        if (number.length < 3) return null

        return AssistantAction.MakeCall(number)
    }

    // ── Send Message ────────────────────────────────────────────

    // Matches: "send message to 9876543210 saying hello",
    // "text 555-1234 hey there", "sms 9876543210 meeting at 5"
    private val sendMessageRegex = Regex(
        """^(?:send\s+(?:a\s+)?(?:message|text|sms)|text|sms)""" +
            """\s+(?:to\s+)?([\d\s\-+()]+?)""" +
            """\s+(?:saying|that|message|:)?\s*(.+)$"""
    )

    private fun parseSendMessage(
        command: String,
        original: String
    ): AssistantAction? {

        val match = sendMessageRegex.find(command) ?: return null

        val number = match.groupValues[1]
            .replace(Regex("[\\s\\-()]+"), "")
            .trim()

        val messageBody = match.groupValues[2].trim()

        if (number.length < 3 || messageBody.isBlank()) return null

        // Preserve original message casing
        val originalMessage = original
            .substring(original.length - messageBody.length)
            .trim()

        return AssistantAction.SendMessage(number, originalMessage)
    }

    // ── Play Music ──────────────────────────────────────────────

    private val playMusicRegex = Regex(
        """^(?:play\s+(?:some\s+)?music|open\s+music(?:\s+player)?|""" +
            """start\s+music|music\s+player)$"""
    )

    private fun parsePlayMusic(command: String): AssistantAction? {
        return if (playMusicRegex.matches(command)) {
            AssistantAction.PlayMusic
        } else {
            null
        }
    }

    // ── Device Info ─────────────────────────────────────────────

    private val deviceInfoRegex = Regex(
        """^(?:device\s+info|system\s+info|phone\s+info|""" +
            """battery\s+(?:status|level|info)|about\s+(?:this\s+)?phone|""" +
            """show\s+device\s+info|what(?:'s|\s+is)\s+my\s+""" +
            """(?:phone|device|battery))$"""
    )

    private fun parseGetDeviceInfo(command: String): AssistantAction? {
        return if (deviceInfoRegex.matches(command)) {
            AssistantAction.GetDeviceInfo
        } else {
            null
        }
    }

    // ── Search Web ──────────────────────────────────────────────

    private val searchWebRegex = Regex(
        """^(?:search\s+(?:for\s+|the\s+web\s+for\s+)?|""" +
            """google\s+|look\s+up\s+|find\s+(?:out\s+)?)(.+)$"""
    )

    private fun parseSearchWeb(
        command: String,
        original: String
    ): AssistantAction? {

        val match = searchWebRegex.find(command) ?: return null
        val query = match.groupValues[1].trim()

        if (query.isBlank()) return null

        return AssistantAction.SearchWeb(query)
    }

    // ── Flashlight ──────────────────────────────────────────────

    private val flashlightOnRegex = Regex(
        """^(?:turn\s+on\s+(?:the\s+)?(?:flashlight|torch)|""" +
            """(?:flashlight|torch)\s+on|enable\s+(?:the\s+)?""" +
            """(?:flashlight|torch))$"""
    )

    private val flashlightOffRegex = Regex(
        """^(?:turn\s+off\s+(?:the\s+)?(?:flashlight|torch)|""" +
            """(?:flashlight|torch)\s+off|disable\s+(?:the\s+)?""" +
            """(?:flashlight|torch))$"""
    )

    private fun parseFlashlight(command: String): AssistantAction? {
        return when {
            flashlightOnRegex.matches(command) ->
                AssistantAction.ToggleFlashlight(true)
            flashlightOffRegex.matches(command) ->
                AssistantAction.ToggleFlashlight(false)
            else -> null
        }
    }

    // ── Take Photo ──────────────────────────────────────────────

    private val takePhotoRegex = Regex(
        """^(?:take\s+(?:a\s+)?(?:photo|picture|selfie|pic)|""" +
            """capture\s+(?:a\s+)?(?:photo|picture|image)|""" +
            """open\s+camera\s+(?:and\s+)?(?:take|capture))$"""
    )

    private fun parseTakePhoto(command: String): AssistantAction? {
        return if (takePhotoRegex.matches(command)) {
            AssistantAction.TakePhoto
        } else {
            null
        }
    }

    // ── Open Settings ───────────────────────────────────────────

    private val openSettingsRegex = Regex(
        """^(?:open|go\s+to|show)\s+(?:the\s+)?settings""" +
            """(?:\s+(.+))?$"""
    )

    private fun parseOpenSettings(command: String): AssistantAction? {

        val match = openSettingsRegex.find(command) ?: return null
        val section = match.groupValues[1].trim().ifBlank { null }

        return AssistantAction.OpenSettings(section)
    }

    // ── Brightness ──────────────────────────────────────────────

    private val brightnessRegex = Regex(
        """^(?:set\s+)?brightness\s+(?:to\s+)?(\d{1,3})\s*%?$"""
    )

    private fun parseBrightness(command: String): AssistantAction? {

        val match = brightnessRegex.find(command) ?: return null
        val level = match.groupValues[1].toIntOrNull() ?: return null

        if (level !in 0..100) return null

        return AssistantAction.SetBrightness(level)
    }

    // ── WiFi ────────────────────────────────────────────────────

    private val wifiOnRegex = Regex(
        """^(?:turn\s+on\s+(?:the\s+)?wi-?fi|""" +
            """wi-?fi\s+on|enable\s+(?:the\s+)?wi-?fi|""" +
            """connect\s+(?:to\s+)?wi-?fi)$"""
    )

    private val wifiOffRegex = Regex(
        """^(?:turn\s+off\s+(?:the\s+)?wi-?fi|""" +
            """wi-?fi\s+off|disable\s+(?:the\s+)?wi-?fi|""" +
            """disconnect\s+wi-?fi)$"""
    )

    private fun parseWifi(command: String): AssistantAction? {
        return when {
            wifiOnRegex.matches(command) ->
                AssistantAction.ToggleWifi(true)
            wifiOffRegex.matches(command) ->
                AssistantAction.ToggleWifi(false)
            else -> null
        }
    }

    // ── Bluetooth ───────────────────────────────────────────────

    private val bluetoothOnRegex = Regex(
        """^(?:turn\s+on\s+(?:the\s+)?bluetooth|""" +
            """bluetooth\s+on|enable\s+(?:the\s+)?bluetooth)$"""
    )

    private val bluetoothOffRegex = Regex(
        """^(?:turn\s+off\s+(?:the\s+)?bluetooth|""" +
            """bluetooth\s+off|disable\s+(?:the\s+)?bluetooth)$"""
    )

    private fun parseBluetooth(command: String): AssistantAction? {
        return when {
            bluetoothOnRegex.matches(command) ->
                AssistantAction.ToggleBluetooth(true)
            bluetoothOffRegex.matches(command) ->
                AssistantAction.ToggleBluetooth(false)
            else -> null
        }
    }
}