package io.github.prplegryn.katch.data

import android.content.Context
import android.net.Uri
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PreferencesRepository(context: Context) {
    private val prefs = context.getSharedPreferences("akatcha_settings", Context.MODE_PRIVATE)
    private val _settings = MutableStateFlow(readSettings())

    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    fun setThemeMode(mode: ThemeMode) = update {
        putString(KEY_THEME, mode.name)
    }

    fun setLastInput(input: String) = update {
        putString(KEY_LAST_INPUT, input)
    }

    fun setStoragePromptShown() = update {
        putBoolean(KEY_STORAGE_PROMPT, true)
    }

    fun setDownloadTree(uri: Uri, label: String) = update {
        putString(KEY_TREE_URI, uri.toString())
        putString(KEY_TREE_LABEL, label)
    }

    fun clearDownloadTree() = update {
        remove(KEY_TREE_URI)
        remove(KEY_TREE_LABEL)
    }

    private fun update(block: android.content.SharedPreferences.Editor.() -> Unit) {
        prefs.edit(commit = true) { block() }
        _settings.value = readSettings()
    }

    private fun readSettings(): AppSettings {
        val theme = prefs.getString(KEY_THEME, ThemeMode.System.name)
            ?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
            ?: ThemeMode.System
        val tree = prefs.getString(KEY_TREE_URI, null)?.let(Uri::parse)
        return AppSettings(
            themeMode = theme,
            downloadTreeUri = tree,
            downloadTreeLabel = prefs.getString(KEY_TREE_LABEL, null),
            lastInput = prefs.getString(KEY_LAST_INPUT, "").orEmpty(),
            storagePromptShown = prefs.getBoolean(KEY_STORAGE_PROMPT, false),
        )
    }

    private companion object {
        const val KEY_THEME = "theme"
        const val KEY_TREE_URI = "download_tree_uri"
        const val KEY_TREE_LABEL = "download_tree_label"
        const val KEY_LAST_INPUT = "last_input"
        const val KEY_STORAGE_PROMPT = "storage_prompt_shown"
    }
}
