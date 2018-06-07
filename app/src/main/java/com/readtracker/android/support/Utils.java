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
   * Return a sentence of a list of items.
   *
   * TODO(christoffer, translation) Must be some way to do this with the translation framework too
   *
   * toSentence(["foo"]) => "foo"
   * toSentence(["foo", "bar"]) => "foo and bar"
   * toSentence(["foo", "bar", "baz"]) => "foo, bar, and baz"
   */
  public static String toSentence(String[] items) {
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
