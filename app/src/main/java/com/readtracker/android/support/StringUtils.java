package com.readtracker.android.support;

import android.content.Context;

import com.readtracker.R;

import java.text.DateFormat;
import java.util.Date;

public class StringUtils {
  private static final long MILLISECONDS_IN_A_DAY = 60 * 60 * 24 * 1000;

  /**
   * Returns a date string based on the context (accounting for locale).
   */
  public static String getDateString(long timestampUnixEpochMs, Context context) {
    final Date date = new Date(timestampUnixEpochMs);
    final DateFormat format = android.text.format.DateFormat.getMediumDateFormat(context);
    return context.getString(R.string.general_on_date, format.format(date));
  }

  /**
   * Returns a time difference in a human format.
   * e.g. "about two weeks ago".
   * TODO(christoffer, translation) Replace with Android translations or the builtin Android helpers
   */
  public static String humanPastTimeFromTimestamp(long unixEpochMs, long now, Context context) {
    return humanPastTime(new Date(unixEpochMs), new Date(now), context);
  }

  /**
   * Return a human readable form of a time in the past.
   *
   * TODO(christoffer, translation) There's probably some helper for this as well in the Android
   * framework.
   */
  @SuppressWarnings("deprecation")
  private static String humanPastTime(Date then, Date now, Context context) {
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

    Date dateRunner = new Date(todayMidnight.getTime() - MILLISECONDS_IN_A_DAY);
    if(then.after(dateRunner)) {
      return "yesterday";
    }

    dateRunner = new Date(todayMidnight.getTime() - 2 * MILLISECONDS_IN_A_DAY);
    if(then.after(dateRunner)) {
      return "two days ago";
    }

    dateRunner = new Date(todayMidnight.getTime() - 3 * MILLISECONDS_IN_A_DAY);
    if(then.after(dateRunner)) {
      return "three days ago";
    }

    dateRunner = new Date(todayMidnight.getTime() - 9 * MILLISECONDS_IN_A_DAY);
    if(then.after(dateRunner)) {
      return "about a week ago";
    }

    dateRunner = new Date(todayMidnight.getTime() - 18 * MILLISECONDS_IN_A_DAY);
    if(then.after(dateRunner)) {
      return "about two weeks ago";
    }

    dateRunner = new Date(todayMidnight.getTime() - 24 * MILLISECONDS_IN_A_DAY);
    if(then.after(dateRunner)) {
      return "about three weeks ago";
    }

    dateRunner = new Date(todayMidnight.getTime() - 45 * MILLISECONDS_IN_A_DAY);
    if(then.after(dateRunner)) {
      return "about a month ago";
    }

    return getDateString(then.getTime(), context);
  }
}
