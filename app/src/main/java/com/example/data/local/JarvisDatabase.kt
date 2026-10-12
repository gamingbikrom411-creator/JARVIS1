package com.example.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "user_profiles")
data class UserProfileEntity(
    @PrimaryKey val userId: String = "default_user",
    val userName: String = "Sir",
    val formalTitle: String = "Sir",
    val roleOrDesignation: String = "Chief Executive",
    val preferredLanguage: String = "Hinglish", // English, Hindi, Hinglish, Bangla
    val activeThemeId: String = "CLASSIC_CYAN",
    val voiceSpeed: Float = 1.0f,
    val voicePitch: Float = 0.95f,
    val isAutoListenEnabled: Boolean = true,
    val isActiveUser: Boolean = true,
    val totalProtocolsInitiated: Int = 0,
    val totalInteractions: Int = 0,
    val lastUpdated: Long = System.currentTimeMillis()
)

@Entity(tableName = "jarvis_memories")
data class JarvisMemoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String = "default_user",
    val category: String, // IDENTITY, PREFERENCE, PROJECT, INSTRUCTION, TASK_HISTORY, NOTE
    val fact: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "conversation_logs")
data class ConversationLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String = "default_user",
    val sender: String, // USER, JARVIS
    val text: String,
    val language: String = "en",
    val isVoice: Boolean = true,
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface JarvisDao {
    @Query("SELECT * FROM user_profiles ORDER BY lastUpdated DESC")
    fun getAllUserProfilesFlow(): Flow<List<UserProfileEntity>>

    @Query("SELECT * FROM user_profiles ORDER BY lastUpdated DESC")
    suspend fun getAllUserProfiles(): List<UserProfileEntity>

    @Query("SELECT * FROM user_profiles WHERE isActiveUser = 1 LIMIT 1")
    fun getActiveUserProfileFlow(): Flow<UserProfileEntity?>

    @Query("SELECT * FROM user_profiles WHERE isActiveUser = 1 LIMIT 1")
    suspend fun getActiveUserProfile(): UserProfileEntity?

    @Query("SELECT * FROM user_profiles WHERE userId = :userId LIMIT 1")
    suspend fun getUserProfileById(userId: String): UserProfileEntity?

    @Query("SELECT * FROM user_profiles WHERE LOWER(userName) = LOWER(:name) LIMIT 1")
    suspend fun getUserProfileByName(name: String): UserProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveUserProfile(profile: UserProfileEntity)

    @Query("UPDATE user_profiles SET isActiveUser = 0")
    suspend fun clearActiveFlags()

    @Transaction
    suspend fun switchActiveUser(userId: String) {
        clearActiveFlags()
        val profile = getUserProfileById(userId)
        if (profile != null) {
            saveUserProfile(profile.copy(isActiveUser = true, lastUpdated = System.currentTimeMillis()))
        }
    }

    @Query("DELETE FROM user_profiles WHERE userId = :userId")
    suspend fun deleteUserProfile(userId: String)

    @Query("SELECT * FROM jarvis_memories WHERE userId = :userId ORDER BY timestamp DESC")
    fun getMemoriesForUserFlow(userId: String): Flow<List<JarvisMemoryEntity>>

    @Query("SELECT * FROM jarvis_memories WHERE userId = :userId ORDER BY timestamp DESC")
    suspend fun getMemoriesForUser(userId: String): List<JarvisMemoryEntity>

    @Query("SELECT * FROM jarvis_memories ORDER BY timestamp DESC")
    suspend fun getAllMemories(): List<JarvisMemoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemory(memory: JarvisMemoryEntity)

    @Query("DELETE FROM jarvis_memories WHERE id = :id")
    suspend fun deleteMemory(id: Long)

    @Query("DELETE FROM jarvis_memories WHERE userId = :userId")
    suspend fun clearMemoriesForUser(userId: String)

    @Query("DELETE FROM jarvis_memories")
    suspend fun clearAllMemories()

    @Query("SELECT * FROM conversation_logs WHERE userId = :userId ORDER BY timestamp DESC LIMIT 100")
    fun getRecentConversationsForUserFlow(userId: String): Flow<List<ConversationLogEntity>>

    @Query("SELECT * FROM conversation_logs ORDER BY timestamp DESC LIMIT 100")
    suspend fun getAllConversations(): List<ConversationLogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversation(log: ConversationLogEntity)

    @Query("DELETE FROM conversation_logs WHERE userId = :userId")
    suspend fun clearConversationsForUser(userId: String)
}

@Database(
    entities = [UserProfileEntity::class, JarvisMemoryEntity::class, ConversationLogEntity::class],
    version = 2,
    exportSchema = false
)
abstract class JarvisDatabase : RoomDatabase() {
    abstract fun jarvisDao(): JarvisDao

    companion object {
        @Volatile
        private var INSTANCE: JarvisDatabase? = null

        fun getDatabase(context: Context): JarvisDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    JarvisDatabase::class.java,
                    "jarvis_stark_db"
                ).fallbackToDestructiveMigration(dropAllTables = true).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
