package com.readtracker.android.activities;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.util.Log;

import com.readtracker.R;
import com.readtracker.android.IntentKeys;
import com.readtracker.android.ReadTrackerApp;

public class SettingsActivity extends PreferenceActivity {
  private static final String TAG = SettingsActivity.class.getName();

  private static final String SETTINGS_COMPACT_FINISH_LIST = "settings.compact_finish_list";
  private static final String EXPORT_JSON = "data.export_json";
  private static final String ABOUT_VERSION = "about.version";
  private static final String ABOUT_LEGAL = "about.legal";

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    addPreferencesFromResource(R.xml.settings);

    try {
      String versionName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
      Preference versionPreference = findPreference(ABOUT_VERSION);
      versionPreference.setSummary(versionName);
    } catch(PackageManager.NameNotFoundException ignored) {
    }

    Preference legalPreference = findPreference(ABOUT_LEGAL);
    legalPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
      @Override
      public boolean onPreferenceClick(Preference preference) {
        Intent intentWebView = new Intent(SettingsActivity.this, InAppBrowserActivity.class);
        intentWebView.putExtra(IntentKeys.WEB_VIEW_URL, "file:///android_asset/legal.html");
        startActivity(intentWebView);
        return true;
      }
    });

    final CheckBoxPreference compactReadingList = (CheckBoxPreference) findPreference(SETTINGS_COMPACT_FINISH_LIST);
    compactReadingList.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
      @Override
      public boolean onPreferenceChange(Preference preference, Object value) {
        boolean isCompactMode = (Boolean) value;
        Log.i(TAG, "Changing compact mode to: " + isCompactMode);
        ReadTrackerApp.getApplicationPreferences()
            .edit()
            .putBoolean(SETTINGS_COMPACT_FINISH_LIST, isCompactMode)
            .commit();
        return true;
      }
    });

        return true;
      }
    });
  }
}
