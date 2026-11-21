package com.aas.mediaplayer.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.dataStore by preferencesDataStore(name = "playback_states")

data class MediaPlaybackState(
    val uri: String,
    val positionMs: Long = 0L,
    val speed: Float = 1.0f,
    val pitch: Float = 1.0f,
    val lastUpdated: Long = System.currentTimeMillis()
)

class PlaybackStateManager(private val context: Context) {

    // مفتاح فريد لكل ملف (URI) لتخزين حالته منفصلة
    private fun stateKey(uri: String) = stringPreferencesKey("state_$uri")

    suspend fun saveState(state: MediaPlaybackState) {
        val json = "${state.positionMs}|${state.speed}|${state.pitch}|${state.lastUpdated}"
        context.dataStore.edit { prefs -> prefs[stateKey(state.uri)] = json }
    }

    suspend fun getState(uri: String): MediaPlaybackState? {
        val prefs = context.dataStore.data.first()
        val json = prefs[stateKey(uri)] ?: return null
        val parts = json.split("|")
        return if (parts.size == 4) {
            MediaPlaybackState(
                uri = uri,
                positionMs = parts[0].toLongOrNull() ?: 0L,
                speed = parts[1].toFloatOrNull() ?: 1.0f,
                pitch = parts[2].toFloatOrNull() ?: 1.0f,
                lastUpdated = parts[3].toLongOrNull() ?: 0L
            )
        } else null
    }

    // جلب آخر ملف تم تشغيله (لزر "العودة إلى آخر ملف")
    suspend fun getLastPlayedUri(): String? {
        val prefs = context.dataStore.data.first()
        return prefs.asMap().entries
            .filter { it.key.name.startsWith("state_") }
            .mapNotNull { entry ->
                val parts = entry.value.toString().split("|")
                if (parts.size == 4) {
                    val uri = entry.key.name.removePrefix("state_")
                    val time = parts[3].toLongOrNull() ?: 0L
                    uri to time
                } else null
            }
            .maxByOrNull { it.second }?.first
    }
}
