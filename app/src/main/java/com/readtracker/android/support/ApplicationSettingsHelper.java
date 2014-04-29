package com.readtracker.android.support;

import android.content.SharedPreferences;

/** Helper class for convenient access to app settings. */
public class ApplicationSettingsHelper {
  private final SharedPreferences mPreferences;

  private static final String KEY_COMPACT_FINISH_LIST = "settings.compact_finish_list";

  public ApplicationSettingsHelper(SharedPreferences preferences) {
    mPreferences = preferences;
  }

  public boolean hasCompactFinishedList() {
    return mPreferences.getBoolean(KEY_COMPACT_FINISH_LIST, false);
  }

  public void setCompactFinishList(boolean value) {
    mPreferences
        .edit()
        .putBoolean(KEY_COMPACT_FINISH_LIST, value)
        .commit();
  }
}
