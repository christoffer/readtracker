package com.readtracker_beta.tests;

import com.readtracker_beta.support.Utils;
import org.junit.Test;

import java.util.Date;

import static org.junit.Assert.assertEquals;

public class UtilsTest {

  private static final long DAYS = 60 * 60 * 24 * 1000;

  private long _ms(long seconds) { return seconds * 1000; }

  private long _ms(long minutes, long seconds) {
    return _ms(minutes * 60 + seconds);
  }

  private long _ms(long hours, long minutes, long seconds) {
    return _ms(hours * 60 + minutes, seconds);
  }

  @Test
  public void testHoursAndMinutesFromMillis() {
    assertEquals("0 minutes", Utils.hoursAndMinutesFromMillis(0));
    assertEquals("59 minutes", Utils.hoursAndMinutesFromMillis(_ms(59, 0)));

    assertEquals("1 hour, 0 minutes", Utils.hoursAndMinutesFromMillis(_ms(1, 0, 0)));
    assertEquals("1 hour, 0 minutes", Utils.hoursAndMinutesFromMillis(_ms(1, 0, 1)));
    assertEquals("2 hours, 1 minute", Utils.hoursAndMinutesFromMillis(_ms(2, 1, 0)));
  }

  @Test
  public void testShortHumanTimeFromMillis() {

    assertEquals("0s", Utils.shortHumanTimeFromMillis(0));
    assertEquals("59s", Utils.shortHumanTimeFromMillis(_ms(59)));

    assertEquals("1m 0s", Utils.shortHumanTimeFromMillis(_ms(1, 0)));
    assertEquals("1m 1s", Utils.shortHumanTimeFromMillis(_ms(1, 1)));

    assertEquals("1h 0m 0s", Utils.shortHumanTimeFromMillis(_ms(1, 0, 0)));
    assertEquals("1h 0m 1s", Utils.shortHumanTimeFromMillis(_ms(1, 0, 1)));
    assertEquals("1h 1m 0s", Utils.shortHumanTimeFromMillis(_ms(1, 1, 0)));

  }

  @Test
  public void testLongHumanTimeFromMillis() {

    assertEquals("0 seconds", Utils.longHumanTimeFromMillis(_ms(0)));
    assertEquals("1 second", Utils.longHumanTimeFromMillis(_ms(1)));
    assertEquals("2 seconds", Utils.longHumanTimeFromMillis(_ms(2)));

    assertEquals("1 minute and 2 seconds", Utils.longHumanTimeFromMillis(_ms(1, 2)));
    assertEquals("2 minutes and 2 seconds", Utils.longHumanTimeFromMillis(_ms(2, 2)));
    assertEquals("2 minutes", Utils.longHumanTimeFromMillis(_ms(2, 0)));

    assertEquals("1 hour and 2 minutes", Utils.longHumanTimeFromMillis(_ms(1, 2, 0)));
    assertEquals("2 hours and 2 minutes", Utils.longHumanTimeFromMillis(_ms(2, 2, 0)));

    assertEquals("2 hours, 49 minutes and 12 seconds", Utils.longHumanTimeFromMillis(_ms(2, 49, 12)));
    assertEquals("2 hours and 12 seconds", Utils.longHumanTimeFromMillis(_ms(2, 0, 12)));

  }

  @Test
  public void testToSentence() {
    assertEquals("", Utils.toSentence(new String[] { }));
    assertEquals("a", Utils.toSentence(new String[] { "a" }));
    assertEquals("a and b", Utils.toSentence(new String[] { "a", "b" }));
    assertEquals("a, b and c", Utils.toSentence(new String[] { "a", "b", "c" }));
    assertEquals("a, b, c and d", Utils.toSentence(new String[] { "a", "b", "c", "d" }));
  }

  @Test
  public void testGetXFromMillis() {
    assertEquals(0, Utils.getHoursFromMillis(_ms(0)));
    assertEquals(1, Utils.getHoursFromMillis(_ms(1, 0, 0)));
    assertEquals(1, Utils.getHoursFromMillis(_ms(1, 59, 59)));
    assertEquals(2, Utils.getHoursFromMillis(_ms(2, 0, 0)));

    assertEquals(0, Utils.getMinutesFromMillis(_ms(0)));
    assertEquals(1, Utils.getMinutesFromMillis(_ms(0, 1, 0)));
    assertEquals(1, Utils.getMinutesFromMillis(_ms(1, 1, 0)));
    assertEquals(1, Utils.getMinutesFromMillis(_ms(1, 1, 59)));
    assertEquals(59, Utils.getMinutesFromMillis(_ms(1, 59, 59)));

    assertEquals(0, Utils.getSecondsFromMillis(_ms(0)));
    assertEquals(1, Utils.getSecondsFromMillis(_ms(1)));
    assertEquals(1, Utils.getSecondsFromMillis(_ms(1, 1, 1)));
    assertEquals(59, Utils.getSecondsFromMillis(_ms(1, 59, 59)));
  }

  /** Convert hours and minutes into Date */
  private Date _datetime(int hours, int minutes) {
    return new Date(2012, 2, 2, hours, 0);
  }

