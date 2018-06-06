package com.readtracker.android.activities;

import android.Manifest;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Environment;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
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
  private static final String SETTINGS_FULL_DATES = "settings.full_dates";
  private static final String IMPORT_JSON = "data.import_json";
  private static final String EXPORT_JSON = "data.export_json";
  private static final String ABOUT_VERSION = "about.version";
  private static final String ABOUT_LEGAL = "about.legal";

  private static final int FILE_PICKER_SELECT_FILE_REQUEST_CODE = 0x01;
  private static final int EXPORT_PERMISSION_REQUEST_CODE = 0x01;
  private static final int IMPORT_PERMISSION_REQUEST_CODE = 0x02;
  private ProgressDialog progressDialog;

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
    importData.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
      @Override public boolean onPreferenceClick(Preference preference) {
        confirmImport();
        return true;
      }
    });

    // TODO Fix deprecation
    //noinspection deprecation
    final Preference exportData = findPreference(EXPORT_JSON);
    exportData.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
      @Override public boolean onPreferenceClick(Preference preference) {
        confirmExport();
        return true;
      }
    });
  }

  @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);

    if(requestCode == FILE_PICKER_SELECT_FILE_REQUEST_CODE && resultCode == RESULT_OK) {
      String importFilePath = data.getStringExtra(FilePickerActivity.RESULT_FILE_PATH);
      if(importFilePath == null) {
        Log.d(TAG, "File picker returned with null file path");
        finish();
        return;
      }

      File importFile = new File(importFilePath);
      if(!importFile.exists() || !importFile.canRead()) {
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

  private void confirmImport() {
    final AlertDialog.Builder builder = new AlertDialog.Builder(this);
    builder
        .setTitle(R.string.settings_import_json)
        .setMessage(R.string.settings_import_confirmation)
        .setIcon(android.R.drawable.ic_dialog_alert)
        .setPositiveButton(R.string.settings_import_positive_button, new DialogInterface.OnClickListener() {
          @Override public void onClick(DialogInterface dialogInterface, int i) {
            importFileOrRequestPermission();
          }
        })
        .setNegativeButton(R.string.general_cancel, new DialogInterface.OnClickListener() {
          @Override public void onClick(DialogInterface dialogInterface, int i) {
            dialogInterface.cancel();
          }
        })
        .setCancelable(true).create().show();
  }



  private void confirmExport() {
    final AlertDialog.Builder builder = new AlertDialog.Builder(this);
    builder
        .setTitle(R.string.settings_export_json)
        .setMessage(R.string.settings_export_confirmation)
        .setPositiveButton(R.string.settings_export_positive_button, new DialogInterface.OnClickListener() {
          @Override public void onClick(DialogInterface dialogInterface, int i) {
            exportFilesOrRequestPermission();
          }
        })
        .setNegativeButton(R.string.general_cancel, new DialogInterface.OnClickListener() {
          @Override public void onClick(DialogInterface dialogInterface, int i) {
            dialogInterface.cancel();
          }
        })
        .setCancelable(true).create().show();
  }

  private void exportFilesOrRequestPermission() {
    final String requiredPermission = Manifest.permission.WRITE_EXTERNAL_STORAGE;
    if(ContextCompat.checkSelfPermission(this, requiredPermission) == PackageManager.PERMISSION_GRANTED) {
      Log.d(TAG, "Have permission for writing external storage");

      final boolean isExternalStorageWritable = Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState());
      if(!isExternalStorageWritable) {
        Log.d(TAG, "external storage not mounted as writable");
        Toast.makeText(this, R.string.settings_export_need_external_media, Toast.LENGTH_LONG).show();
        return;
      }

      final JSONExporter jsonExporter = JSONExporter.from(this);
      File exportToDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);

      final File exportedJsonFile = jsonExporter.exportAllBooksToDir(exportToDir);
      if(exportedJsonFile == null || !exportedJsonFile.exists()) {
        Log.e(TAG, String.format("Failed to write intermediary JSON export file. File was not created. exportedJSONFile: %s, exportToDir: %s", exportedJsonFile, exportToDir));
        Toast.makeText(this, R.string.settings_export_failed, Toast.LENGTH_LONG).show();
        return;
      }

      // File written to disk. Attempt to add it to the download manager so the user can find it
      // easily.
      DownloadManager downloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
      if(downloadManager == null) {
        Log.w(TAG, "Unexpectedly got a null DownloadManager");
      } else {
        final String filename = exportedJsonFile.getName();
        final String filepath = exportedJsonFile.getAbsolutePath();
        final long fileLength = exportedJsonFile.length();
        downloadManager.addCompletedDownload(filename, filename, true, "application/json", filepath, fileLength, true);
      }
      Toast.makeText(SettingsActivity.this, R.string.settings_export_success, Toast.LENGTH_SHORT).show();
    } else {
      Log.d(TAG, "Doesn't have permission for writing external storage");
      ActivityCompat.requestPermissions(this, new String[]{requiredPermission}, EXPORT_PERMISSION_REQUEST_CODE);
    }
  }

  private void importFileOrRequestPermission() {
    final String requiredPermission = Manifest.permission.READ_EXTERNAL_STORAGE;
    if(ContextCompat.checkSelfPermission(this, requiredPermission) == PackageManager.PERMISSION_GRANTED) {
      Log.v(TAG, "Have permission for accessing external storage");
      new MaterialFilePicker()
          .withActivity(this)
          .withRequestCode(FILE_PICKER_SELECT_FILE_REQUEST_CODE)
          .start();
    } else {
      Log.v(TAG, "Doesn't have permission for accessing external storage");
      ActivityCompat.requestPermissions(this, new String[]{requiredPermission}, IMPORT_PERMISSION_REQUEST_CODE);
    }
  }

  @Override public void onImportStart() {
    if(this.progressDialog != null) {
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
    final Resources res = getResources();
    if(result != null) {
      Log.d(TAG, result.toString());
      // NOTE(christoffer) pluralized translations seem complicated...
      final int totalBooks = result.createdBookCount + result.mergedBookCount;
      final String numBooks = res.getQuantityString(R.plurals.plural_book, totalBooks, totalBooks);
      final String numNewBooks = res.getQuantityString(R.plurals.plural_new, result.createdBookCount, result.createdBookCount);
      final String numMergedBook = res.getQuantityString(R.plurals.plural_merged, result.mergedBookCount, result.mergedBookCount);
      final String numQuotes = res.getQuantityString(R.plurals.plural_quote, result.createdQuotesCount, result.createdQuotesCount);
      final String numSessions = res.getQuantityString(R.plurals.plural_session, result.createdSessionCount, result.createdSessionCount);
      final String message = res.getString(R.string.settings_import_book_report, numBooks, numNewBooks, numMergedBook, numQuotes, numSessions);
      Toast.makeText(SettingsActivity.this, message, Toast.LENGTH_LONG).show();
    } else {
      Toast.makeText(SettingsActivity.this, R.string.settings_import_failed, Toast.LENGTH_LONG).show();
    }
  }

  @Override public void onImportUpdate(int currentBook, int totalBooks) {
    Log.d(TAG, String.format("import progress: %d out of %d", currentBook, totalBooks));
    if(this.progressDialog != null) {
      this.progressDialog.setIndeterminate(false);
      this.progressDialog.setMax(totalBooks);
      this.progressDialog.setProgress(currentBook);
    }
  }
}
