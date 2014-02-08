package com.readtracker.android.activities;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.util.Log;

import com.readtracker.android.ApplicationReadTracker;
import com.readtracker.android.IntentKeys;
import com.readtracker.android.R;
import com.readtracker.android.SettingsKeys;
import com.readtracker.android.support.ReadTrackerUser;

public class SettingsActivity extends PreferenceActivity {
  private static final String TAG = SettingsActivity.class.getName();

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    addPreferencesFromResource(R.xml.settings);

    Preference emailPreference = findPreference(SettingsKeys.USER_EMAIL);

    final ReadTrackerUser currentUser = getApp().getCurrentUser();

    // Current user info
    if(currentUser == null) {
      emailPreference.setSummary(R.string.settings_not_logged_in);
      emailPreference.setEnabled(false);
    } else {
      emailPreference.setSummary(getString(R.string.settings_logged_in_as, currentUser.getDisplayName()));
      //emailPreference.setSummary("Logged in as " + currentUser.getDisplayName());
      emailPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
        @Override public boolean onPreferenceClick(Preference preference) {
          Uri userUri = Uri.parse(currentUser.getWebURL());
          Intent browserIntent = new Intent(Intent.ACTION_VIEW, userUri);
          startActivity(browserIntent);
          return true;
        }
      });
    }

    // Hook up the sign out button

    Preference signOutPreference = findPreference(SettingsKeys.USER_SIGN_OUT);
    if(currentUser == null) {
      signOutPreference.setTitle(R.string.settings_sign_in);
      signOutPreference.setSummary(R.string.settings_sign_in_subtext);
    } else {
      signOutPreference.setTitle(R.string.settings_sign_out);
    }

    signOutPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
      @Override public boolean onPreferenceClick(Preference preference) {
        setResult(ActivityCodes.RESULT_SIGN_OUT);
        finish();
        return true;
      }
    });

    try {
      String versionName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
      Preference versionPreference = findPreference(SettingsKeys.ABOUT_VERSION);
      versionPreference.setSummary(versionName);
    } catch(PackageManager.NameNotFoundException ignored) {
    }

    Preference legalPreference = findPreference(SettingsKeys.ABOUT_LEGAL);
    legalPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
      @Override
      public boolean onPreferenceClick(Preference preference) {
        Intent intentWebView = new Intent(SettingsActivity.this, InAppBrowserActivity.class);
        intentWebView.putExtra(IntentKeys.WEB_VIEW_URL, "file:///android_asset/legal.html");
        startActivity(intentWebView);
        return true;
      }
    });

    final CheckBoxPreference compactReadingList = (CheckBoxPreference) findPreference(SettingsKeys.SETTINGS_COMPACT_FINISH_LIST);
    compactReadingList.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
      @Override
      public boolean onPreferenceChange(Preference preference, Object value) {
        boolean isCompactMode = (Boolean) value;
        Log.i(TAG, "Changing compact mode to: " + isCompactMode);
        ApplicationReadTracker.getApplicationPreferences()
          .edit()
          .putBoolean(SettingsKeys.SETTINGS_COMPACT_FINISH_LIST, isCompactMode)
          .commit();
        return true;
      }
    });
  }

  /**
   * Provides a shorter method name for grabbing a type casted reference to the
   * application instance.
   *
   * @return the application instance
   */
  private ApplicationReadTracker getApp() {
    return (ApplicationReadTracker) getApplication();
  }
}
