package com.example.emotion

enum class UserEmotion(val label: String, val pitchModifier: Float, val speedModifier: Float) {
    HAPPY("HAPPY", 1.04f, 1.05f),
    EXCITED("EXCITED", 1.08f, 1.10f),
    CALM("CALM", 0.95f, 0.98f),
    SAD("SAD", 0.90f, 0.92f),
    ANGRY("ANGRY", 0.92f, 0.98f),
    FRUSTRATED("FRUSTRATED", 0.93f, 1.00f),
    STRESSED("STRESSED", 0.94f, 0.95f),
    WORRIED("WORRIED", 0.94f, 0.95f),
    CONFUSED("CONFUSED", 0.96f, 0.94f),
    BORED("BORED", 0.96f, 1.02f),
    DISAPPOINTED("DISAPPOINTED", 0.91f, 0.93f),
    SURPRISED("SURPRISED", 1.05f, 1.05f),
    CURIOUS("CURIOUS", 1.01f, 1.01f),
    NEUTRAL("NEUTRAL", 0.96f, 1.02f)
}

enum class EmotionConfidence {
    LOW,
    MEDIUM,
    HIGH
}

data class EmotionalEstimate(
    val emotion: UserEmotion,
    val confidence: EmotionConfidence,
    val summaryCues: String = ""
)

object EmotionDetector {

