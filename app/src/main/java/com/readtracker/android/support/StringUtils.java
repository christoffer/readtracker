package com.readtracker.android.support;

import android.content.Context;

import com.readtracker.R;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import static com.readtracker.android.support.Utils.pluralizeWithCount;
import static com.readtracker.android.support.Utils.toSentence;

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

  /**
   * Returns a string representation like "3 hours, 12 minutes"
   *
   * TODO(christoffer, translation) Replace with Android translations
   *
   * @param duration the duration to represent
   * @param context
   * @return the duration formatted as full hours and minutes
   */
  public static String hoursAndMinutesFromMillis(long duration, Context context) {
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
  public static String longHumanTimeFromMillis(long durationMillis, Context context) {
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
   * @see #longCoarseHumanTimeFromMillis(long, Context)
   */
  public static String longCoarseHumanTimeFromSeconds(long seconds, Context context) {
    return longCoarseHumanTimeFromMillis(seconds * 1000, context);
  }

  /**
   * TODO(christoffer, translation) Replace with Android translations
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
}
