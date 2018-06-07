package com.readtracker.android.support;

import android.content.SharedPreferences;

/** Helper class for convenient access to app settings. */
public class ApplicationSettingsHelper {
  private final SharedPreferences mPreferences;

  private static final String KEY_COMPACT_FINISH_LIST = "settings.compact_finish_list";
  private static final String KEY_FULL_DATES = "settings.full_dates";

  public ApplicationSettingsHelper(SharedPreferences preferences) {
    mPreferences = preferences;
  }

  public boolean getUseCompactFinishedList() {
    return mPreferences.getBoolean(KEY_COMPACT_FINISH_LIST, false);
  }

  public void setUseCompactFinishedList(boolean value) {
    mPreferences
        .edit()
        .putBoolean(KEY_COMPACT_FINISH_LIST, value)
        .apply();
  }

  public boolean getUseFullDates() {
    return mPreferences.getBoolean(KEY_FULL_DATES, false);
  }

  public void setUseFullDates(boolean value) {
    mPreferences
        .edit()
        .putBoolean(KEY_FULL_DATES, value)
        .apply();
  }
}
