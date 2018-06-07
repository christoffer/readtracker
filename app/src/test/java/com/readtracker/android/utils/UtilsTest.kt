package com.readtracker.android.utils

import com.readtracker.android.db.Session
import com.readtracker.android.support.Utils
import org.junit.Assert.*
import org.junit.Test
import java.util.*

class UtilsTest {

    /**
     * Assert that the session stop times are ordered and stored correctly.
     */
    @Test
    fun utilsTest_GetSessionStops_ReturnsFloatArray() {
        val first = Session().apply {
            endPosition = 0.2f
        }

        val second = Session().apply {
            endPosition = 0.3f
        }

        val third = Session().apply {
            endPosition = 0.4f
        }

        // Insert in non-sequential order
        val sessions = Arrays.asList(third, first, second)

        val stops = Utils.getSessionStops(sessions)

        assertEquals(0.2f, stops[0], 0.00001f)
        assertEquals(0.3f, stops[1], 0.00001f)
        assertEquals(0.4f, stops[2], 0.00001f)
    }

    /**
     * Assert the custom object equal checker that is created in the Utils class.
     * Formula used: a == b || (a != null && a.equals(b) where a, b is two Object parameters
     */
    @Test
    fun utilsTest_EqualObjectsCheck_ReturnsTrue() {
        val a = 45
        val b = 45
        val c = 50

        assertTrue(Utils.equal(a, a))
        assertTrue(Utils.equal(a, b))

        assertFalse(Utils.equal(a, c))
        assertFalse(Utils.equal(null, b))
        assertFalse(Utils.equal(a, null))
    }
}