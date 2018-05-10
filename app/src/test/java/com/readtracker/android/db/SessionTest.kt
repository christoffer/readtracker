package com.readtracker.android.db

import android.support.test.filters.SmallTest
import org.junit.Assert.assertEquals
import org.junit.Test

class SessionTest {

    /**
     * Create a [Book] entity and set a random [Session] on it. Merges this
     * information onto another book entity and assert the session.
     */
    @SmallTest
    @Test
    fun sessionTest_MergeInformationFromSessionToSession_ReturnsSessionCopy() {
        val original = Session().apply {
            durationSeconds = 123
            timestampMs = 123456789L
            startPosition = 0.25f
            endPosition = 0.75f
        }

        val merged = Session().apply {
            merge(original)
        }

        assertEquals(123, merged.durationSeconds)
        assertEquals(123456789L, merged.timestampMs)
        assertEquals(0.25f, merged.startPosition)
        assertEquals(0.75f, merged.endPosition)
    }

}