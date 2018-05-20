package com.readtracker.android;

import android.content.Context;
import android.content.SharedPreferences;

public class TestUtils {
  public static void clearPreferences(Context context) {
    SharedPreferences.Editor preferences = context.getSharedPreferences(
        ReadTrackerApp.PREFERENCES_FILE_NAME, Context.MODE_PRIVATE
    ).edit();
    preferences.clear();
    preferences.commit();
  }
}
