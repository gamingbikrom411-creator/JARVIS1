package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object SessionBackupManager {
    private const val PREFS_NAME = "jarvis_session_backup_prefs"
    private const val KEY_ACTIVE_USER_ID = "active_user_id"
    private const val KEY_ACTIVE_USER_NAME = "active_user_name"
    private const val KEY_BACKUP_TIMESTAMP = "backup_timestamp"
    private const val BACKUP_FILE_NAME = "jarvis_permanent_session_backup.json"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    suspend fun saveBackup(
        context: Context,
        activeProfile: UserProfileEntity?,
        allProfiles: List<UserProfileEntity>,
        allMemories: List<JarvisMemoryEntity>,
        recentLogs: List<ConversationLogEntity>
    ) = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject().apply {
                put("version", 2)
                put("timestamp", System.currentTimeMillis())
                put("activeUserId", activeProfile?.userId ?: "default_user")
                put("activeUserName", activeProfile?.userName ?: "Sir")

                // Profiles
                val profilesArray = JSONArray()
                allProfiles.forEach { p ->
                    profilesArray.put(JSONObject().apply {
                        put("userId", p.userId)
                        put("userName", p.userName)
                        put("formalTitle", p.formalTitle)
                        put("roleOrDesignation", p.roleOrDesignation)
                        put("preferredLanguage", p.preferredLanguage)
                        put("activeThemeId", p.activeThemeId)
                        put("voiceSpeed", p.voiceSpeed.toDouble())
                        put("voicePitch", p.voicePitch.toDouble())
                        put("isAutoListenEnabled", p.isAutoListenEnabled)
                        put("isActiveUser", p.isActiveUser)
                        put("totalProtocolsInitiated", p.totalProtocolsInitiated)
                        put("totalInteractions", p.totalInteractions)
                        put("lastUpdated", p.lastUpdated)
                    })
                }
                put("profiles", profilesArray)

                // Memories
                val memoriesArray = JSONArray()
                allMemories.forEach { m ->
                    memoriesArray.put(JSONObject().apply {
                        put("id", m.id)
                        put("userId", m.userId)
                        put("category", m.category)
                        put("fact", m.fact)
                        put("timestamp", m.timestamp)
                    })
                }
                put("memories", memoriesArray)

                // Conversation Logs
                val logsArray = JSONArray()
                recentLogs.take(50).forEach { l ->
                    logsArray.put(JSONObject().apply {
                        put("id", l.id)
                        put("userId", l.userId)
                        put("sender", l.sender)
                        put("text", l.text)
                        put("language", l.language)
                        put("isVoice", l.isVoice)
                        put("timestamp", l.timestamp)
                    })
                }
                put("conversations", logsArray)
            }

            // 1. Write to persistent file
            val file = File(context.filesDir, BACKUP_FILE_NAME)
            file.writeText(root.toString())

            // 2. Write key markers to SharedPreferences
            getPrefs(context).edit().apply {
                putString(KEY_ACTIVE_USER_ID, activeProfile?.userId ?: "default_user")
                putString(KEY_ACTIVE_USER_NAME, activeProfile?.userName ?: "Sir")
                putLong(KEY_BACKUP_TIMESTAMP, System.currentTimeMillis())
                apply()
            }
        } catch (_: Exception) {}
    }

    suspend fun restoreBackupIfAvailable(
        context: Context,
        dao: JarvisDao
    ): UserProfileEntity? = withContext(Dispatchers.IO) {
        try {
            val file = File(context.filesDir, BACKUP_FILE_NAME)
            if (!file.exists()) return@withContext null

            val text = file.readText()
            if (text.isBlank()) return@withContext null

            val root = JSONObject(text)
            val activeUserId = root.optString("activeUserId", "default_user")

            // Restore Profiles
            val profilesArray = root.optJSONArray("profiles")
            var restoredActive: UserProfileEntity? = null
            if (profilesArray != null) {
                for (i in 0 until profilesArray.length()) {
                    val pObj = profilesArray.getJSONObject(i)
                    val uid = pObj.optString("userId", "default_user")
                    val profile = UserProfileEntity(
                        userId = uid,
                        userName = pObj.optString("userName", "Sir"),
                        formalTitle = pObj.optString("formalTitle", "Sir"),
                        roleOrDesignation = pObj.optString("roleOrDesignation", "Chief Executive"),
                        preferredLanguage = pObj.optString("preferredLanguage", "Hinglish"),
                        activeThemeId = pObj.optString("activeThemeId", "CLASSIC_CYAN"),
                        voiceSpeed = pObj.optDouble("voiceSpeed", 1.0).toFloat(),
                        voicePitch = pObj.optDouble("voicePitch", 0.95).toFloat(),
                        isAutoListenEnabled = pObj.optBoolean("isAutoListenEnabled", true),
                        isActiveUser = (uid == activeUserId),
                        totalProtocolsInitiated = pObj.optInt("totalProtocolsInitiated", 0),
                        totalInteractions = pObj.optInt("totalInteractions", 0),
                        lastUpdated = pObj.optLong("lastUpdated", System.currentTimeMillis())
                    )
                    dao.saveUserProfile(profile)
                    if (profile.isActiveUser) {
                        restoredActive = profile
                    }
                }
            }

            // Restore Memories
            val memoriesArray = root.optJSONArray("memories")
            if (memoriesArray != null) {
                for (i in 0 until memoriesArray.length()) {
                    val mObj = memoriesArray.getJSONObject(i)
                    dao.insertMemory(
                        JarvisMemoryEntity(
                            id = mObj.optLong("id", 0),
                            userId = mObj.optString("userId", "default_user"),
                            category = mObj.optString("category", "IDENTITY"),
                            fact = mObj.optString("fact", ""),
                            timestamp = mObj.optLong("timestamp", System.currentTimeMillis())
                        )
                    )
                }
            }

            // Restore Conversations
            val logsArray = root.optJSONArray("conversations")
            if (logsArray != null) {
                for (i in 0 until logsArray.length()) {
                    val lObj = logsArray.getJSONObject(i)
                    dao.insertConversation(
                        ConversationLogEntity(
                            id = lObj.optLong("id", 0),
                            userId = lObj.optString("userId", "default_user"),
                            sender = lObj.optString("sender", "USER"),
                            text = lObj.optString("text", ""),
                            language = lObj.optString("language", "en"),
                            isVoice = lObj.optBoolean("isVoice", true),
                            timestamp = lObj.optLong("timestamp", System.currentTimeMillis())
                        )
                    )
                }
            }

            return@withContext restoredActive
        } catch (e: Exception) {
            return@withContext null
        }
    }

    fun getLastBackupInfo(context: Context): Pair<String, Long> {
        val prefs = getPrefs(context)
        val name = prefs.getString(KEY_ACTIVE_USER_NAME, "Sir") ?: "Sir"
        val time = prefs.getLong(KEY_BACKUP_TIMESTAMP, 0L)
        return Pair(name, time)
    }
}
