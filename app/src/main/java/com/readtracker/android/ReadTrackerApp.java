package com.readtracker.android;

import android.app.Application;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.readtracker.android.db.DatabaseHelper;
import com.readtracker.android.db.DatabaseManager;
import com.readtracker.android.support.ApplicationSettingsHelper;
import com.squareup.otto.Bus;

public class ReadTrackerApp extends Application {
  private static final String TAG = ReadTrackerApp.class.getName();

  public static final String PREFERENCES_FILE_NAME = "ReadTrackerPrefFile";

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

  private Bus mBus;

  // Convenient access to app settings
  private ApplicationSettingsHelper mAppSettingsHelper;
  private DatabaseManager mDatabaseManager;

  public ReadTrackerApp() {

  }

  /** Gets the application class from a Context. */
  public static ReadTrackerApp from(Context context) {
    return (ReadTrackerApp) context.getApplicationContext();
  }

  /** Get access to the shared event bus. */
  public Bus getBus() {
    if(mBus == null) {
      mBus = new Bus();
    }
    return mBus;
  }

  @Override
  public void onCreate() {
    super.onCreate();

    Log.i(TAG, "Initializing ReadTrackerApp");

    // TODO replace with settings helper
    mPreferences = getSharedPreferences(PREFERENCES_FILE_NAME, MODE_PRIVATE);

    // TODO add to settings helper
    // Flag first time starting the app
    mFirstTimeFlag = mPreferences.getBoolean(KEY_FIRST_TIME, true);

    // Setup persistence
    mDatabaseHelper = new DatabaseHelper(this);

    mDatabaseManager = new DatabaseManager(mDatabaseHelper);

    // Assign singleton
    mInstance = this;
  }

  public DatabaseManager getDatabaseManager() {
    return mDatabaseManager;
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
    if(mProgressDialog != null) {
      try {
        mProgressDialog.dismiss();
      } catch(IllegalArgumentException ignored) {
        // We sometimes end up here if the activity that started the progress dialog is no longer
        // attached to the view manager.
      }
    }
    mProgressDialog = null;
  }

  public ApplicationSettingsHelper getAppSettings() {
    if(mAppSettingsHelper == null) {
      mAppSettingsHelper = new ApplicationSettingsHelper(getPreferences());
    }
    return mAppSettingsHelper;
  }

  public SharedPreferences getPreferences() {
    return getSharedPreferences(PREFERENCES_FILE_NAME, MODE_PRIVATE);
  }
}
