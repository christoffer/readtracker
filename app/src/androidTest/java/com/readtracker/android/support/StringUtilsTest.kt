package com.readtracker.android.support

import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.support.test.InstrumentationRegistry.getTargetContext
import com.readtracker.android.support.StringUtils.*
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.*

const val SECONDS = 1000L
const val MINUTES = 60 * SECONDS
const val HOURS = 60 * MINUTES
const val DAYS = 24 * HOURS

class StringUtilsTest {

    private fun getContextWithLocale(context: Context, language: String, country: String): ContextWrapper {
        val locale = Locale(language, country)
        Locale.setDefault(locale)
        val res = context.resources
        val config = res.configuration
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocale(locale)
        } else {
            @Suppress("DEPRECATION")
            config.locale = locale
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
            @Suppress("DEPRECATION")
            res.updateConfiguration(config, res.displayMetrics)
            return ContextWrapper(context)
        }
        return ContextWrapper(context.createConfigurationContext(config))
    }

    /**
     * Assert the string representation of the past time from the current time,
     * using different millisecond values to call the helper class.
     */
    @Test
    fun utilsTest_HumanPastTimeFromTimestamp_ReturnsString() {
        val now = 1009886564000L /* some date at around noon */
        val millisPast22November1981 = now - 375254055000L

        val context = getContextWithLocale(getTargetContext(), "en", "EN")

        fun getStringForOffsetInPast(offsetMilliSeconds: Long): String {
            return StringUtils.humanPastTimeFromTimestamp(now - offsetMilliSeconds, now, context)
        }

        assertEquals("earlier today", getStringForOffsetInPast(2 * HOURS))
        assertEquals("yesterday", getStringForOffsetInPast(28 * HOURS))
        assertEquals("two days ago", getStringForOffsetInPast(50 * HOURS))
        assertEquals("three days ago", getStringForOffsetInPast(68 * HOURS))
        assertEquals("about a week ago", getStringForOffsetInPast(5 * DAYS))
        assertEquals("about two weeks ago", getStringForOffsetInPast(11 * DAYS))
        assertEquals("about three weeks ago", getStringForOffsetInPast(19 * DAYS))
        assertEquals("about a month ago", getStringForOffsetInPast(29 * DAYS))
        assertEquals("on Nov 22, 1981", getStringForOffsetInPast(millisPast22November1981))
    }

    @Test
    fun getDateString_returns_localized_dates() {
        val nov22nd1981 = 375292786000L

        val englishLocale = getContextWithLocale(getTargetContext(), "en", "EN")
        assertEquals("on Nov 22, 1981", StringUtils.getDateString(nov22nd1981, englishLocale))

        val swedishLocale = getContextWithLocale(getTargetContext(), "sv", "SE")
        val expectedResult = if (Build.VERSION.SDK_INT <= 22) {
            // NOTE(christoffer) The formatting seems to have changed around this time, not sure
            // exactly when but at least it's different between 22 and 25
            "on 22 nov 1981"
        } else {
            "on 22 nov. 1981"
        }
        assertEquals(expectedResult, StringUtils.getDateString(nov22nd1981, swedishLocale))
    }
    /**
     * Assert the conversion of millisecond values to String representation in hours and minutes.
     */
    @Test
    fun utilsTest_HoursAndMinutesFromMillis_ReturnsString() {
        val context = getTargetContext()
        assertEquals("0 minutes", hoursAndMinutesFromMillis(0, context))
        assertEquals("0 minutes", hoursAndMinutesFromMillis(36 * SECONDS, context))
        assertEquals("1 minute", hoursAndMinutesFromMillis(87 * SECONDS, context))
        assertEquals("5 minutes", hoursAndMinutesFromMillis(5 * MINUTES, context))
        assertEquals("47 minutes", hoursAndMinutesFromMillis(47 * MINUTES + 12 * SECONDS, context))

        assertEquals("1 hour, 47 minutes", hoursAndMinutesFromMillis(1 * HOURS + 47 * MINUTES + 12 * SECONDS, context))
        assertEquals("2 hours, 47 minutes", hoursAndMinutesFromMillis(2 * HOURS + 47 * MINUTES + 12 * SECONDS, context))
    }

    /**
     * Assert the conversion of millisecond values to String representation of the following format:
     * x hours, y minutes and z seconds
     */
    @Test
    fun utilsTest_LongHumanTimeFromMillis_ReturnsString() {
        val context = getTargetContext()
        assertEquals("1 minute", longHumanTimeFromMillis(MINUTES, context))
        assertEquals("1 hour", longHumanTimeFromMillis(HOURS, context))
        assertEquals("1 second", longHumanTimeFromMillis(SECONDS, context))

        assertEquals("2 hours and 2 minutes", longHumanTimeFromMillis(2 * HOURS + 2 * MINUTES, context))
        assertEquals("2 hours and 2 seconds", longHumanTimeFromMillis(2 * HOURS + 2 * SECONDS, context))
        assertEquals("2 minutes and 2 seconds", longHumanTimeFromMillis(2 * MINUTES + 2 * SECONDS, context))

        assertEquals("2 hours, 2 minutes and 2 seconds", longHumanTimeFromMillis(2 * HOURS + 2 * MINUTES + 2 * SECONDS, context))
    }

    /**
     * @see [utilsTest_LongHumanTimeFromMillis_ReturnsString]
     */
    @Test
    fun utilsTest_LongCoarseHumanTimeFromMillis_ReturnsString() {
        val context = getTargetContext()
        assertEquals("13 seconds", longCoarseHumanTimeFromMillis(13 * SECONDS, context))
        assertEquals("3 minutes", longCoarseHumanTimeFromMillis(3 * MINUTES + 13 * SECONDS, context))
        assertEquals("3 hours and 47 minutes", longCoarseHumanTimeFromMillis(3 * HOURS + 47 * MINUTES + 13 * SECONDS, context))
    }
}