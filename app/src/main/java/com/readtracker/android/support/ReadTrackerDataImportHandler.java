package com.readtracker.android.support;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.Toast;

import com.nbsp.materialfilepicker.MaterialFilePicker;
import com.nbsp.materialfilepicker.ui.FilePickerActivity;
import com.readtracker.R;
import com.readtracker.android.ReadTrackerApp;
import com.readtracker.android.activities.ActivityCodes;
import com.readtracker.android.db.export.JSONExporter;
import com.readtracker.android.db.export.JSONImporter;
import com.readtracker.android.tasks.ImportReadTrackerFileTask;

import org.jetbrains.annotations.NotNull;

import java.io.File;

public class ReadTrackerDataImportHandler {
  private static final String TAG = "RTDataImportHandler";
  private static ProgressDialog progressDialog;

  public static void handleRequestPermissionResult(ImportReadTrackerFileTask.ResultListener resultListener, int requestCode, String[] permissions, int[] grantResults) {
    if (requestCode == ActivityCodes.IMPORT_PERMISSION_REQUEST_CODE) {
      if (wasGrantedPermission(Manifest.permission.READ_EXTERNAL_STORAGE, permissions, grantResults)) {
        importFileOrRequestPermission(resultListener.getResultActivity());
      }
    }

    if(requestCode == ActivityCodes.EXPORT_PERMISSION_REQUEST_CODE) {
      if (wasGrantedPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, permissions, grantResults)) {
        exportFilesOrRequestPermission(resultListener.getResultActivity());
      }
    }
  }

  public static boolean handleActivityResult(ImportReadTrackerFileTask.ResultListener resultListener, int requestCode, int resultCode, Intent data) {
    if(requestCode == ActivityCodes.FILE_PICKER_SELECT_FILE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
      String importFilePath = data.getStringExtra(FilePickerActivity.RESULT_FILE_PATH);
      if(importFilePath == null) {
        Log.d(TAG, "File picker returned with null file path");
        resultListener.getResultActivity().finish();
        return true;
      }

      File importFile = new File(importFilePath);
      if(!importFile.exists() || !importFile.canRead()) {
        Log.d(TAG, String.format("File picker returned with file that exists: %b, canRead: %b - exiting", importFile.exists(), importFile.canRead()));
        resultListener.getResultActivity().finish();
        return true;
      }

      Log.i(TAG, "Attempting import from file " + importFile.getAbsolutePath());
      ImportReadTrackerFileTask.importFile(
          importFile,
          ReadTrackerApp.from(resultListener.getResultActivity()).getDatabaseManager(),
          resultListener
      );
    }

    return false;
  }

  private static void importFileOrRequestPermission(final Activity parentActivity) {
    final String requiredPermission = Manifest.permission.READ_EXTERNAL_STORAGE;
    if(ContextCompat.checkSelfPermission(parentActivity, requiredPermission) == PackageManager.PERMISSION_GRANTED) {
      Log.v(TAG, "Have permission for accessing external storage");
      new MaterialFilePicker()
          .withActivity(parentActivity)
          .withRequestCode(ActivityCodes.FILE_PICKER_SELECT_FILE_REQUEST_CODE)
          .start();
    } else {
      Log.v(TAG, "Doesn't have permission for accessing external storage");
      ActivityCompat.requestPermissions(parentActivity, new String[]{requiredPermission}, ActivityCodes.IMPORT_PERMISSION_REQUEST_CODE);
    }
  }

  private static void exportFilesOrRequestPermission(final Activity parentActivity) {
    final String requiredPermission = Manifest.permission.WRITE_EXTERNAL_STORAGE;
    if(ContextCompat.checkSelfPermission(parentActivity, requiredPermission) == PackageManager.PERMISSION_GRANTED) {
      Log.d(TAG, "Have permission for writing external storage");

      final boolean isExternalStorageWritable = Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState());
      if(!isExternalStorageWritable) {
        Log.d(TAG, "external storage not mounted as writable");
        Toast.makeText(parentActivity, R.string.settings_export_need_external_media, Toast.LENGTH_LONG).show();
        return;
      }

      final JSONExporter jsonExporter = JSONExporter.from(parentActivity);
      File exportToDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);

      final File exportedJsonFile = jsonExporter.exportAllBooksToDir(exportToDir);
      if(exportedJsonFile == null || !exportedJsonFile.exists()) {
        Log.e(TAG, String.format("Failed to write intermediary JSON export file. File was not created. exportedJSONFile: %s, exportToDir: %s", exportedJsonFile, exportToDir));
        Toast.makeText(parentActivity, R.string.settings_export_failed, Toast.LENGTH_LONG).show();
        return;
      }
      notifySuccessAndOfferSharingIntent(parentActivity, exportedJsonFile.getAbsolutePath());
    } else {
      Log.d(TAG, "Doesn't have permission for writing external storage");
      ActivityCompat.requestPermissions(parentActivity, new String[]{requiredPermission}, ActivityCodes.EXPORT_PERMISSION_REQUEST_CODE);
    }
  }

  private static void notifySuccessAndOfferSharingIntent(final Activity parentActivity, final String absoluteFilepath) {
    final AlertDialog.Builder builder = new AlertDialog.Builder(parentActivity);
    builder
        .setTitle(R.string.settings_export_success)
        .setMessage(R.string.settings_export_share_file)
        .setIcon(android.R.drawable.ic_dialog_email)
        .setPositiveButton(R.string.settings_share_export, new DialogInterface.OnClickListener() {
          @Override public void onClick(DialogInterface dialogInterface, int i) {
            final Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);
            sendIntent.setType("text/*"); // NOTE(christoffer) Use text to offer more intent action handlers
            sendIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse("file//" + absoluteFilepath));
            parentActivity.startActivity(Intent.createChooser(sendIntent, parentActivity.getString(R.string.settings_share_export)));
          }
        })
        .setNegativeButton(R.string.general_cancel, new DialogInterface.OnClickListener() {
          @Override public void onClick(DialogInterface dialogInterface, int i) {
            dialogInterface.cancel();
          }
        })
        .setCancelable(true).create().show();
  }

  // NOTE(christoffer) Public static method so that it can be used to expose import buttons in
  // other activities in the app (for example the empty HomeActivity).
  public static void confirmImport(final Activity parentActivity) {
    final AlertDialog.Builder builder = new AlertDialog.Builder(parentActivity);
    builder
        .setTitle(R.string.settings_import_json)
        .setMessage(R.string.settings_import_confirmation)
        .setIcon(android.R.drawable.ic_dialog_alert)
        .setPositiveButton(R.string.settings_import_positive_button, new DialogInterface.OnClickListener() {
          @Override public void onClick(DialogInterface dialogInterface, int i) {
            importFileOrRequestPermission(parentActivity);
          }
        })
        .setNegativeButton(R.string.general_cancel, new DialogInterface.OnClickListener() {
          @Override public void onClick(DialogInterface dialogInterface, int i) {
            dialogInterface.cancel();
          }
        })
        .setCancelable(true).create().show();
  }

  public static void confirmExport(final Activity parentActivity) {
    final AlertDialog.Builder builder = new AlertDialog.Builder(parentActivity);
    builder
        .setTitle(R.string.settings_export_json)
        .setMessage(R.string.settings_export_confirmation)
        .setPositiveButton(R.string.settings_export_positive_button, new DialogInterface.OnClickListener() {
          @Override public void onClick(DialogInterface dialogInterface, int i) {
            exportFilesOrRequestPermission(parentActivity);
          }
        })
        .setNegativeButton(R.string.general_cancel, new DialogInterface.OnClickListener() {
          @Override public void onClick(DialogInterface dialogInterface, int i) {
            dialogInterface.cancel();
          }
        })
        .setCancelable(true).create().show();
  }

  public static void openProgressDialog(Activity parentActivity) {
    if(progressDialog != null) {
      progressDialog.dismiss();
    }
    progressDialog = new ProgressDialog(parentActivity);
    progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
    progressDialog.setIndeterminate(true);
    progressDialog.setMessage(parentActivity.getString(R.string.settings_import_running));
    progressDialog.show();
  }

  public static void closeProgressDialog(Activity parentActivity, JSONImporter.ImportResultReport result) {
    progressDialog.dismiss();
    progressDialog = null;
    final Resources res = parentActivity.getResources();
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
      Toast.makeText(parentActivity, message, Toast.LENGTH_LONG).show();
    } else {
      Toast.makeText(parentActivity, R.string.settings_import_failed, Toast.LENGTH_LONG).show();
    }
  }

  public static void showProgressUpdate(int currentBook, int totalBooks) {
    Log.d(TAG, String.format("import progress: %d out of %d", currentBook, totalBooks));
    if(progressDialog != null) {
      progressDialog.setIndeterminate(false);
      progressDialog.setMax(totalBooks);
      progressDialog.setProgress(currentBook);
    }
  }

  private static boolean wasGrantedPermission(@NotNull final String permissionName, String[] permissions, int[] grantResults) {
    for(int i = 0; i < permissions.length; i++) {
      if(permissionName.equals(permissions[i])) {
        final boolean wasGranted = i < grantResults.length && grantResults[i] == PackageManager.PERMISSION_GRANTED;
        return wasGranted;
      }
    }
    return false;
  }
}
