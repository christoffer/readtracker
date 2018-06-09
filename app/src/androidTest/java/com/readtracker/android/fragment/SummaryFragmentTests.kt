package com.readtracker.android.fragment

import android.support.test.InstrumentationRegistry.getTargetContext
import com.readtracker.android.fragments.SummaryFragment.getPepTalkString
import com.readtracker.android.integration_test_utils.getContextWithLocale
import junit.framework.Assert
import junit.framework.TestCase.*
import org.junit.Test

class SummmaryFragmentTests {
    @Test
    fun getPepTalkString_returnsExpectedResults() {
        val context = getContextWithLocale(getTargetContext(), "en", "EN")
        fun toSeconds(hours: Int, minutes: Int): Float = (hours * 60 * 60 + minutes * 60).toFloat()
        assertNull(getPepTalkString(context, toSeconds(hours=0, minutes=0)))
        assertEquals("Why not finish it today?", getPepTalkString(context, toSeconds(hours=0, minutes=37)))

        // 2h = 120 minutes = 40 minutes per day for three days
        assertEquals("That's about 40 minutes per day to finish it in three days.", getPepTalkString(context, toSeconds(hours=2, minutes=0)))
        // 2h:45 = 165 minutes = 55 minutes per day for three days
        assertEquals("That's about 55 minutes per day to finish it in three days.", getPepTalkString(context, toSeconds(hours=2, minutes=45)))

        // 7h = 1h / day for a week
        assertEquals("That's about 1 hour per day to finish it in a week.", getPepTalkString(context, toSeconds(hours=7, minutes=0)))
        // 8:42 = 522 minutes ~ 1:14 per day for a week
        assertEquals("That's about 1 hour and 14 minutes per day to finish it in a week.", getPepTalkString(context, toSeconds(hours=8, minutes=42)))

        assertEquals("That's about 1 hour per day to finish it in two weeks.", getPepTalkString(context, toSeconds(hours=14, minutes=0)))
        // 16:42 = 1002 minutes / 14 ~ 71 ~ 1:11
        assertEquals("That's about 1 hour and 11 minutes per day to finish it in two weeks.", getPepTalkString(context, toSeconds(hours=16, minutes=42)))

        assertNull(getPepTalkString(context, toSeconds(hours=21, minutes=0)))
    }
}