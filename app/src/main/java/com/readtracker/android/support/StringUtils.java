package com.readtracker.android.support;

import android.content.Context;

import com.readtracker.R;

import java.text.DateFormat;
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
    if (date.after(now)) {
      return "";
    }

    final Calendar calendarInstance = Calendar.getInstance();
    calendarInstance.setTime(now);
    calendarInstance.set(Calendar.HOUR_OF_DAY, 0);
    calendarInstance.set(Calendar.MINUTE, 0);
    calendarInstance.set(Calendar.SECOND, 0);
    final Date todayMidnight = calendarInstance.getTime();

    if (date.after(todayMidnight)) {
      return context.getString(R.string.date_earlier_today);
    }

    long numberOfDaysInThePast = 1 + (todayMidnight.getTime() - date.getTime()) / MILLISECONDS_IN_A_DAY;

    if (numberOfDaysInThePast <= 1) {
      return context.getString(R.string.date_yesterday);
    }

    if (numberOfDaysInThePast <= 2) {
      return context.getString(R.string.date_two_days_ago);
    }

    if (numberOfDaysInThePast <= 3) {
      return context.getString(R.string.date_three_days_ago);
    }

    if (numberOfDaysInThePast <= 9) {
      return context.getString(R.string.date_about_a_week_ago);
    }

    if (numberOfDaysInThePast <= 18) {
      return context.getString(R.string.date_about_two_weeks_ago);
    }

    if (numberOfDaysInThePast <= 24) {
      return context.getString(R.string.date_about_three_weeks_ago);
    }

    if (numberOfDaysInThePast <= 45) {
      return context.getString(R.string.date_about_a_month_ago);
    }

    return getDateString(date.getTime(), context);
  }
}
