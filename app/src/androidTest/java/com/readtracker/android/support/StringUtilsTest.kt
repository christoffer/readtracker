package com.readtracker.android.support

import android.os.Build
import androidx.test.platform.app.InstrumentationRegistry.getTargetContext
import com.readtracker.android.db.Book
import com.readtracker.android.db.Session
import com.readtracker.android.integration_test_utils.getContextWithLocale
import com.readtracker.android.support.StringUtils.*
import org.junit.Assert.assertEquals
import org.junit.Test

const val SECONDS = 1000L
const val MINUTES = 60 * SECONDS
const val HOURS = 60 * MINUTES
const val DAYS = 24 * HOURS

class StringUtilsTest {

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
    fun hoursAndMinutesFromMillis_returnsExpectedStrings() {
        val context = getContextWithLocale(getTargetContext(), "en", "EN")
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
    fun longHumanTimeFromMillis_returnsExpectedString() {
        val context = getContextWithLocale(getTargetContext(), "en", "EN")
        assertEquals("1 minute", longHumanTimeFromMillis(MINUTES, context))
        assertEquals("1 hour", longHumanTimeFromMillis(HOURS, context))
        assertEquals("1 second", longHumanTimeFromMillis(SECONDS, context))

        assertEquals("2 hours and 2 minutes", longHumanTimeFromMillis(2 * HOURS + 2 * MINUTES, context))
        assertEquals("2 hours and 2 seconds", longHumanTimeFromMillis(2 * HOURS + 2 * SECONDS, context))
        assertEquals("2 minutes and 2 seconds", longHumanTimeFromMillis(2 * MINUTES + 2 * SECONDS, context))

        assertEquals("2 hours, 2 minutes and 2 seconds", longHumanTimeFromMillis(2 * HOURS + 2 * MINUTES + 2 * SECONDS, context))
    }

    @Test
    fun utilsTest_LongCoarseHumanTimeFromMillis_ReturnsString() {
        val context = getTargetContext()
        assertEquals("13 seconds", longCoarseHumanTimeFromMillis(13 * SECONDS, context))
        assertEquals("3 minutes", longCoarseHumanTimeFromMillis(3 * MINUTES + 13 * SECONDS, context))
        assertEquals("3 hours and 47 minutes", longCoarseHumanTimeFromMillis(3 * HOURS + 47 * MINUTES + 13 * SECONDS, context))
    }


    @Test
    fun StringUtils_test_formatSessionReadAmountHtml() {
        val context = getContextWithLocale(getTargetContext(), "en", "EN")

        val noBookNoPages = Session().apply { startPosition = 0.381f; endPosition = 0.422f }
        assertEquals("<b>4.1%</b>", formatSessionReadAmountHtml(context, noBookNoPages))

        val noPageBook = Book().apply { pageCount = null }
        val sessionNoPageBook = Session().apply {
            book = noPageBook
            startPosition = 0.381f
            endPosition = 0.422f
        }
        assertEquals("<b>4.1%</b>", formatSessionReadAmountHtml(context, sessionNoPageBook))

        val pageBook = Book().apply { pageCount = 100f }
        val sessionPageBook = Session().apply {
            book = pageBook
            startPosition = 0.381f
            endPosition = 0.422f
        }
        assertEquals("<b>4</b> pages", formatSessionReadAmountHtml(context, sessionPageBook))
    }


    @Test
    fun StringUtils_test_formatSessionDurationHtml() {
        val context = getContextWithLocale(getTargetContext(), "en", "EN")

        val hoursAndMinutes = Session().apply { durationSeconds = (2 * HOURS + 43 * MINUTES + 15 * SECONDS) / 1000 }
        assertEquals("<b>2</b>h, <b>43</b>min", formatSessionDurationHtml(context, hoursAndMinutes))

        val onlyMinutes = Session().apply { durationSeconds = (14 * MINUTES + 35 * SECONDS) / 1000 }
        assertEquals("<b>14</b> minutes", formatSessionDurationHtml(context, onlyMinutes))

        val onlyMinute = Session().apply { durationSeconds = (1 * MINUTES + 35 * SECONDS) / 1000 }
        assertEquals("<b>1</b> minute", formatSessionDurationHtml(context, onlyMinute))
    }


    @Test
    fun StringUtils_test_formatSessionFromTo() {
        val context = getContextWithLocale(getTargetContext(), "en", "EN")

        val pctSession = Session().apply { startPosition = 0.113f; endPosition = 0.156f }
        assertEquals("11.3 - 15.6%", formatSessionFromTo(context, pctSession))

        val pagesBook = Book().apply { pageCount = 200f }
        val pageSession = Session().apply {
            startPosition = 0.113f
            endPosition = 0.156f
            book = pagesBook
        }
        assertEquals("p. 22 - 31", formatSessionFromTo(context, pageSession))

//        val onlyMinutes = Session().apply { durationSeconds = (14 * MINUTES + 35 * SECONDS) / 1000 }
//        assertEquals("<b>14</b> minutes", formatSessionDurationHtml(context, onlyMinutes))
//
//        val onlyMinute = Session().apply { durationSeconds = (1 * MINUTES + 35 * SECONDS) / 1000 }
//        assertEquals("<b>1</b> minute", formatSessionDurationHtml(context, onlyMinute))
    }
}
