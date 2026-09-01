package com.lucid.gallery.data

import android.content.Context
import android.content.SharedPreferences

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("gallery_settings", Context.MODE_PRIVATE)

    var sortMode: String
        get() = prefs.getString("sort_mode", "added") ?: "added"
        set(value) = prefs.edit().putString("sort_mode", value).apply()

    var selectedFilters: Set<String>
        get() = prefs.getStringSet("selected_filters", null)
            ?.takeIf { it.isNotEmpty() }
            ?: setOf("Camera")
        set(value) = prefs.edit().putStringSet(
            "selected_filters",
            if (value.isEmpty()) setOf("Camera") else value
        ).apply()
}