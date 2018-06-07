package com.readtracker.android.utils

import com.readtracker.android.db.Session
import com.readtracker.android.support.Utils
import org.junit.Assert.*
import org.junit.Test
import java.util.*

class UtilsTest {

    /**
     * Assert the pluralization of words by giving an integer value and the singular form of a word.
     */
    @Test
    fun utilsTest_PluralizeWord_ReturnsString() {
        assertEquals("dogs", Utils.pluralizeWord(0, "dog"))
        assertEquals("dogs", Utils.pluralizeWord(4, "dog"))
        assertEquals("dog", Utils.pluralizeWord(1, "dog"))

        assertEquals("kisses", Utils.pluralizeWord(0, "kiss"))
        assertEquals("kisses", Utils.pluralizeWord(4, "kiss"))
        assertEquals("kiss", Utils.pluralizeWord(1, "kiss"))
    }

    /**
     * @see .utilsTest_PluralizeWord_ReturnsString
     */
    @Test
    fun utilsTest_PluralizeWithCount_ReturnsString() {
        assertEquals("0 dogs", Utils.pluralizeWithCount(0, "dog"))
        assertEquals("4 dogs", Utils.pluralizeWithCount(4, "dog"))
        assertEquals("1 dog", Utils.pluralizeWithCount(1, "dog"))
    }

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