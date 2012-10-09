package com.readtracker;


import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

/**
 * Generic utility functions
 */
public class Utils {
  private static final long DAYS = 60 * 60 * 24 * 1000;

  public static String hoursAndMinutesFromMillis(long duration) {
    long[] hms = convertMillisToHoursMinutesSeconds(duration);
    long hours = hms[0];
    long minutes = hms[1];

    if(hours == 0) {
      return String.format("%s min", minutes);
    }

    return String.format("%sh %sm", hours, minutes);
  }

  public static String shortHumanTimeFromMillis(long duration) {
    long[] hms = convertMillisToHoursMinutesSeconds(duration);

    if(hms[0] == 0 && hms[1] == 0) {
      return String.format("%ss", hms[2]);
    }

    if(hms[0] == 0) {
      return String.format("%sm %ss", hms[1], hms[2]);
    }

    return String.format("%sh %sm %ss", hms[0], hms[1], hms[2]);
  }

  public static String longHumanTimeFromMillis(long durationMillis) {
    long[] hms = convertMillisToHoursMinutesSeconds(durationMillis);

    long hours = hms[0];
    long minutes = hms[1];
    long seconds = hms[2];

    ArrayList<String> parts = new ArrayList<String>(3);

    if(hours > 0) parts.add(_pluralized(hours, "hour"));
    if(minutes > 0) parts.add(_pluralized(minutes, "minute"));
    if(seconds > 0 || parts.size() == 0)
      parts.add(_pluralized(seconds, "second"));

    return toSentence(parts.toArray(new String[parts.size()]));
  }

  public static String longHumanTimeFromSeconds(long durationSeconds) {
    return longHumanTimeFromMillis(durationSeconds * 1000);
  }

  private static String _pluralized(long number, String name) {
    if(number == 1) {
      return String.format("%d %s", number, name); // 1 hour
    }
    return String.format("%d %ss", number, name); // 4 hours
  }

  public static String longCoarseHumanTimeFromMillis(long durationMillis) {
    long durationSeconds = durationMillis / 1000;
    if(durationSeconds < 60) {
      return longHumanTimeFromMillis(durationMillis);
    }
    durationSeconds = (durationSeconds / 60) * 60;
    return longHumanTimeFromMillis(durationSeconds * 1000);
  }

  public static String pluralize(int num, String noun) {
    return num == 1 ? noun : noun + (noun.endsWith("s") ? "es" : "s");
  }

  public static long parseLong(String str, long defaultValue) {
    try {
      return Long.parseLong(str);
    } catch(NumberFormatException ex) {
      return defaultValue;
    }
  }

  public static int parseInt(String str, int defaultValue) {
    try {
      return Integer.parseInt(str);
    } catch(NumberFormatException ex) {
      return defaultValue;
    }
  }

  public static long[] convertMillisToHoursMinutesSeconds(long milliseconds) {
    long seconds = (long) (milliseconds / 1000.d);
    long minutes = (long) (seconds / 60.d);
    long hours = (long) (minutes / 60.0d);

    seconds = seconds - minutes * 60;
    minutes = minutes - hours * 60;

    return new long[] { hours, minutes, seconds };
  }

  public static int getHoursFromMillis(long milliseconds) {
    return (int) convertMillisToHoursMinutesSeconds(milliseconds)[0];
  }

  public static int getMinutesFromMillis(long milliseconds) {
    return (int) convertMillisToHoursMinutesSeconds(milliseconds)[1];
  }

  public static int getSecondsFromMillis(long milliseconds) {
    return (int) convertMillisToHoursMinutesSeconds(milliseconds)[2];
  }

  /**
   * Creates a sentence from a set of items.
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

    String joined = "";
    for(int i = 0; i < items.length - 1; i++) {
      joined += items[i] + ", ";
    }

    return String.format("%s and %s", joined.substring(0, joined.length() - 2), items[items.length - 1]);
  }

  public static String humanTimeOfDay(Date occurredAt) {
    int hour = occurredAt.getHours();
    if(hour >= 4 && hour < 9) {
      return "in the morning";
    }
    if(hour >= 9 && hour < 11) {
      return "midmorning";
    }
    if(hour >= 11 && hour < 13) {
      return "around noon";
    }
    if(hour >= 13 && hour < 16) {
      return "in the afternoon";
    }
    if(hour >= 16 && hour < 19) {
      return "in the late afternoon";
    }
    if(hour >= 19 && hour < 23) {
      return "in the evening";
    }
    return "at night";
  }

  public static String humanPastDate(Date pastDate) {
    return humanPastDate(new Date(), pastDate);
  }

  public static String humanPastDate(Date now, Date then) {
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
}
