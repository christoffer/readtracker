package com.readtracker.android.support;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;

import com.readtracker.R;
import com.readtracker.android.db.Book;
import com.readtracker.android.db.Session;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
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
   */
  public static String humanPastTimeFromTimestamp(long unixEpochMs, long now, Context context) {
    return humanPastTime(new Date(unixEpochMs), new Date(now), context);
  }

  /**
   * Return a human readable form of a time in the past
   */
  private static String humanPastTime(Date date, Date now, Context context) {
    if(date.after(now)) {
      return "";
    }

    final Calendar calendarInstance = Calendar.getInstance();
    calendarInstance.setTime(now);
    calendarInstance.set(Calendar.HOUR_OF_DAY, 0);
    calendarInstance.set(Calendar.MINUTE, 0);
    calendarInstance.set(Calendar.SECOND, 0);
    final Date todayMidnight = calendarInstance.getTime();

    if(date.after(todayMidnight)) {
      return context.getString(R.string.date_earlier_today);
    }

    long numberOfDaysInThePast = 1 + (todayMidnight.getTime() - date.getTime()) / MILLISECONDS_IN_A_DAY;

    if(numberOfDaysInThePast <= 1) {
      return context.getString(R.string.date_yesterday);
    }

    if(numberOfDaysInThePast <= 2) {
      return context.getString(R.string.date_two_days_ago);
    }

    if(numberOfDaysInThePast <= 3) {
      return context.getString(R.string.date_three_days_ago);
    }

    if(numberOfDaysInThePast <= 9) {
      return context.getString(R.string.date_about_a_week_ago);
    }

    if(numberOfDaysInThePast <= 18) {
      return context.getString(R.string.date_about_two_weeks_ago);
    }

    if(numberOfDaysInThePast <= 24) {
      return context.getString(R.string.date_about_three_weeks_ago);
    }

    if(numberOfDaysInThePast <= 45) {
      return context.getString(R.string.date_about_a_month_ago);
    }

    return getDateString(date.getTime(), context);
  }

  /**
   * Returns a string representation like "3 hours, 12 minutes"
   *
   * @param duration the duration to represent, given in milliseconds
   */
  public static String hoursAndMinutesFromMillis(long duration, Context context) {
    int[] hms = convertMillisecondsToHMS(duration);
    int hours = hms[0];
    int minutes = hms[1];
    final Resources res = context.getResources();

    if(hours == 0) {
      return res.getQuantityString(R.plurals.plural_minute, minutes, minutes);
    }

    final String pluralHours = res.getQuantityString(R.plurals.plural_hour, hours, hours);
    final String pluralMinutes = res.getQuantityString(R.plurals.plural_minute, minutes, minutes);
    return context.getString(R.string.time_hours_and_minutes, pluralHours, pluralMinutes);
  }

  /**
   * Returns a duration as x hours, y minutes and z seconds.
   * Parts that are 0 are left out.
   * Eg: 3 hours and 12 seconds.
   */
  public static String longHumanTimeFromMillis(long durationMillis, Context context) {
    int[] hms = convertMillisecondsToHMS(durationMillis);
    final Resources res = context.getResources();

    int hours = hms[0];
    int minutes = hms[1];
    int seconds = hms[2];

    ArrayList<String> parts = new ArrayList<>(3);

    if(hours > 0) parts.add(res.getQuantityString(R.plurals.plural_hour, hours, hours));
    if(minutes > 0) parts.add(res.getQuantityString(R.plurals.plural_minute, minutes, minutes));
    if(seconds > 0 || parts.size() == 0)
      parts.add(res.getQuantityString(R.plurals.plural_second, seconds, seconds));

    if(parts.size() == 1) {
      return parts.get(0);
    }

    if(parts.size() == 2) {
      return context.getString(R.string.general_two_item_sentence, parts.get(0), parts.get(1));
    }

    return context.getString(R.string.general_three_item_sentence, parts.get(0), parts.get(1), parts.get(2));
  }

  /**
   * Convenience wrapper around {longCoarseHumanTimeFromMillis}
   */
  public static String longCoarseHumanTimeFromSeconds(long seconds, Context context) {
    return longCoarseHumanTimeFromMillis(seconds * 1000, context);
  }

  /**
   * Returns a string describing a duration in matter of hours and minutes.
   */
  public static String longCoarseHumanTimeFromMillis(long durationMillis, Context context) {
    long durationSeconds = durationMillis / 1000;
    if(durationSeconds < 60) {
      return longHumanTimeFromMillis(durationMillis, context);
    }
    durationSeconds = (durationSeconds / 60) * 60;
    return longHumanTimeFromMillis(durationSeconds * 1000, context);
  }

  /**
   * Converts a range given in milliseconds to hours, minutes and seconds.
   * <p>
   * Returns the result as an int array with three items: [H, M, S]
   */
  public static int[] convertMillisecondsToHMS(long milliseconds) {
    int seconds = (int) (milliseconds / 1000.d);
    int minutes = (int) (seconds / 60.d);
    int hours = (int) (minutes / 60.0d);

    seconds = seconds - minutes * 60;
    minutes = minutes - hours * 60;

    return new int[]{hours, minutes, seconds};
  }

  /**
   * Returns a formatted string like "13 pages" or "0.4%" based on how long the reading was in the
   * provided Session.
   */
  public static String formatSessionReadAmountHtml(Context context, Session session) {
    Book sessionBook = session.getBook();

    if(sessionBook == null || !sessionBook.hasPageNumbers()) {
      final float length = 100 * session.getLength();
      return context.getString(R.string.StringUtils_format_pages_length_pct_html, length); // => "0.4%"
    }

    final float pages = sessionBook.getPageCount();
    final int pageCount = Math.round(pages * session.getLength());
    return context.getString(R.string.StringUtils_format_pages_length_pages_html, pageCount);
  }


  /**
   * Returns a formatted string like "<b>2</b>h, <b>13</b>min or <b>45</b> minutes based on the
   * session duration
   */
  public static String formatSessionDurationHtml(Context context, Session session) {
    final Resources res = context.getResources();

    int[] hms = convertMillisecondsToHMS(session.getDurationSeconds() * 1000);
    if(hms[0] > 0) {
      // has hours, use short format
      return res.getString(R.string.StringUtils_format_duration_short_html, hms[0], hms[1]); // => "2h, 13min"
    }
    // use long format
    return res.getQuantityString(R.plurals.plural_minute_html, hms[1], hms[1]); // => "13 minutes"
  }

  /**
   * Returns a formatted string like "p. 12 - 15" or "14.5 - 16.6%" based on the start and end page
   * of the session.
   */
  public static String formatSessionFromTo(Context context, Session session) {
    Book sessionBook = session.getBook();

    if(sessionBook == null || !sessionBook.hasPageNumbers()) {
      final float startPct = session.getStartPosition() * 100f;
      final float endPct = session.getEndPosition() * 100f;
      return context.getString(R.string.StringUtils_format_session_from_to_pct, startPct, endPct);
    }

    final float pageCount = sessionBook.getPageCount();
    final int startPage = Math.round(session.getStartPosition() * pageCount);
    final int endPage = Math.round(session.getEndPosition() * pageCount);
    return context.getString(R.string.StringUtils_format_session_from_to_pages, startPage, endPage);
  }
}
