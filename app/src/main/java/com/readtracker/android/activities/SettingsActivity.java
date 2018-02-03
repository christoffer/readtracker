package com.readtracker.android.activities;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.support.annotation.NonNull;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.widget.Toast;

import com.readtracker.R;
import com.readtracker.android.IntentKeys;
import com.readtracker.android.ReadTrackerApp;
import com.readtracker.android.db.export.JSONExporter;

import java.io.File;

public class SettingsActivity extends PreferenceActivity {
  private static final String TAG = SettingsActivity.class.getName();

  private static final String SETTINGS_COMPACT_FINISH_LIST = "settings.compact_finish_list";
  private static final String IMPORT_JSON = "data.import_json";
  private static final String EXPORT_JSON = "data.export_json";
  private static final String ABOUT_VERSION = "about.version";
  private static final String ABOUT_LEGAL = "about.legal";
  private static final String ICONS8 = "about.icons8";

  private static final int REQUEST_IMPORT = 0x01;
  private static final int PERMISSION_FOR_EXPORT = 0x01;
  private static final int REQUEST_PERMISSION_FOR_IMPORT = 0x02;

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

    Preference icons8 = findPreference(ICONS8);
    icons8.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
      @Override public boolean onPreferenceClick(Preference preference) {
        Intent intentWebView = new Intent(Intent.ACTION_VIEW, Uri.parse("http://icons8.com/"));
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
        ReadTrackerApp.from(SettingsActivity.this).getAppSettings().setCompactFinishList(isCompactMode);
        return true;
      }
    });

    final Preference importData = findPreference(IMPORT_JSON);
    importData.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
      @Override public boolean onPreferenceClick(Preference preference) {
        return onImportDataClick();
      }
    });

    final Preference exportData = findPreference(EXPORT_JSON);
    exportData.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
      @Override public boolean onPreferenceClick(Preference preference) {
        return onExportDataClick();
      }
    });
  }

  @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    if(requestCode == REQUEST_IMPORT && resultCode == RESULT_OK) {
      finish();
    }
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    final boolean didGetPermission = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
    switch(requestCode) {
      case REQUEST_PERMISSION_FOR_IMPORT:
        if(didGetPermission) {
          onImportDataClick();
        }
        break;
      case PERMISSION_FOR_EXPORT:
        if(didGetPermission) {
          onExportDataClick();
        }
        break;
      default:
        break;
    }
  }

  private void showSendIntentForFile(File exportFile) {
    Log.d(TAG, String.format("Showing export for file %s", exportFile));
    try {
      final Uri uri = FileProvider.getUriForFile(this, "com.readtracker.fileprovider", exportFile);
      final Intent exportIntent = new Intent(Intent.ACTION_SEND);
      // NOTE(christoffer) Use text/plain instead of something JSON specific since there's a lot more
      // handlers that can handle sending plain text.
      exportIntent.putExtra(Intent.EXTRA_STREAM, uri);
      exportIntent.setType("text/plain");
      exportIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
      startActivity(Intent.createChooser(exportIntent, getString(R.string.settings_export_json_save_data)));
    } catch(IllegalArgumentException ex) {
      Log.e(TAG, String.format("Failed to create file URI for exported file %s, error: %s", exportFile, ex.toString()));
      Toast.makeText(SettingsActivity.this, R.string.settings_export_failed, Toast.LENGTH_SHORT).show();
    }
  }

  /**
   * Writes the JSON export data to a directory that is whitelisted for FileProvider.
   *
   * @return the exported File if successful, Null otherwise.
   */
  private File exportDataToFileProviderDir(JSONExporter jsonExporter) {
    // NOTE(christoffer) Path below needs to be declared in @xml/filepaths
    File tmpCacheDir = getCacheDir();
    if(!tmpCacheDir.exists() || !tmpCacheDir.canWrite()) {
      Log.e(TAG, String.format("Couldn't get a writable cache dir for writing the exported data (%s, exist: %b, write: %b", tmpCacheDir, tmpCacheDir.exists(), tmpCacheDir.canWrite()));
      return null;
    }

    File fileProviderDir = new File(tmpCacheDir, "exports");
    if (!fileProviderDir.exists()) {
      final boolean didCreate = fileProviderDir.mkdirs();
      if (!didCreate) {
        Log.e(TAG, "Failed to create file exporter directory in cacheDir");
        return null;
      }
    }

    final File exportedJsonFile = jsonExporter.exportAllBooksToDir(fileProviderDir);
    if (exportedJsonFile == null || !exportedJsonFile.exists()) {
      Log.e(TAG, String.format("Failed to write intermediary JSON export file. File was not created. exportedJSONFile: %s, tmpCacheDir: %s", exportedJsonFile, fileProviderDir));
      return null;
    }

    return exportedJsonFile;
  }

  private boolean onExportDataClick() {
    final JSONExporter jsonExporter = JSONExporter.from(SettingsActivity.this);
    File exportedDataFile = exportDataToFileProviderDir(jsonExporter);

    if(exportedDataFile == null) {
      Toast.makeText(SettingsActivity.this, R.string.settings_export_failed, Toast.LENGTH_SHORT).show();
    } else {
      showSendIntentForFile(exportedDataFile);
    }
    return true;
  }

  private boolean onImportDataClick() {
    Intent intent = new Intent(this, ImportActivity.class);
    startActivityForResult(intent, REQUEST_IMPORT);
    return true;
  }
}
