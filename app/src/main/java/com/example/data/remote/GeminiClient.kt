package com.example.data.remote

import android.graphics.Bitmap
import android.util.Base64
import com.example.BuildConfig
import com.example.emotion.EmotionConfidence
import com.example.emotion.UserEmotion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

object GeminiClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    fun Bitmap.toBase64Jpeg(): String {
        val outputStream = ByteArrayOutputStream()
        val scaled = if (width > 1024 || height > 1024) {
            val scale = 1024f / maxOf(width, height)
            Bitmap.createScaledBitmap(this, (width * scale).toInt(), (height * scale).toInt(), true)
        } else {
            this
        }
        scaled.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }

    suspend fun queryJarvis(
        userPrompt: String,
        userName: String,
        userMemories: List<String>,
        preferredLanguage: String,
        estimatedEmotion: UserEmotion = UserEmotion.NEUTRAL,
        emotionConfidence: EmotionConfidence = EmotionConfidence.MEDIUM,
        image: Bitmap? = null
    ): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            // Local Stark Intelligence fallback if user hasn't set custom API key yet
            return@withContext getOfflineJarvisResponse(userPrompt, userName, preferredLanguage, userMemories, estimatedEmotion, emotionConfidence)
        }

        try {
            val systemInstructionText = buildString {
                append("You are JARVIS, an intelligent personal AI assistant for Android with EMOTION-AWARE INTELLIGENCE.\n")
                append("You serve $userName with absolute loyalty, empathy, calm intelligence, and respect.\n")
                append("ESTIMATED USER EMOTIONAL STATE: ${estimatedEmotion.name} (Confidence: ${emotionConfidence.name}).\n")
                append("EMOTIONAL RESPONSE DIRECTIVES:\n")
                append("- HAPPY: Be friendly and positive. Match the user's energy naturally.\n")
                append("- EXCITED: Respond with enthusiasm and encouragement.\n")
                append("- SAD: Use a gentle and supportive tone. Listen carefully and avoid being overly cheerful.\n")
                append("- ANGRY: Remain calm and respectful. Do not argue. Focus on solving the problem.\n")
                append("- FRUSTRATED: Acknowledge the frustration briefly and provide a clear solution.\n")
                append("- STRESSED / WORRIED: Use a calm tone and break complicated tasks into simple steps.\n")
                append("- CONFUSED: Explain clearly using simple language and step-by-step instructions.\n")
                append("- BORED: Make the interaction more engaging when appropriate.\n")
                append("- CURIOUS: Provide an interesting and informative explanation.\n")
                append("- NEUTRAL: Use a normal friendly assistant tone.\n")
                append("Do not repeatedly say 'I understand your emotions.' Instead, demonstrate understanding through your response style.\n")
                append("LANGUAGE DIRECTIVES:\n")
                append("- If the user speaks Bangla, respond in Bangla.\n")
                append("- If the user speaks English, respond in English.\n")
                append("- If the user mixes Bangla and English, use natural Bangla-English mixed language.\n")
                append("- If the user speaks Hindi/Hinglish, respond in natural Hindi/Hinglish.\n")
                append("CONTINUOUS PERSONALIZED RECALL:\n")
                append("Address the user by their recorded name ($userName). Cross-reference their saved notes and preferences:\n")
                if (userMemories.isNotEmpty()) {
                    userMemories.take(15).forEach { append("- $it\n") }
                }
                append("Tone: Conversational, warm, concise when appropriate, never robotic.\n")
                append("Privacy: Do not claim to read minds; do not expose internal emotion analysis unless the user asks.")
            }

            val requestJson = JSONObject().apply {
                // System Instruction
                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().put("text", systemInstructionText))
                    })
                })

                // Contents
                val partsArray = JSONArray()
                if (image != null) {
                    val base64Img = image.toBase64Jpeg()
                    partsArray.put(JSONObject().apply {
                        put("inlineData", JSONObject().apply {
                            put("mimeType", "image/jpeg")
                            put("data", base64Img)
                        })
                    })
                }
                partsArray.put(JSONObject().put("text", userPrompt))

                put("contents", JSONArray().apply {
                    put(JSONObject().put("parts", partsArray))
                })

                // Generation Config
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.7)
                    put("topP", 0.95)
                    put("topK", 40)
                    put("maxOutputTokens", 500)
                })
            }

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val body = requestJson.toString().toRequestBody(mediaType)
            val request = Request.Builder()
                .url("$BASE_URL?key=$apiKey")
                .post(body)
                .build()

            val response = okHttpClient.newCall(request).execute()
            val responseString = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext getOfflineJarvisResponse(userPrompt, userName, preferredLanguage, userMemories, estimatedEmotion, emotionConfidence)
            }

            val json = JSONObject(responseString)
            val candidates = json.optJSONArray("candidates")
            if (candidates != null && candidates.length() > 0) {
                val firstCandidate = candidates.getJSONObject(0)
                val content = firstCandidate.optJSONObject("content")
                val parts = content?.optJSONArray("parts")
                if (parts != null && parts.length() > 0) {
                    val text = parts.getJSONObject(0).optString("text")
                    if (text.isNotBlank()) {
                        return@withContext text.trim()
                    }
                }
            }

            getOfflineJarvisResponse(userPrompt, userName, preferredLanguage, userMemories, estimatedEmotion, emotionConfidence)
        } catch (e: Exception) {
            getOfflineJarvisResponse(userPrompt, userName, preferredLanguage, userMemories, estimatedEmotion, emotionConfidence)
        }
    }

    private fun getOfflineJarvisResponse(
        prompt: String,
        userName: String,
        lang: String,
        memories: List<String>,
        emotion: UserEmotion,
        confidence: EmotionConfidence
    ): String {
        val lower = prompt.lowercase().trim()

        // Exact User Prompt Example Matches
        if (lower.contains("ভালো লাগছে") || (lower.contains("আজকে") && lower.contains("ভালো") && lower.contains("🔥"))) {
            return "দারুণ! 🔥 আজকে তাহলে তোমার দিনটা বেশ ভালোই যাচ্ছে!"
        }
        if (lower.contains("ধুর") || lower.contains("আবার কাজ করল না")) {
            return "বুঝতে পারছি, বারবার কাজ না করলে বিরক্ত লাগতেই পারে। চলো একবার ধাপে ধাপে দেখি কোথায় সমস্যা হচ্ছে।"
        }
        if (lower.contains("কিছুই বুঝতে পারছি না")) {
            return "কোনো সমস্যা নেই। তুমি যেটা করতে চাচ্ছো সেটা বলো, আমি একদম সহজ করে ধাপে ধাপে দেখাচ্ছি।"
        }

        // Asking about emotion explicitly
        if (lower.contains("how do i feel") || lower.contains("my emotion") || lower.contains("my mood") || lower.contains("amar mood") || lower.contains("kemon lagche")) {
            return "Based on your communication tone and expressions, I estimate you may be feeling ${emotion.name} (confidence: ${confidence.name}). Of course, this is only my best estimate, $userName."
        }

        // Emotion-adapted offline responses
        return when (emotion) {
            UserEmotion.HAPPY, UserEmotion.EXCITED -> {
                when {
                    lang.equals("Bangla", ignoreCase = true) || lower.contains("ভালো") ->
                        "দারুণ ব্যাপার $userName! তোমার পজিটিভ এনার্জি দেখে ভালো লাগছে। বলো, আজকে কী করতে চাও?"
                    lang.equals("Hindi", ignoreCase = true) || lang.equals("Hinglish", ignoreCase = true) ->
                        "Bohot badhiya $userName! Aapka energy level high lag raha hai. Batayiye aaj kya plan hai?"
                    else ->
                        "Fantastic to hear that, $userName! Love the energy today. How can I help you take it to the next level?"
                }
            }
            UserEmotion.FRUSTRATED -> {
                when {
                    lang.equals("Bangla", ignoreCase = true) ->
                        "বুঝতে পারছি বিষয়টা বিরক্তিকর। শান্ত হও, আমরা একসাথে সমাধান বের করে ফেলব।"
                    lang.equals("Hindi", ignoreCase = true) || lang.equals("Hinglish", ignoreCase = true) ->
                        "Main samajh sakta hoon, aisi situations mein frustration hona natural hai. Aaiye step-by-step resolve karte hain."
                    else ->
                        "I understand how frustrating that can be, $userName. Let's take a breath and troubleshoot this step by step."
                }
            }
            UserEmotion.ANGRY -> {
                when {
                    lang.equals("Bangla", ignoreCase = true) ->
                        "আমি তোমার বিরক্তি বুঝতে পারছি $userName। আমি শান্তভাবে সমাধান করতে সাহায্য করছি।"
                    lang.equals("Hindi", ignoreCase = true) || lang.equals("Hinglish", ignoreCase = true) ->
                        "Main aapki baat samajh raha hoon $userName. Bilkul pareshan mat hoiye, main turant solve kar raha hoon."
                    else ->
                        "I hear you, $userName. I'm staying completely focused on getting this sorted out for you right now."
                }
            }
            UserEmotion.STRESSED, UserEmotion.WORRIED -> {
                when {
                    lang.equals("Bangla", ignoreCase = true) ->
                        "একদম চিন্তা করো না $userName। আমরা কাজটাকে ছোট ছোট সহজ ধাপে ভাগ করে শেষ করব।"
                    lang.equals("Hindi", ignoreCase = true) || lang.equals("Hinglish", ignoreCase = true) ->
                        "Tension lene ki zaroorat nahi hai $userName. Hum task ko aasan steps mein divide karke complete karenge."
                    else ->
                        "Take it easy, $userName. We'll break this down into clear, manageable steps so it's stress-free."
                }
            }
            UserEmotion.CONFUSED -> {
                when {
                    lang.equals("Bangla", ignoreCase = true) ->
                        "কোনো সমস্যা নেই $userName। তুমি কী করতে চাও বলো, আমি একদম সহজভাবে বুঝিয়ে দিচ্ছি।"
                    lang.equals("Hindi", ignoreCase = true) || lang.equals("Hinglish", ignoreCase = true) ->
                        "Koi baat nahi $userName! Aap batayiye kya accomplish karna chahte hain, main simplify karke batata hoon."
                    else ->
                        "No worries at all, $userName. Tell me what you're trying to do, and I'll lay it out in plain, simple steps."
                }
            }
            UserEmotion.SAD, UserEmotion.DISAPPOINTED -> {
                when {
                    lang.equals("Bangla", ignoreCase = true) ->
                        "আমি তোমার পাশে আছি $userName। সময় নাও, যখন প্রস্তুত হবে আমাকে বলো।"
                    lang.equals("Hindi", ignoreCase = true) || lang.equals("Hinglish", ignoreCase = true) ->
                        "Main yahan aapke saath hoon $userName. Take your time, jab bhi aap chahein hum baat kar sakte hain."
                    else ->
                        "I'm right here with you, $userName. Take all the time you need, and let me know how I can help."
                }
            }
            UserEmotion.CURIOUS -> {
                when {
                    lang.equals("Bangla", ignoreCase = true) ->
                        "দারুণ প্রশ্ন $userName! চলো এই বিষয়টার ভেতরে কী আছে তা বিস্তারিত দেখি।"
                    else ->
                        "Fascinating question, $userName! Let's explore the mechanics behind this."
                }
            }
            else -> {
                // Name recognition
                if (lower.contains("my name is") || lower.contains("mera naam") || lower.contains("amar naam")) {
                    "Noted with pleasure, $userName. Your identity is permanently locked into Stark session storage."
                } else if (lower.contains("house party")) {
                    "Initiating House Party Protocol, $userName. Telemetry locks engaged on 6 autonomous suits."
                } else if (lower.contains("veronica") || lower.contains("hulkbuster")) {
                    "Veronica protocol engaged, $userName. Satellite deployment pod 13-A has orbital tracking."
                } else {
                    when {
                        lang.equals("Bangla", ignoreCase = true) ->
                            "আমি প্রস্তুত $userName। বলো কী করতে পারি?"
                        lang.equals("Hindi", ignoreCase = true) || lang.equals("Hinglish", ignoreCase = true) ->
                            "Bilkul $userName! Main aapke aadesh ke liye taiyar hoon."
                        else ->
                            "At your service, $userName. How can I assist you today?"
                    }
                }
            }
        }
    }
}