  @Test
  public void testHumanTimeOfDay() {
    assertEquals("at night", Utils.humanTimeOfDay(_datetime(23, 0)));
    assertEquals("at night", Utils.humanTimeOfDay(_datetime(0, 0)));
    assertEquals("at night", Utils.humanTimeOfDay(_datetime(0, 1)));
    assertEquals("at night", Utils.humanTimeOfDay(_datetime(3, 59)));

    assertEquals("in the morning", Utils.humanTimeOfDay(_datetime(4, 0)));
    assertEquals("in the morning", Utils.humanTimeOfDay(_datetime(8, 59)));

    assertEquals("midmorning", Utils.humanTimeOfDay(_datetime(9, 0)));
    assertEquals("midmorning", Utils.humanTimeOfDay(_datetime(10, 59)));

    assertEquals("around noon", Utils.humanTimeOfDay(_datetime(11, 0)));
    assertEquals("around noon", Utils.humanTimeOfDay(_datetime(12, 59)));

    assertEquals("in the afternoon", Utils.humanTimeOfDay(_datetime(13, 0)));
    assertEquals("in the afternoon", Utils.humanTimeOfDay(_datetime(15, 59)));

    assertEquals("in the late afternoon", Utils.humanTimeOfDay(_datetime(16, 0)));
    assertEquals("in the late afternoon", Utils.humanTimeOfDay(_datetime(18, 59)));

    assertEquals("in the evening", Utils.humanTimeOfDay(_datetime(19, 0)));
    assertEquals("in the evening", Utils.humanTimeOfDay(_datetime(22, 59)));
  }

  private Date _createDate(long secondDifference) {
    return new Date(new Date().getTime() + secondDifference);
  }

  @Test
  public void testHumanPastDate() {
    final int MAY = 5 - 1; // stupid java date
    Date now = new Date(2011 - 1900, MAY, 12, 21, 45, 16);
    Date future = new Date(now.getTime() + 1);

    assertEquals("", Utils.humanPastDate(now, future));

    Date earlierToday = new Date(2011 - 1900, MAY, 12, 8, 45, 16);
    assertEquals("earlier today", Utils.humanPastDate(now, earlierToday));

    Date justAfterMidnight = new Date(2011 - 1900, MAY, 12, 0, 0, 1);
    assertEquals("earlier today", Utils.humanPastDate(now, justAfterMidnight));

    Date justBeforeMidnight = new Date(2011 - 1900, MAY, 11, 23, 59, 59);
    assertEquals("yesterday", Utils.humanPastDate(now, justBeforeMidnight));

    Date justAfterYesterdayMidnight = new Date(2011 - 1900, MAY, 11, 0, 0, 1);
    assertEquals("yesterday", Utils.humanPastDate(now, justAfterYesterdayMidnight));

    Date dayBeforeYesterday = new Date(2011 - 1900, MAY, 10, 15, 23, 23);
    assertEquals("two days ago", Utils.humanPastDate(now, dayBeforeYesterday));

    Date threeDaysAgo = new Date(now.getTime() - 3 * DAYS);
    assertEquals("three days ago", Utils.humanPastDate(now, threeDaysAgo));

    Date fourDaysAgo = new Date(now.getTime() - 4 * DAYS);
    assertEquals("about a week ago", Utils.humanPastDate(now, fourDaysAgo));

    Date nineDaysAgo = new Date(now.getTime() - 9 * DAYS);
    assertEquals("about a week ago", Utils.humanPastDate(now, nineDaysAgo));

    Date tenDaysAgo = new Date(now.getTime() - 10 * DAYS);
    assertEquals("about two weeks ago", Utils.humanPastDate(now, tenDaysAgo));

    Date eighteenDaysAgo = new Date(now.getTime() - 18 * DAYS);
    assertEquals("about two weeks ago", Utils.humanPastDate(now, eighteenDaysAgo));

    Date nineteenDaysAgo = new Date(now.getTime() - 19 * DAYS);
    assertEquals("about three weeks ago", Utils.humanPastDate(now, nineteenDaysAgo));

    Date twentyFourDaysAgo = new Date(now.getTime() - 24 * DAYS);
    assertEquals("about three weeks ago", Utils.humanPastDate(now, twentyFourDaysAgo));

    Date twentyFiveDaysAgo = new Date(now.getTime() - 25 * DAYS);
    assertEquals("about a month ago", Utils.humanPastDate(now, twentyFiveDaysAgo));

    Date fourtyFiveDaysAgo = new Date(now.getTime() - 44 * DAYS);
    assertEquals("about a month ago", Utils.humanPastDate(now, fourtyFiveDaysAgo));

    Date aWhileAgo = new Date(2009 - 1900, 2, 3, 14, 3, 43);
    assertEquals("on Mar 3, 2009", Utils.humanPastDate(now, aWhileAgo));

    Date anotherWhileAgo = new Date(2010 - 1900, 11, 11, 11, 11, 11);
    assertEquals("on Dec 11, 2010", Utils.humanPastDate(now, anotherWhileAgo));
  }

}
