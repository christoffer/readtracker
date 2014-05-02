package com.readtracker.android;

import com.readtracker.android.db.Session;
import com.readtracker.android.support.Utils;

import junit.framework.TestCase;

import java.util.Arrays;
import java.util.List;

public class UtilsTest extends TestCase {

  private static final long SECONDS = 1000; /* convert ms to seconds */
  private static final long MINUTES = 60 * SECONDS; /* convert ms to minutes */
  private static final long HOURS = 60 * MINUTES; /* convert ms to minutes */
  private static final long DAYS = 24 * HOURS; /* convert ms to days*/

  public void test_hoursAndMinutesFromMillis() {
    assertEquals("0 minutes", Utils.hoursAndMinutesFromMillis(0));
    assertEquals("0 minutes", Utils.hoursAndMinutesFromMillis(36 * SECONDS));
    assertEquals("1 minute", Utils.hoursAndMinutesFromMillis(87 * SECONDS));
    assertEquals("5 minutes", Utils.hoursAndMinutesFromMillis(5 * MINUTES));
    assertEquals("47 minutes", Utils.hoursAndMinutesFromMillis(47 * MINUTES + 12 * SECONDS));

    assertEquals("1 hour, 47 minutes", Utils.hoursAndMinutesFromMillis(1 * HOURS + 47 * MINUTES + 12 * SECONDS));
    assertEquals("2 hours, 47 minutes", Utils.hoursAndMinutesFromMillis(2 * HOURS + 47 * MINUTES + 12 * SECONDS));
  }

  public void test_longHumanTimeFromMillis() {
    assertEquals("1 minute", Utils.longHumanTimeFromMillis(1 * MINUTES));
    assertEquals("1 hour", Utils.longHumanTimeFromMillis(1 * HOURS));
    assertEquals("1 second", Utils.longHumanTimeFromMillis(1 * SECONDS));

    assertEquals("2 hours and 2 minutes", Utils.longHumanTimeFromMillis(2 * HOURS + 2 * MINUTES));
    assertEquals("2 hours and 2 seconds", Utils.longHumanTimeFromMillis(2 * HOURS + 2 * SECONDS));
    assertEquals("2 minutes and 2 seconds", Utils.longHumanTimeFromMillis(2 * MINUTES + 2 * SECONDS));

    assertEquals("2 hours, 2 minutes and 2 seconds", Utils.longHumanTimeFromMillis(2 * HOURS + 2 * MINUTES + 2 * SECONDS));
  }

  public void test_longCoarseHumanTimeFromMillis() {
    assertEquals("13 seconds", Utils.longCoarseHumanTimeFromMillis(13 * SECONDS));
    assertEquals("3 minutes", Utils.longCoarseHumanTimeFromMillis(3 * MINUTES + 13 * SECONDS));
    assertEquals("3 hours and 47 minutes", Utils.longCoarseHumanTimeFromMillis(3 * HOURS + 47 * MINUTES + 13 * SECONDS));
  }

  public void test_pluralizeWord() {
    assertEquals("dogs", Utils.pluralizeWord(0, "dog"));
    assertEquals("dogs", Utils.pluralizeWord(4, "dog"));
    assertEquals("dog", Utils.pluralizeWord(1, "dog"));
  }

  public void test_pluralizeWithCount() {
    assertEquals("0 dogs", Utils.pluralizeWithCount(0, "dog"));
    assertEquals("4 dogs", Utils.pluralizeWithCount(4, "dog"));
    assertEquals("1 dog", Utils.pluralizeWithCount(1, "dog"));
  }

  public void test_humanPastTimeFromTimestamp() {
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

  public void test_getSessionStops() {
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

  public void test_equal() {
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