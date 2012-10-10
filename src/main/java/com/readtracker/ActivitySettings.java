package com.readtracker;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import com.readtracker.value_objects.ReadTrackerUser;

public class ActivitySettings extends PreferenceActivity {
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    addPreferencesFromResource(R.xml.settings);

    Preference emailPreference = findPreference(SettingsKeys.USER_EMAIL);

    final ReadTrackerUser currentUser = getApp().getCurrentUser();

    // Current user info
    if(currentUser == null) {
      emailPreference.setTitle("Anonymous user");
      emailPreference.setSummary("Not logged in to Readmill");
      emailPreference.setEnabled(false);
    } else {
      emailPreference.setTitle(currentUser.getDisplayName());
      emailPreference.setSummary(currentUser.getEmail());
      emailPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
        @Override
        public boolean onPreferenceClick(Preference preference) {
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
      signOutPreference.setTitle("Sign in");
      signOutPreference.setSummary("Click to sign in to Readmill");
    } else {
      signOutPreference.setTitle("Sign out");
    }

    signOutPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
      @Override
      public boolean onPreferenceClick(Preference preference) {
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
        Intent intentWebView = new Intent(ActivitySettings.this, ActivityWebView.class);
        intentWebView.putExtra(IntentKeys.WEB_VIEW_URL, "file:///android_asset/legal.html");
        startActivity(intentWebView);
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
