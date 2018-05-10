package com.readtracker.android.tasks;

import android.os.AsyncTask;
import android.util.Log;

import com.readtracker.android.db.DatabaseManager;
import com.readtracker.android.db.export.JSONImporter;

import java.io.File;
import java.lang.ref.WeakReference;

/**
 * Task that imports a ReadTracker export file and notifies the caller of progress and completion.
 */
public class ImportReadTrackerFileTask extends AsyncTask<File, Void, JSONImporter.ImportResultReport> {
  private final String TAG = ImportReadTrackerFileTask.class.getName();
  private final WeakReference<DatabaseManager> dbManager;
  private final WeakReference<ResultListener> listener;
  private Exception exception;

  private ImportReadTrackerFileTask(DatabaseManager databaseManager, ResultListener listener) {
    this.dbManager = new WeakReference<>(databaseManager);
    this.listener = new WeakReference<>(listener);
  }

  public static void importFile(File fromFile, DatabaseManager toDatabaseManager, ResultListener listener) {
    (new ImportReadTrackerFileTask(toDatabaseManager, listener)).execute(fromFile);
  }

  @Override protected void onPreExecute() {
    final ResultListener listener = this.listener.get();
    if (listener != null) {
      listener.onImportStart();
    }
  }

  @Override protected JSONImporter.ImportResultReport doInBackground(File... args) {
    final File fileToImport = args[0];
    final DatabaseManager dbManager = this.dbManager.get();
    if(dbManager != null) {
      try {
        JSONImporter importer = new JSONImporter(dbManager);
        return importer.importFile(fileToImport);
      } catch(Exception e) {
        Log.e(TAG, "Error while importing file", exception);
        exception = e;
        return null;
      }
    } else {
      Log.d(TAG, "Database manager went away before starting the task. Quitting...");
      return null;
    }
  }

  @Override protected void onPostExecute(JSONImporter.ImportResultReport report) {
    final ResultListener listener = this.listener.get();
    if (listener == null) {
      Log.v(TAG, "ResultListener went away while importing");
      return;
    }

    if(exception != null) {
      Log.v(TAG, "Background returned with exception");
      listener.onImportComplete(null);
      return;
    }


    if (report == null) {
      Log.v(TAG, "Background task did not return a report, so it probably failed");
      listener.onImportComplete(null);
      return;
    }

    listener.onImportComplete(report);
  }

  public interface ResultListener {
    void onImportStart();
    void onImportComplete(JSONImporter.ImportResultReport importReport);
  }
}