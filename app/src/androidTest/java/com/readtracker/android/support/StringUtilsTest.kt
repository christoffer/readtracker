package com.readtracker.android.support

import android.content.Context
import android.support.test.InstrumentationRegistry.getTargetContext
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.*

const val MS_IN_AN_HOUR = 60 * 60 * 1000L
const val MS_IN_A_DAY = 24 * MS_IN_AN_HOUR

class StringUtilsTest {

    @Suppress("DEPRECATION")
    private fun setLocale(context: Context, language: String, country: String) {
        val locale = Locale(language, country)
        // here we update locale for date formatters
        Locale.setDefault(locale)
        // here we update locale for app resources
        val res = context.resources
        val config = res.configuration
        config.locale = locale
        res.updateConfiguration(config, res.getDisplayMetrics())
    }

    /**
     * Assert the string representation of the past time from the current time,
     * using different millisecond values to call the helper class.
     */
    @Test
    fun utilsTest_HumanPastTimeFromTimestamp_ReturnsString() {
        val now = 1009886564000L /* some date at around noon */
        val millisPast22November1981 = now - 375254055000L

        val context = getTargetContext()
        setLocale(context, "en", "EN")

        fun getStringForOffsetInPast(offsetMilliSeconds: Long): String {
            return StringUtils.humanPastTimeFromTimestamp(now - offsetMilliSeconds, now, context)
        }

        assertEquals("earlier today", getStringForOffsetInPast(2 * MS_IN_AN_HOUR))
        assertEquals("yesterday", getStringForOffsetInPast(28 * MS_IN_AN_HOUR))
        assertEquals("two days ago", getStringForOffsetInPast(50 * MS_IN_AN_HOUR))
        assertEquals("three days ago", getStringForOffsetInPast(68 * MS_IN_AN_HOUR))
        assertEquals("about a week ago", getStringForOffsetInPast(5 * MS_IN_A_DAY))
        assertEquals("about two weeks ago", getStringForOffsetInPast(11 * MS_IN_A_DAY))
        assertEquals("about three weeks ago", getStringForOffsetInPast(19 * MS_IN_A_DAY))
        assertEquals("about a month ago", getStringForOffsetInPast(29 * MS_IN_A_DAY))
        assertEquals("on Nov 22, 1981", getStringForOffsetInPast(millisPast22November1981))
    }

    @Test
    fun getDateString_returns_localized_dates() {
        val context = getTargetContext()
        val nov22nd1981 = 375292786000L

        setLocale(context, "en", "EN")
        assertEquals("on Nov 22, 1981", StringUtils.getDateString(nov22nd1981, context))
        setLocale(context, "sv", "SE")
        assertEquals("on 22 nov. 1981", StringUtils.getDateString(nov22nd1981, context))
    }
}