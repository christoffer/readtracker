package com.readtracker.android;

import android.app.Application;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.j256.ormlite.dao.Dao;
import com.readtracker.android.db.Book;
import com.readtracker.android.db.DatabaseHelper;
import com.readtracker.android.db.LocalHighlight;
import com.readtracker.android.db.LocalReading;
import com.readtracker.android.db.LocalSession;
import com.readtracker.android.db.Quote;
import com.readtracker.android.db.Session;

import java.sql.SQLException;

public class ReadTrackerApp extends Application {
  private static final String TAG = ReadTrackerApp.class.getName();

  public static final String PREF_FILE_NAME = "ReadTrackerPrefFile";

  // Keys for saving information in Settings
  public static final String KEY_FIRST_TIME = "first-time";

  // Access to application instance
  private static ReadTrackerApp mInstance = null;

  // Store reference to allow cheaper look-ups
  private SharedPreferences mPreferences;

  // Access to database
  private DatabaseHelper mDatabaseHelper;

  // Keep a reference to the progress bar in a global state, so we can dismiss
  // it from any activity
  private ProgressDialog mProgressDialog;

  // Flag for first time usage of ReadTracker
  private boolean mFirstTimeFlag;

  public ReadTrackerApp() {
  }

  @Override
  public void onCreate() {
    super.onCreate();
    initApplication();
  }

  public DatabaseHelper getDatabaseHelper() {
    return mDatabaseHelper;
  }

  private void initApplication() {
    Log.i(TAG, "Initializing application");
    mPreferences = getSharedPreferences(PREF_FILE_NAME, MODE_PRIVATE);

    // Flag first time starting the app
    mFirstTimeFlag = mPreferences.getBoolean(KEY_FIRST_TIME, true);

    // Setup persistence
    mDatabaseHelper = new DatabaseHelper(this);

    // Assign singleton
    mInstance = this;
  }

  public static SharedPreferences getApplicationPreferences() {
    return mInstance.mPreferences;
  }

  /**
   * Gets the flag that indicates whether or not this is the first time the user
   * is starting readtracker or not.
   *
   * @return true if this is the first time, otherwise false
   */
  public boolean getFirstTimeFlag() {
    return mFirstTimeFlag;
  }

  /**
   * Remove the flag for first time usage.
   */
  public void removeFirstTimeFlag() {
    mPreferences.edit().putBoolean(KEY_FIRST_TIME, false).commit();
    mFirstTimeFlag = false;
  }

  /**
   * Shows a global progress dialog.
   * <p/>
   * This is useful when switching between Activities before a progress dialog
   * has been completed.
   *
   * @param ctx     Context to show dialog in
   * @param message Message to show
   */
  public void showProgressDialog(Context ctx, String message) {
    clearProgressDialog();
    mProgressDialog = ProgressDialog.show(ctx, "", message);
  }

  public void clearProgressDialog() {
    if (mProgressDialog != null) {
      try {
        mProgressDialog.dismiss();
      } catch (IllegalArgumentException ignored) {
        // We sometimes end up here if the activity that started the progress dialog is no longer
        // attached to the view manager.
      }
    }
    mProgressDialog = null;
  }

  private static void assertInstance() {
    if (mInstance == null) {
      throw new RuntimeException("Application ReadTracker has not been initialized");
    }
  }

  public static Dao<Quote, Integer> getQuoteDao() throws SQLException {
    return mInstance.getDatabaseHelper().getQuoteDao();
  }

  public static Dao<Book, Integer> getBookDao() throws SQLException {
    return mInstance.getDatabaseHelper().getBookDao();
  }

  public static Dao<Session, Integer> getSessionDao() throws SQLException {
    return mInstance.getDatabaseHelper().getSessionDao();
  }

  public static Dao<LocalReading, Integer> getReadingDao() throws SQLException {
    assertInstance();
    return mInstance.getDatabaseHelper().getReadingDao();
  }

  public static Dao<LocalSession, Integer> getLocalSessionDao() throws SQLException {
    assertInstance();
    return mInstance.getDatabaseHelper().getLocalSessionDao();
  }

  public static Dao<LocalHighlight, Integer> getHighlightDao() throws SQLException {
    assertInstance();
    return mInstance.getDatabaseHelper().getHighlightDao();
  }
}
