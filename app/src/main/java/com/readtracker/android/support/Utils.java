package com.readtracker.android.support;


import android.graphics.Color;

import com.readtracker.android.db.Book;
import com.readtracker.android.db.Session;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Locale;

/**
 * Generic utility functions
 */
public class Utils {
  private static final long DAYS = 60 * 60 * 24 * 1000;

  /**
   * Returns a string representation like "3 hours, 12 minutes"
   *
   * @param duration the duration to represent
   * @return the duration formatted as full hours and minutes
   */
  public static String hoursAndMinutesFromMillis(long duration) {
    int[] hms = bucketMilliseconds(duration);
    int hours = hms[0];
    int minutes = hms[1];

    if(hours == 0) {
      return pluralizeWithCount(minutes, "minute");
    }

    return String.format("%s, %s",
        pluralizeWithCount(hours, "hour"),
        pluralizeWithCount(minutes, "minute")
    );
  }

  /**
   * Returns a duration as x hours, y minutes and z seconds.
   * Parts that are 0 are left out.
   * For example:
   * 3 hours and 12 seconds.
   */
  public static String longHumanTimeFromMillis(long durationMillis) {
    int[] hms = bucketMilliseconds(durationMillis);

    int hours = hms[0];
    int minutes = hms[1];
    int seconds = hms[2];

    ArrayList<String> parts = new ArrayList<String>(3);

    if(hours > 0) parts.add(pluralizeWithCount(hours, "hour"));
    if(minutes > 0) parts.add(pluralizeWithCount(minutes, "minute"));
    if(seconds > 0 || parts.size() == 0)
      parts.add(pluralizeWithCount(seconds, "second"));

    return toSentence(parts.toArray(new String[parts.size()]));
  }

  /**
   * @see #longCoarseHumanTimeFromMillis(long)
   */
  public static String longCoarseHumanTimeFromSeconds(long seconds) {
    return longCoarseHumanTimeFromMillis(seconds * 1000);
  }

  /**
   * Returns a string describing a duration in matter of hours and minutes.
   */
  public static String longCoarseHumanTimeFromMillis(long durationMillis) {
    long durationSeconds = durationMillis / 1000;
    if(durationSeconds < 60) {
      return longHumanTimeFromMillis(durationMillis);
    }
    durationSeconds = (durationSeconds / 60) * 60;
    return longHumanTimeFromMillis(durationSeconds * 1000);
  }

  /**
   * Returns the (english) pluralization of a word.
   * e.g. "dog" vs. "dogs"
   */
  public static String pluralizeWord(int num, String noun) {
    return num == 1 ? noun : noun + (noun.endsWith("s") ? "es" : "s");
  }

  /**
   * Returns the count and pluralized version of a word.
   * e.g. "1 dog" vs. "3 dogs"
   */
  public static String pluralizeWithCount(int num, String noun) {
    return String.format("%d %s", num, pluralizeWord(num, noun));
  }

  /**
   * Returns a time difference in a human format.
   * e.g. "about two weeks ago".
   */
  public static String humanPastTimeFromTimestamp(long unixEpochMs, long now) {
    return humanPastTime(new Date(unixEpochMs), new Date(now));
  }

  /** Return a color value to use for the book. */
  public static int calculateBookColor(Book book) {
    final String colorKey = book.getTitle() + book.getAuthor();
    float color = 360 * (Math.abs(colorKey.hashCode()) / (float) Integer.MAX_VALUE);
    return Color.HSVToColor(new float[]{color, 0.4f, 0.5f});
  }

  /** Returns the sessions a sorted stops list for the segmented progress bar. */
  public static float[] getSessionStops(Collection<Session> sessions) {
    float[] stops = new float[sessions.size()];
    int i = 0;
    for(Session session : sessions) {
      stops[i++] = session.getEndPosition();
    }

    Arrays.sort(stops);
    return stops;
  }

  /** Lifted from Google Guava. */
  public static boolean equal(Object a, Object b) {
    return a == b || (a != null && a.equals(b));
  }

  private static int[] bucketMilliseconds(long milliseconds) {
    int seconds = (int) (milliseconds / 1000.d);
    int minutes = (int) (seconds / 60.d);
    int hours = (int) (minutes / 60.0d);

    seconds = seconds - minutes * 60;
    minutes = minutes - hours * 60;

    return new int[]{hours, minutes, seconds};
  }

  private static String toSentence(String[] items) {
    if(items.length == 0) {
      return "";
    }
    if(items.length == 1) {
      return items[0];
    }
    if(items.length == 2) {
      return String.format("%s and %s", items[0], items[1]);
    }

    String joined = "";
    for(int i = 0; i < items.length - 1; i++) {
      joined += items[i] + ", ";
    }

    return String.format("%s and %s", joined.substring(0, joined.length() - 2), items[items.length - 1]);
  }

  @SuppressWarnings("deprecation")
  private static String humanPastTime(Date then, Date now) {
    if(then.after(now)) {
      return "";
    }

    Date todayMidnight = (Date) now.clone();
    todayMidnight.setHours(0);
    todayMidnight.setMinutes(0);
    todayMidnight.setSeconds(0);

    if(then.after(todayMidnight)) {
      return "earlier today";
    }

    Date dateRunner = new Date(todayMidnight.getTime() - 1 * DAYS);
    if(then.after(dateRunner)) {
      return "yesterday";
    }

    dateRunner = new Date(todayMidnight.getTime() - 2 * DAYS);
    if(then.after(dateRunner)) {
      return "two days ago";
    }

    dateRunner = new Date(todayMidnight.getTime() - 3 * DAYS);
    if(then.after(dateRunner)) {
      return "three days ago";
    }

    dateRunner = new Date(todayMidnight.getTime() - 9 * DAYS);
    if(then.after(dateRunner)) {
      return "about a week ago";
    }

    dateRunner = new Date(todayMidnight.getTime() - 18 * DAYS);
    if(then.after(dateRunner)) {
      return "about two weeks ago";
    }

    dateRunner = new Date(todayMidnight.getTime() - 24 * DAYS);
    if(then.after(dateRunner)) {
      return "about three weeks ago";
    }

    dateRunner = new Date(todayMidnight.getTime() - 45 * DAYS);
    if(then.after(dateRunner)) {
      return "about a month ago";
    }

    final SimpleDateFormat dateFormat = new SimpleDateFormat("'on 'MMM d, yyyy", Locale.ENGLISH);

    return dateFormat.format(then);
  }

  /** Returns the string content of a file. */
  public static String readInputFile(File importFile) throws IOException {
    return readInputStream(new FileInputStream(importFile));
  }

  /**
   * Reads an InputStream and returns it as a String.
   */
  public static String readInputStream(InputStream inputStream) throws IOException {
    BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF8"));
    StringBuilder out = new StringBuilder();
    String line;
    while((line = reader.readLine()) != null) {
      out.append(line);
    }
    reader.close();
    return out.toString();
  }

  public static boolean isEmpty(String string) {
    return string == null || string.length() == 0;
  }
}