    fun estimateEmotion(
        text: String,
        isVoiceInput: Boolean = false,
        amplitude: Float = 0f
    ): EmotionalEstimate {
        val lower = text.lowercase().trim()

        // 1. Check for strong emojis
        if (lower.contains("😡") || lower.contains("🤬") || lower.contains("💢")) {
            return EmotionalEstimate(UserEmotion.ANGRY, EmotionConfidence.HIGH, "Aggressive cues / angry emoji")
        }
        if (lower.contains("🔥") && (lower.contains("ভালো") || lower.contains("awesome") || lower.contains("great") || lower.contains("nice"))) {
            return EmotionalEstimate(UserEmotion.EXCITED, EmotionConfidence.HIGH, "High-energy enthusiasm")
        }
        if (lower.contains("😊") || lower.contains("😀") || lower.contains("😄") || lower.contains("❤️") || lower.contains("🎉")) {
            return EmotionalEstimate(UserEmotion.HAPPY, EmotionConfidence.HIGH, "Positive expressions")
        }
        if (lower.contains("😢") || lower.contains("😭") || lower.contains("💔") || lower.contains("😔")) {
            return EmotionalEstimate(UserEmotion.SAD, EmotionConfidence.HIGH, "Sorrow / crying indicators")
        }
        if (lower.contains("🤔") || lower.contains("🧐")) {
            return EmotionalEstimate(UserEmotion.CURIOUS, EmotionConfidence.HIGH, "Pondering / curiosity")
        }
        if (lower.contains("😴")) {
            return EmotionalEstimate(UserEmotion.BORED, EmotionConfidence.HIGH, "Low interest / boredom")
        }

        // 2. Bangla Emotion Signals
        // Frustrated / Angry in Bangla
        if (lower.contains("ধুর") || lower.contains("আবার কাজ করল না") || lower.contains("রাগ হচ্ছে") || lower.contains("বিরক্ত") || lower.contains("কাজ করছে না কেন")) {
            val conf = if (lower.contains("আবার") || lower.contains("ধুর")) EmotionConfidence.HIGH else EmotionConfidence.MEDIUM
            return EmotionalEstimate(UserEmotion.FRUSTRATED, conf, "Repeated failure / frustration signals in Bangla")
        }
        // Confused in Bangla
        if (lower.contains("কিছুই বুঝতে পারছি না") || lower.contains("বুঝতে পারছি না") || lower.contains("বুঝলাম না") || lower.contains("কীভাবে করব") || lower.contains("কি করব")) {
            return EmotionalEstimate(UserEmotion.CONFUSED, EmotionConfidence.HIGH, "Comprehension difficulty in Bangla")
        }
        // Happy / Excited in Bangla
        if (lower.contains("ভালো লাগছে") || lower.contains("দারুণ") || lower.contains("অনেক ভালো") || lower.contains("খুশি") || lower.contains("সেরা")) {
            val conf = if (lower.contains("অনেক") || lower.contains("দারুণ")) EmotionConfidence.HIGH else EmotionConfidence.MEDIUM
            return EmotionalEstimate(if (lower.contains("অনেক") || lower.contains("দারুণ")) UserEmotion.EXCITED else UserEmotion.HAPPY, conf, "Positive affirmation in Bangla")
        }
        // Sad in Bangla
        if (lower.contains("মন খারাপ") || lower.contains("কষ্ট লাগছে") || lower.contains("খারাপ লাগছে") || lower.contains("হতাশ")) {
            return EmotionalEstimate(UserEmotion.SAD, EmotionConfidence.HIGH, "Sorrow expression in Bangla")
        }
        // Stressed / Worried in Bangla
        if (lower.contains("ভয় লাগছে") || lower.contains("টেনশন") || lower.contains("চিন্তা হচ্ছে") || lower.contains("চাপ লাগছে")) {
            return EmotionalEstimate(UserEmotion.STRESSED, EmotionConfidence.HIGH, "Anxiety / stress in Bangla")
        }

        // 3. English Emotion Signals
        // Frustrated / Angry
        if (lower.contains("failed again") || lower.contains("not working") || lower.contains("damn") || lower.contains("pissed") || lower.contains("so annoyed") || lower.contains("hate this")) {
            return EmotionalEstimate(UserEmotion.FRUSTRATED, EmotionConfidence.HIGH, "Frustration indicators")
        }
        if (lower.contains("angry") || lower.contains("furious") || lower.contains("mad at")) {
            return EmotionalEstimate(UserEmotion.ANGRY, EmotionConfidence.HIGH, "Anger signals")
        }
        // Confused
        if (lower.contains("don't understand") || lower.contains("no idea") || lower.contains("so confused") || lower.contains("what does this mean") || lower.contains("make no sense")) {
            return EmotionalEstimate(UserEmotion.CONFUSED, EmotionConfidence.HIGH, "Confusion signals")
        }
        // Happy / Excited
        if (lower.contains("feeling great") || lower.contains("so happy") || lower.contains("love this") || lower.contains("awesome") || lower.contains("let's go") || lower.contains("fantastic")) {
            return EmotionalEstimate(if (lower.contains("awesome") || lower.contains("let's go") || text.contains("!")) UserEmotion.EXCITED else UserEmotion.HAPPY, EmotionConfidence.HIGH, "Enthusiastic positivity")
        }
        // Sad / Disappointed
        if (lower.contains("feeling down") || lower.contains("depressed") || lower.contains("so sad") || lower.contains("unhappy") || lower.contains("disappointed")) {
            return EmotionalEstimate(if (lower.contains("disappointed")) UserEmotion.DISAPPOINTED else UserEmotion.SAD, EmotionConfidence.MEDIUM, "Low mood expression")
        }
        // Stressed / Worried
        if (lower.contains("so stressed") || lower.contains("worried about") || lower.contains("panicking") || lower.contains("anxious") || lower.contains("deadline is today")) {
            return EmotionalEstimate(UserEmotion.STRESSED, EmotionConfidence.HIGH, "High pressure / stress")
        }
        // Curious
        if (lower.contains("curious") || lower.contains("how does") || lower.contains("why does") || lower.contains("wondering if") || lower.contains("tell me how")) {
            return EmotionalEstimate(UserEmotion.CURIOUS, EmotionConfidence.MEDIUM, "Inquiry / curiosity")
        }
        // Bored
        if (lower.contains("bored") || lower.contains("nothing to do") || lower.contains("so dull")) {
            return EmotionalEstimate(UserEmotion.BORED, EmotionConfidence.MEDIUM, "Low engagement")
        }

        // 4. Voice Telemetry heuristics (if voice input)
        if (isVoiceInput && amplitude > 0.8f) {
            return EmotionalEstimate(UserEmotion.EXCITED, EmotionConfidence.LOW, "High volume acoustic peak")
        }

        return EmotionalEstimate(UserEmotion.NEUTRAL, EmotionConfidence.MEDIUM, "Standard conversational tone")
    }
}
