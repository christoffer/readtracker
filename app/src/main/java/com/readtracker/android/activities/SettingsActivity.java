package com.readtracker.android.activities;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.widget.Toast;

import com.nbsp.materialfilepicker.MaterialFilePicker;
import com.nbsp.materialfilepicker.ui.FilePickerActivity;
import com.readtracker.R;
import com.readtracker.android.IntentKeys;
import com.readtracker.android.ReadTrackerApp;
import com.readtracker.android.db.export.JSONExporter;
import com.readtracker.android.db.export.JSONImporter;
import com.readtracker.android.tasks.ImportReadTrackerFileTask;

import java.io.File;
import java.util.Arrays;

public class SettingsActivity extends PreferenceActivity implements ImportReadTrackerFileTask.ResultListener {
  private static final String TAG = SettingsActivity.class.getName();

  private static final String SETTINGS_COMPACT_FINISH_LIST = "settings.compact_finish_list";
  private static final String IMPORT_JSON = "data.import_json";
  private static final String EXPORT_JSON = "data.export_json";
  private static final String ABOUT_VERSION = "about.version";
  private static final String ABOUT_LEGAL = "about.legal";
  private static final String ICONS8 = "about.icons8";

  private static final int FILE_PICKER_SELECT_FILE_REQUEST_CODE = 0x01;
  private static final int EXPORT_PERMISSION_REQUEST_CODE = 0x01;
  private static final int IMPORT_PERMISSION_REQUEST_CODE = 0x02;
  private ProgressDialog progressDialog;

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
        importFileOrRequestPermission();
        return true;
      }
    });

    final Preference exportData = findPreference(EXPORT_JSON);
    exportData.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
      @Override public boolean onPreferenceClick(Preference preference) {
        exportFilesOrRequestPermission();
        return true;
      }
    });
  }

  @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);

    if(requestCode == FILE_PICKER_SELECT_FILE_REQUEST_CODE && resultCode == RESULT_OK) {
      String importFilePath = data.getStringExtra(FilePickerActivity.RESULT_FILE_PATH);
      if (importFilePath == null) {
        Log.d(TAG, "File picker returned with null file path");
        finish();
        return;
      }

      File importFile = new File(importFilePath);
      if (!importFile.exists() || !importFile.canRead()) {
        Log.d(TAG, String.format("File picker returned with file that exists: %b, canRead: %b - exiting", importFile.exists(), importFile.canRead()));
        finish();
        return;
      }

      Log.i(TAG, "Attempting import from file " + importFile.getAbsolutePath());
      ImportReadTrackerFileTask.importFile(importFile, ReadTrackerApp.from(this).getDatabaseManager(), this);
    }
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    Log.v(TAG, String.format("onRequestPermissionsResult: permissions %s, grantedResults %s", Arrays.toString(permissions), Arrays.toString(grantResults)));
    final boolean didGetPermission = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
    switch(requestCode) {
      case IMPORT_PERMISSION_REQUEST_CODE:
        if(didGetPermission) {
          importFileOrRequestPermission();
        }
        break;
      case EXPORT_PERMISSION_REQUEST_CODE:
        if(didGetPermission) {
          exportFilesOrRequestPermission();
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

  private void exportFilesOrRequestPermission() {
    // TODO ask for permission
    final JSONExporter jsonExporter = JSONExporter.from(SettingsActivity.this);
    File exportedDataFile = exportDataToFileProviderDir(jsonExporter);

    if(exportedDataFile == null) {
      Toast.makeText(SettingsActivity.this, R.string.settings_export_failed, Toast.LENGTH_SHORT).show();
    } else {
      showSendIntentForFile(exportedDataFile);
    }
  }

  private void importFileOrRequestPermission() {
    final String requiredPermission = Manifest.permission.WRITE_EXTERNAL_STORAGE;
    if (ContextCompat.checkSelfPermission(this, requiredPermission) == PackageManager.PERMISSION_GRANTED) {
      Log.v(TAG, "has permission");
      new MaterialFilePicker()
          .withActivity(this)
          .withRequestCode(FILE_PICKER_SELECT_FILE_REQUEST_CODE)
          .start();
    } else {
      Log.v(TAG, "Doesn't have permission");
      if (ActivityCompat.shouldShowRequestPermissionRationale(this, requiredPermission)) {
        Log.d(TAG, "Need permission");
      } else {
        Log.v(TAG, "Requesting permission");
        ActivityCompat.requestPermissions(this, new String[]{requiredPermission}, IMPORT_PERMISSION_REQUEST_CODE);
      }
    }
  }

  @Override public void onImportStart() {
    if (this.progressDialog != null) {
      this.progressDialog.dismiss();
    }
    this.progressDialog = new ProgressDialog(this);
    this.progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
    this.progressDialog.setIndeterminate(true);
    this.progressDialog.setMessage(getString(R.string.settings_import_running));
    this.progressDialog.show();
  }

  @Override public void onImportComplete(JSONImporter.ImportResultReport result) {
    this.progressDialog.dismiss();
    this.progressDialog = null;
    if (result != null) {
      Log.d(TAG, result.toString());
      // NOTE(christoffer) pluralized translations seem complicated...
      final Resources res = getResources();
      final int totalBooks = result.createdBookCount + result.mergedBookCount;
      final String numBooks = res.getQuantityString(R.plurals.plural_book, totalBooks, totalBooks);
      final String numNewBooks = res.getQuantityString(R.plurals.plural_new, result.createdBookCount, result.createdBookCount);
      final String numMergedBook = res.getQuantityString(R.plurals.plural_merged, result.mergedBookCount, result.mergedBookCount);
      final String numQuotes = res.getQuantityString(R.plurals.plural_quote, result.createdQuotesCount, result.createdQuotesCount);
      final String numSessions = res.getQuantityString(R.plurals.plural_session, result.createdSessionCount, result.createdSessionCount);
      final String message = res.getString(R.string.settings_import_book_report, numBooks, numNewBooks, numMergedBook, numQuotes, numSessions);
      Toast.makeText(SettingsActivity.this, message, Toast.LENGTH_LONG).show();
    }
  }

  @Override public void onImportUpdate(int currentBook, int totalBooks) {
    Log.d(TAG, String.format("import progress: %d out of %d", currentBook, totalBooks));
    if (this.progressDialog != null) {
      this.progressDialog.setIndeterminate(false);
      this.progressDialog.setMax(totalBooks);
      this.progressDialog.setProgress(currentBook);
    }
  }
}
