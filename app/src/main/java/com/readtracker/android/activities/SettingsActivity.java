package com.readtracker.android.activities;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import androidx.annotation.NonNull;
import android.util.Log;

import com.readtracker.R;
import com.readtracker.android.IntentKeys;
import com.readtracker.android.ReadTrackerApp;
import com.readtracker.android.db.export.JSONImporter;
import com.readtracker.android.support.ReadTrackerDataImportHandler;
import com.readtracker.android.tasks.ImportReadTrackerFileTask;

import java.util.Arrays;

public class SettingsActivity extends PreferenceActivity implements ImportReadTrackerFileTask.ResultListener {
  private static final String TAG = SettingsActivity.class.getName();

  private static final String SETTINGS_COMPACT_FINISH_LIST = "settings.compact_finish_list";
  private static final String SETTINGS_FULL_DATES = "settings.full_dates";
  private static final String IMPORT_JSON = "data.import_json";
  private static final String EXPORT_JSON = "data.export_json";
  private static final String ABOUT_VERSION = "about.version";
  private static final String ABOUT_LEGAL = "about.legal";

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    // TODO Fix deprecation
    //noinspection deprecation
    addPreferencesFromResource(R.xml.settings);

    try {
      String versionName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
      // TODO Fix deprecation
      //noinspection deprecation
      Preference versionPreference = findPreference(ABOUT_VERSION);
      versionPreference.setSummary(versionName);
    } catch(PackageManager.NameNotFoundException ignored) {
    }

    // TODO Fix deprecation
    //noinspection deprecation
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

    // TODO Fix deprecation
    //noinspection deprecation
    final CheckBoxPreference compactReadingList = (CheckBoxPreference) findPreference(SETTINGS_COMPACT_FINISH_LIST);
    compactReadingList.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
      @Override
      public boolean onPreferenceChange(Preference preference, Object value) {
        boolean isCompactMode = (Boolean) value;
        Log.i(TAG, "Changing compact mode to: " + isCompactMode);
        ReadTrackerApp.from(SettingsActivity.this).getAppSettings().setUseCompactFinishedList(isCompactMode);
        return true;
      }
    });

    // TODO Fix deprecation
    //noinspection deprecation
    final CheckBoxPreference useFullDatesCheckbox = (CheckBoxPreference) findPreference(SETTINGS_FULL_DATES);
    useFullDatesCheckbox.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
      @Override
      public boolean onPreferenceChange(Preference preference, Object value) {
        boolean shouldUseFullDates = (Boolean) value;
        Log.i(TAG, "Changing full dates value to: " + shouldUseFullDates);
        ReadTrackerApp.from(SettingsActivity.this).getAppSettings().setUseFullDates(shouldUseFullDates);
        return true;
      }
    });

    // TODO Fix deprecation
    //noinspection deprecation
    final Preference importData = findPreference(IMPORT_JSON);
    final Activity parentActivity = this;

    importData.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
      @Override public boolean onPreferenceClick(Preference preference) {
        ReadTrackerDataImportHandler.confirmImport(parentActivity);
        return true;
      }
    });

    // TODO Fix deprecation
    //noinspection deprecation
    final Preference exportData = findPreference(EXPORT_JSON);
    exportData.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
      @Override public boolean onPreferenceClick(Preference preference) {
        ReadTrackerDataImportHandler.confirmExport(parentActivity);
        return true;
      }
    });
  }

  @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    // NOTE Ignore return value since we're only doing activity results for import/export
    ReadTrackerDataImportHandler.handleActivityResult(this, requestCode, resultCode, data);
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    Log.v(TAG, String.format("onRequestPermissionsResult: permissions %s, grantedResults %s", Arrays.toString(permissions), Arrays.toString(grantResults)));
    ReadTrackerDataImportHandler.handleRequestPermissionResult(this, requestCode, permissions, grantResults);
  }

  @Override public void onImportStart() {
    ReadTrackerDataImportHandler.openProgressDialog(this);
  }

  @Override public void onImportComplete(JSONImporter.ImportResultReport result) {
    ReadTrackerDataImportHandler.closeProgressDialog(this, result);
  }

  @Override public void onImportUpdate(int currentBook, int totalBooks) {
    ReadTrackerDataImportHandler.showProgressUpdate(currentBook, totalBooks);
  }

  @Override public Activity getResultActivity() {
    return this;
  }
}
