package com.lucanicoletti.wristotp.otp

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNull

class Base32Test {

    @Test
    fun decodesRfcSeed() {
        assertContentEquals("12345678901234567890".encodeToByteArray(), Base32.decode("GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ"))
    }

    @Test
    fun ignoresCaseSpacesAndPadding() {
        assertContentEquals("f".encodeToByteArray(), Base32.decode("my======"))
        assertContentEquals("12345678901234567890".encodeToByteArray(), Base32.decode("gezd gnbv gy3t qojq gezd gnbv gy3t qojq"))
    }

    @Test
    fun rejectsInvalidCharacters() {
        assertNull(Base32.decode("GEZD1NBV"))
    }

    @Test
    fun encodesWithoutPadding() {
        assertEquals("GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ", Base32.encode("12345678901234567890".encodeToByteArray()))
        assertEquals("MY", Base32.encode("f".encodeToByteArray()))
    }

    @Test
    fun roundTripsEveryLength() {
        for (length in 0..40) {
            val bytes = ByteArray(length) { (it * 37 + 11).toByte() }
            assertContentEquals(bytes, Base32.decode(Base32.encode(bytes)))
        }
    }
}
