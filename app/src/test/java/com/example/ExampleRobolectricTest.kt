package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.JarvisDatabase
import com.example.data.local.JarvisMemoryEntity
import com.example.data.local.SessionBackupManager
import com.example.data.local.UserProfileEntity
import com.example.data.model.StarkProtocolsRepository
import com.example.ui.theme.HudThemeId
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read app name from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("JARVIS", appName)
  }

  @Test
  fun `verify stark protocols defined`() {
    val protocols = StarkProtocolsRepository.defaultProtocols
    assertTrue(protocols.size >= 6)
    val houseParty = protocols.firstOrNull { it.id == "house_party" }
    assertTrue(houseParty != null)
    val veronica = protocols.firstOrNull { it.id == "veronica" }
    assertTrue(veronica != null)
  }

  @Test
  fun `verify 8 hud themes defined`() {
    assertEquals(8, HudThemeId.values().size)
  }

  @Test
  fun `verify multi user persistence and session backup`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = JarvisDatabase.getDatabase(context)
    val dao = db.jarvisDao()

    val user1 = UserProfileEntity(
      userId = "user_midul",
      userName = "Midul",
      formalTitle = "Sir",
      preferredLanguage = "Hinglish",
      isActiveUser = true
    )
    val user2 = UserProfileEntity(
      userId = "user_tony",
      userName = "Tony Stark",
      formalTitle = "Dr. Stark",
      preferredLanguage = "English",
      isActiveUser = false
    )

    dao.saveUserProfile(user1)
    dao.saveUserProfile(user2)

    val memory = JarvisMemoryEntity(
      userId = "user_midul",
      category = "PREFERENCE",
      fact = "Prefers dark HUD theme and fast response"
    )
    dao.insertMemory(memory)

    // Verify retrieval
    val active = dao.getActiveUserProfile()
    assertNotNull(active)
    assertEquals("Midul", active?.userName)

    val midulMemories = dao.getMemoriesForUser("user_midul")
    assertEquals(1, midulMemories.size)
    assertEquals("PREFERENCE", midulMemories[0].category)

    // Verify session backup save & restore
    SessionBackupManager.saveBackup(
      context = context,
      activeProfile = user1,
      allProfiles = listOf(user1, user2),
      allMemories = listOf(memory),
      recentLogs = emptyList()
    )

    val restored = SessionBackupManager.restoreBackupIfAvailable(context, dao)
    assertNotNull(restored)
    assertEquals("Midul", restored?.userName)
  }

  @Test
  fun `verify emotion detection for Bangla and English inputs`() {
    val happyInput = "ভাই আজকে আমার অনেক ভালো লাগছে! 🔥"
    val happyEst = com.example.emotion.EmotionDetector.estimateEmotion(happyInput)
    assertTrue(happyEst.emotion == com.example.emotion.UserEmotion.EXCITED || happyEst.emotion == com.example.emotion.UserEmotion.HAPPY)

    val frustratedInput = "ধুর, আবার কাজ করল না 😡"
    val frustEst = com.example.emotion.EmotionDetector.estimateEmotion(frustratedInput)
    assertTrue(frustEst.emotion == com.example.emotion.UserEmotion.FRUSTRATED || frustEst.emotion == com.example.emotion.UserEmotion.ANGRY)

    val confusedInput = "আমি কিছুই বুঝতে পারছি না।"
    val confEst = com.example.emotion.EmotionDetector.estimateEmotion(confusedInput)
    assertEquals(com.example.emotion.UserEmotion.CONFUSED, confEst.emotion)

    // Voice pitch & speed bounds check
    assertTrue(happyEst.emotion.pitchModifier in 0.8f..1.3f)
    assertTrue(happyEst.emotion.speedModifier in 0.8f..1.3f)
  }
}
