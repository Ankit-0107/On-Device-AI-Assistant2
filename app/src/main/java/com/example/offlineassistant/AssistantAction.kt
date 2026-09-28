package com.example.offlineassistant

sealed class AssistantAction {

    data class OpenApp(
        val packageName: String
    ) : AssistantAction()

    data class CreateNote(
        val text: String
    ) : AssistantAction()

    object GetNotes : AssistantAction()

    data class DeleteNote(
        val index: Int
    ) : AssistantAction()

    data class SetAlarm(
        val hour: Int,
        val minute: Int,
        val label: String? = null
    ) : AssistantAction()

    data class SetTimer(
        val seconds: Int,
        val label: String? = null
    ) : AssistantAction()

    data class CreateReminder(
        val text: String
    ) : AssistantAction()

    data class MakeCall(
        val phoneNumber: String
    ) : AssistantAction()

    data class SendMessage(
        val phoneNumber: String,
        val message: String
    ) : AssistantAction()

    object PlayMusic : AssistantAction()

    object GetDeviceInfo : AssistantAction()

    data class SearchWeb(
        val query: String
    ) : AssistantAction()

    data class ToggleFlashlight(
        val turnOn: Boolean
    ) : AssistantAction()

    object TakePhoto : AssistantAction()

    data class OpenSettings(
        val section: String? = null
    ) : AssistantAction()

    data class SetBrightness(
        val level: Int
    ) : AssistantAction()

    data class ToggleWifi(
        val enable: Boolean
    ) : AssistantAction()

    data class ToggleBluetooth(
        val enable: Boolean
    ) : AssistantAction()
}