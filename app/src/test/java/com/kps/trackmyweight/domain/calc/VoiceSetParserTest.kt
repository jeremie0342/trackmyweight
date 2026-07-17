package com.kps.trackmyweight.domain.calc

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class VoiceSetParserTest {
    @Test fun `parses N reps à X kilos`() {
        val r = VoiceSetParser.parse("12 reps à 80 kilos")
        assertNotNull(r)
        assertEquals(12, r!!.reps)
        assertEquals(80f, r.weightKg, 0.01f)
    }

    @Test fun `parses N a X kg`() {
        val r = VoiceSetParser.parse("10 à 60 kg")!!
        assertEquals(10, r.reps)
        assertEquals(60f, r.weightKg, 0.01f)
    }

    @Test fun `parses N fois X`() {
        val r = VoiceSetParser.parse("8 fois 82,5")!!
        assertEquals(8, r.reps)
        assertEquals(82.5f, r.weightKg, 0.01f)
    }

    @Test fun `parses reverse form X kg N fois`() {
        val r = VoiceSetParser.parse("80 kg 10")!!
        assertEquals(10, r.reps)
        assertEquals(80f, r.weightKg, 0.01f)
    }

    @Test fun `handles decimal comma`() {
        val r = VoiceSetParser.parse("5 reps a 102,5 kilos")!!
        assertEquals(5, r.reps)
        assertEquals(102.5f, r.weightKg, 0.01f)
    }

    @Test fun `returns null on gibberish`() {
        assertNull(VoiceSetParser.parse("blablabla"))
    }
}
