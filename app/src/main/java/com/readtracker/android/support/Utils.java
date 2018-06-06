package com.readtracker.android.support;


import android.content.Context;
import android.util.DisplayMetrics;
import android.util.TypedValue;

import com.readtracker.android.db.Session;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

/**
 * Generic utility functions
 */
public class Utils {

  /**
   * Returns a string representation like "3 hours, 12 minutes"
   *
   * TODO(christoffer, translation) Replace with Android translations
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
   *
   * TODO(christoffer, translation) Replace with Android translations
   */
  public static String longHumanTimeFromMillis(long durationMillis) {
    int[] hms = bucketMilliseconds(durationMillis);

    int hours = hms[0];
    int minutes = hms[1];
    int seconds = hms[2];

    ArrayList<String> parts = new ArrayList<>(3);

    if(hours > 0) parts.add(pluralizeWithCount(hours, "hour"));
    if(minutes > 0) parts.add(pluralizeWithCount(minutes, "minute"));
    if(seconds > 0 || parts.size() == 0)
      parts.add(pluralizeWithCount(seconds, "second"));

    return toSentence(parts.toArray(new String[parts.size()]));
  }

  /**
   * TODO(christoffer, translation) Replace with Android translations
   * @see #longCoarseHumanTimeFromMillis(long)
   */
  public static String longCoarseHumanTimeFromSeconds(long seconds) {
    return longCoarseHumanTimeFromMillis(seconds * 1000);
  }

  /**
   * TODO(christoffer, translation) Replace with Android translations
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
   * TODO(christoffer, translation) Replace with Android translations
   */
  public static String pluralizeWord(int num, String noun) {
    return num == 1 ? noun : noun + (noun.endsWith("s") ? "es" : "s");
  }

  /**
   * Returns the count and pluralized version of a word.
   * e.g. "1 dog" vs. "3 dogs"
   * TODO(christoffer, translation) Replace with Android translations
   */
  public static String pluralizeWithCount(int num, String noun) {
    return String.format("%d %s", num, pluralizeWord(num, noun));
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

  /**
   * Return the number of hours, minutes and seconds of a timestamp (millisecond length)
   */
  private static int[] bucketMilliseconds(long milliseconds) {
    int seconds = (int) (milliseconds / 1000.d);
    int minutes = (int) (seconds / 60.d);
    int hours = (int) (minutes / 60.0d);

    seconds = seconds - minutes * 60;
    minutes = minutes - hours * 60;

    return new int[]{hours, minutes, seconds};
  }

  /**
   * Return a sentence of a list of items.
   *
   * TODO(christoffer, translation) Must be some way to do this with the translation framework too
   *
   * toSentence(["foo"]) => "foo"
   * toSentence(["foo", "bar"]) => "foo and bar"
   * toSentence(["foo", "bar", "baz"]) => "foo, bar, and baz"
   */
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

    StringBuilder joined = new StringBuilder();
    for(int i = 0; i < items.length - 1; i++) {
      joined.append(items[i]).append(", ");
    }

    return String.format("%s and %s", joined.substring(0, joined.length() - 2), items[items.length - 1]);
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

  public static int convertDPtoPixels(Context context, int dpValue) {
    final DisplayMetrics metrics = context.getResources().getDisplayMetrics();
    return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dpValue, metrics);
  }
}
