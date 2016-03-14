package com.readtracker.android.utils;

import android.test.AndroidTestCase;

import com.readtracker.android.db.Session;
import com.readtracker.android.support.Utils;

import org.junit.Ignore;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class UtilsTest extends AndroidTestCase {

  private static final long SECONDS = 1000; /* buildAll ms to seconds */
  private static final long MINUTES = 60 * SECONDS; /* buildAll ms to minutes */
  private static final long HOURS = 60 * MINUTES; /* buildAll ms to minutes */
  private static final long DAYS = 24 * HOURS; /* buildAll ms to days*/

  /**
   * Assert the conversion of millisecond values to String representation in hours and minutes.
   */
  @Test
  public void utilsTest_HoursAndMinutesFromMillis_ReturnsString() {
    assertEquals("0 minutes", Utils.hoursAndMinutesFromMillis(0));
    assertEquals("0 minutes", Utils.hoursAndMinutesFromMillis(36 * SECONDS));
    assertEquals("1 minute", Utils.hoursAndMinutesFromMillis(87 * SECONDS));
    assertEquals("5 minutes", Utils.hoursAndMinutesFromMillis(5 * MINUTES));
    assertEquals("47 minutes", Utils.hoursAndMinutesFromMillis(47 * MINUTES + 12 * SECONDS));

    assertEquals("1 hour, 47 minutes", Utils.hoursAndMinutesFromMillis(1 * HOURS + 47 * MINUTES + 12 * SECONDS));
    assertEquals("2 hours, 47 minutes", Utils.hoursAndMinutesFromMillis(2 * HOURS + 47 * MINUTES + 12 * SECONDS));
  }

  /**
   * Assert the conversion of millisecond values to String representation of the following format:
   * x hours, y minutes and z seconds
   */
  @Test
  public void utilsTest_LongHumanTimeFromMillis_ReturnsString() {
    assertEquals("1 minute", Utils.longHumanTimeFromMillis(1 * MINUTES));
    assertEquals("1 hour", Utils.longHumanTimeFromMillis(1 * HOURS));
    assertEquals("1 second", Utils.longHumanTimeFromMillis(1 * SECONDS));

    assertEquals("2 hours and 2 minutes", Utils.longHumanTimeFromMillis(2 * HOURS + 2 * MINUTES));
    assertEquals("2 hours and 2 seconds", Utils.longHumanTimeFromMillis(2 * HOURS + 2 * SECONDS));
    assertEquals("2 minutes and 2 seconds", Utils.longHumanTimeFromMillis(2 * MINUTES + 2 * SECONDS));

    assertEquals("2 hours, 2 minutes and 2 seconds", Utils.longHumanTimeFromMillis(2 * HOURS + 2 * MINUTES + 2 * SECONDS));
  }

  /**
   * @see #utilsTest_LongHumanTimeFromMillis_ReturnsString()
   *
   * The only difference is that we drop the seconds if it is minute mark is present.
   */
  @Test
  public void utilsTest_LongCoarseHumanTimeFromMillis_ReturnsString() {
    assertEquals("13 seconds", Utils.longCoarseHumanTimeFromMillis(13 * SECONDS));
    assertEquals("3 minutes", Utils.longCoarseHumanTimeFromMillis(3 * MINUTES + 13 * SECONDS));
    assertEquals("3 hours and 47 minutes", Utils.longCoarseHumanTimeFromMillis(3 * HOURS + 47 * MINUTES + 13 * SECONDS));
  }

  /**
   * Assert the pluralization of words by giving an integer value and the singular form of a word.
   */
  @Test
  public void utilsTest_PluralizeWord_ReturnsString() {
    assertEquals("dogs", Utils.pluralizeWord(0, "dog"));
    assertEquals("dogs", Utils.pluralizeWord(4, "dog"));
    assertEquals("dog", Utils.pluralizeWord(1, "dog"));
  }

  /**
   * @see #utilsTest_PluralizeWord_ReturnsString()
   *
   * The only difference is that we return the number count in front of the noun as well.
   */
  @Test
  public void utilsTest_PluralizeWithCount_ReturnsString() {
    assertEquals("0 dogs", Utils.pluralizeWithCount(0, "dog"));
    assertEquals("4 dogs", Utils.pluralizeWithCount(4, "dog"));
    assertEquals("1 dog", Utils.pluralizeWithCount(1, "dog"));
  }

  /**
   * Assert the string representation of the past time from the current time,
   * using different millisecond values to call the helper class.
   */
  @Test
  public void utilsTest_HumanPastTimeFromTimestamp_ReturnsString() {
    final long now = 1009886564000L; /* some date at around noon */
    final long millisPast22November1981 = now - 375254055000L;

    HumanPastTimeHelper humanPastTimeHelper = new HumanPastTimeHelper(now);

    assertEquals("earlier today", humanPastTimeHelper.call(2 * HOURS));
    assertEquals("yesterday", humanPastTimeHelper.call(28 * HOURS));
    assertEquals("two days ago", humanPastTimeHelper.call(50 * HOURS));
    assertEquals("three days ago", humanPastTimeHelper.call(68 * HOURS));
    assertEquals("about a week ago", humanPastTimeHelper.call(5 * DAYS));
    assertEquals("about two weeks ago", humanPastTimeHelper.call(11 * DAYS));
    assertEquals("about three weeks ago", humanPastTimeHelper.call(19 * DAYS));
    assertEquals("about a month ago", humanPastTimeHelper.call(29 * DAYS));
    assertEquals("on Nov 22, 1981", humanPastTimeHelper.call(millisPast22November1981));
  }

  /**
   * Assert that the session stop times are ordered and stored correctly.
   */
  @Test
  public void utilsTest_GetSessionStops_ReturnsFloatArray() {
    Session first = new Session() {{
      setEndPosition(0.2f);
    }};
    Session second = new Session() {{
      setEndPosition(0.3f);
    }};
    Session third = new Session() {{
      setEndPosition(0.4f);
    }};

    List<Session> sessions = Arrays.asList(third, first, second);

    float[] stops = Utils.getSessionStops(sessions);

    assertEquals(0.2f, stops[0], 0.00001f);
    assertEquals(0.3f, stops[1], 0.00001f);
    assertEquals(0.4f, stops[2], 0.00001f);
  }

  /**
   * Assert the custom object equal checker that is created in the Utils class.
   * Formula used: a == b || (a != null && a.equals(b) where a, b is two Object parameters
   */
  @Test
  public void utilsTest_EqualObjectsCheck_ReturnsTrue() {
    Object a = new Integer(45);
    Object b = new Integer(45);
    Object c = new Integer(50);

    assertTrue(Utils.equal(a, a));
    assertTrue(Utils.equal(a, b));

    assertFalse(Utils.equal(a, c));
    assertFalse(Utils.equal(null, b));
    assertFalse(Utils.equal(a, null));
  }

  /** Small helper class for calling humanPastTimeFromTimestamp succinctly . */
  private static class HumanPastTimeHelper {
    final long now;

    public HumanPastTimeHelper(long now) {
      this.now = now;
    }

    /**
     * Returns Utils.humanPastTimeFromTimestamp(now - millisecondsAgo, now);
     */
    public String call(long millisecondsAgo) {
      return Utils.humanPastTimeFromTimestamp(now - millisecondsAgo, now);
    }
  }
}