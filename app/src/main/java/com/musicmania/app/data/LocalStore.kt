package com.musicmania.app.data

import android.content.Context
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.UUID

class LocalStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("music_mania_store", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    fun loadSongs(key: String): List<Song> = runCatching {
        json.decodeFromString<List<Song>>(prefs.getString(key, null) ?: "[]")
    }.getOrDefault(emptyList())

    fun saveSongs(key: String, songs: List<Song>) { prefs.edit().putString(key, json.encodeToString(songs)).apply() }
    fun loadLog(): Map<String, Float> = runCatching {
        json.decodeFromString<Map<String, Float>>(prefs.getString("log", null) ?: "{}")
    }.getOrDefault(emptyMap())
    fun saveLog(log: Map<String, Float>) { prefs.edit().putString("log", json.encodeToString(log)).apply() }

    fun getProfileName(): String = prefs.getString("profile_name", null) ?: "MUSIC MANIA USER"

    fun setProfileName(name: String) {
        val clean = name.trim().take(40)
        if (clean.isNotBlank()) prefs.edit().putString("profile_name", clean).apply()
    }

    fun getInstallId(): String = prefs.getString("install_id", null) ?: UUID.randomUUID().toString().also {
        prefs.edit().putString("install_id", it).apply()
    }
    fun getMemberSince(): String = prefs.getString("member_since", null) ?: SimpleDateFormat("MMM yyyy", Locale.US).format(java.util.Date()).also {
        prefs.edit().putString("member_since", it).apply()
    }

    fun setLastSong(song: Song, index: Int, queue: List<Song>) {
        prefs.edit()
            .putString("last_song", json.encodeToString(song))
            .putString("last_queue", json.encodeToString(queue))
            .putInt("last_index", index)
            .apply()
    }
    fun loadLastQueue(): List<Song> = runCatching {
        json.decodeFromString<List<Song>>(prefs.getString("last_queue", null) ?: "[]")
    }.getOrDefault(emptyList())
    fun lastIndex(): Int = prefs.getInt("last_index", 0)
}

object Days {
    fun key(daysAgo: Int): String {
        val c = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -daysAgo) }
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(c.time)
    }
}
