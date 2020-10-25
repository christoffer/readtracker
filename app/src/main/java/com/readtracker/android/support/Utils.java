package com.readtracker.android.support;


import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.TypedValue;

import com.readtracker.android.db.Session;

import java.io.*;
import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;

/**
 * Generic utility functions
 */
public class Utils {


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

  public static Locale getLocale(Context context) {
    final Configuration config = context.getResources().getConfiguration();
    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
      return getLocale(config);
    } else {
      return getLocaleLegacy(config);
    }
  }

  @TargetApi(Build.VERSION_CODES.N)
  private static Locale getLocale(Configuration config) {
    return config.getLocales().get(0);
  }

  private static Locale getLocaleLegacy(Configuration config) {
    return config.locale;
  }
}
