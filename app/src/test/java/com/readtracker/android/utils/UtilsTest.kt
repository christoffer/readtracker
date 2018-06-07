package com.readtracker.android.utils

import android.content.Context
import com.readtracker.android.db.Session
import com.readtracker.android.support.StringUtils
import com.readtracker.android.support.Utils
import org.junit.Assert.*
import org.junit.Test
import java.util.*

const val SECONDS: Long = 1000
const val MINUTES: Long = 60 * SECONDS
const val HOURS: Long = 60 * MINUTES

class UtilsTest {
    /**
     * Assert the conversion of millisecond values to String representation in hours and minutes.
     */
    @Test
    fun utilsTest_HoursAndMinutesFromMillis_ReturnsString() {
        assertEquals("0 minutes", Utils.hoursAndMinutesFromMillis(0))
        assertEquals("0 minutes", Utils.hoursAndMinutesFromMillis(36 * SECONDS))
        assertEquals("1 minute", Utils.hoursAndMinutesFromMillis(87 * SECONDS))
        assertEquals("5 minutes", Utils.hoursAndMinutesFromMillis(5 * MINUTES))
        assertEquals("47 minutes", Utils.hoursAndMinutesFromMillis(47 * MINUTES + 12 * SECONDS))

        assertEquals("1 hour, 47 minutes", Utils.hoursAndMinutesFromMillis(1 * HOURS + 47 * MINUTES + 12 * SECONDS))
        assertEquals("2 hours, 47 minutes", Utils.hoursAndMinutesFromMillis(2 * HOURS + 47 * MINUTES + 12 * SECONDS))
    }

    /**
     * Assert the conversion of millisecond values to String representation of the following format:
     * x hours, y minutes and z seconds
     */
    @Test
    fun utilsTest_LongHumanTimeFromMillis_ReturnsString() {
        assertEquals("1 minute", Utils.longHumanTimeFromMillis(MINUTES))
        assertEquals("1 hour", Utils.longHumanTimeFromMillis(HOURS))
        assertEquals("1 second", Utils.longHumanTimeFromMillis(SECONDS))

        assertEquals("2 hours and 2 minutes", Utils.longHumanTimeFromMillis(2 * HOURS + 2 * MINUTES))
        assertEquals("2 hours and 2 seconds", Utils.longHumanTimeFromMillis(2 * HOURS + 2 * SECONDS))
        assertEquals("2 minutes and 2 seconds", Utils.longHumanTimeFromMillis(2 * MINUTES + 2 * SECONDS))

        assertEquals("2 hours, 2 minutes and 2 seconds", Utils.longHumanTimeFromMillis(2 * HOURS + 2 * MINUTES + 2 * SECONDS))
    }

    /**
     * @see [utilsTest_LongHumanTimeFromMillis_ReturnsString]
     */
    @Test
    fun utilsTest_LongCoarseHumanTimeFromMillis_ReturnsString() {
        assertEquals("13 seconds", Utils.longCoarseHumanTimeFromMillis(13 * SECONDS))
        assertEquals("3 minutes", Utils.longCoarseHumanTimeFromMillis(3 * MINUTES + 13 * SECONDS))
        assertEquals("3 hours and 47 minutes", Utils.longCoarseHumanTimeFromMillis(3 * HOURS + 47 * MINUTES + 13 * SECONDS))
    }

    /**
     * Assert the pluralization of words by giving an integer value and the singular form of a word.
     */
    @Test
    fun utilsTest_PluralizeWord_ReturnsString() {
        assertEquals("dogs", Utils.pluralizeWord(0, "dog"))
        assertEquals("dogs", Utils.pluralizeWord(4, "dog"))
        assertEquals("dog", Utils.pluralizeWord(1, "dog"))
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