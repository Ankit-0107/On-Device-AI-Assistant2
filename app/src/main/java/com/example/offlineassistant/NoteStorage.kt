package com.example.offlineassistant

import android.content.Context

class NoteStorage(
    context: Context
) {

    private val preferences =
        context.getSharedPreferences(
            "offline_assistant_notes",
            Context.MODE_PRIVATE
        )

    fun saveNote(note: String) {

        val notes = getNotes().toMutableList()

        notes.add(note)

        preferences.edit()
            .putString(
                "notes",
                notes.joinToString("\n")
            )
            .apply()
    }

    fun getNotes(): List<String> {

        val savedNotes =
            preferences.getString("notes", "") ?: ""

        if (savedNotes.isBlank()) {
            return emptyList()
        }

        return savedNotes.split("\n")
    }

    fun deleteNote(index: Int) {

        val notes = getNotes().toMutableList()

        if (index in notes.indices) {

            notes.removeAt(index)

            preferences.edit()
                .putString(
                    "notes",
                    notes.joinToString("\n")
                )
                .apply()
        }
    }

    fun clearAllNotes() {

        preferences.edit()
            .remove("notes")
            .apply()
    }
}