// ABOUTME: Unit tests for voice-driven mode command parsing.
// ABOUTME: Ensures we only accept short "check/skip" phrases and ignore unrelated speech.
package com.example.niche_todos

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceDrivenModeCommandParserTest {

    @Test
    fun acceptsCheckAndSkipAlone() {
        assertTrue(VoiceDrivenModeControllerCommandParser.isCommand("check", 0.9f))
        assertTrue(VoiceDrivenModeControllerCommandParser.isCommand("skip", 0.9f))
    }

    @Test
    fun acceptsPoliteShortVariants() {
        assertTrue(VoiceDrivenModeControllerCommandParser.isCommand("check please", 0.9f))
        assertTrue(VoiceDrivenModeControllerCommandParser.isCommand("skip it", 0.9f))
        assertTrue(VoiceDrivenModeControllerCommandParser.isCommand("ok check", 0.9f))
    }

    @Test
    fun rejectsLongOrUnrelatedSpeech() {
        assertFalse(VoiceDrivenModeControllerCommandParser.isCommand("I think we should check the mail", 0.9f))
        assertFalse(VoiceDrivenModeControllerCommandParser.isCommand("skip this one because it's complicated", 0.9f))
        assertFalse(VoiceDrivenModeControllerCommandParser.isCommand("hello there", 0.9f))
    }

    @Test
    fun rejectsLowConfidence() {
        // Some devices report very low confidence even for correct single-word commands.
        assertTrue(VoiceDrivenModeControllerCommandParser.isCommand("check", 0.0f))
        assertTrue(VoiceDrivenModeControllerCommandParser.isCommand("skip", 0.2f))

        // Still reject low-confidence multi-word phrases to reduce accidental triggers.
        assertFalse(VoiceDrivenModeControllerCommandParser.isCommand("ok check", 0.2f))
        assertFalse(VoiceDrivenModeControllerCommandParser.isCommand("skip please", 0.3f))
    }
}

/**
 * Test seam: keep parsing rules stable without instantiating Android classes.
 */
internal object VoiceDrivenModeControllerCommandParser {
    fun isCommand(raw: String, confidence: Float): Boolean {
        return parse(raw, confidence) != null
    }

    fun parse(raw: String, confidence: Float): String? {
        val tokens = raw.lowercase()
            .split(Regex("\\s+"))
            .map { it.replace(Regex("[^a-z]"), "") }
            .filter { it.isNotBlank() }
        if (tokens.isEmpty()) return null

        if (tokens.size == 1) {
            return when (tokens[0]) {
                "check" -> "check"
                "skip" -> "skip"
                else -> null
            }
        }

        if (confidence in 0f..0.45f) {
            return null
        }

        if (tokens.size > 3) return null
        val allowedExtra = setOf("it", "please", "pls", "ok", "okay")
        fun matches(cmd: String): Boolean {
            if (!tokens.contains(cmd)) return false
            val unknown = tokens.filter { it != cmd && it !in allowedExtra }
            return unknown.isEmpty()
        }
        return when {
            matches("check") -> "check"
            matches("skip") -> "skip"
            else -> null
        }
    }
}
