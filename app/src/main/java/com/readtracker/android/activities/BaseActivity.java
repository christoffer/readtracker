package com.readtracker.android.activities;

import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.util.TypedValue;
import android.widget.Toast;

import com.readtracker.BuildConfig;
import com.readtracker.android.ReadTrackerApp;
import com.readtracker.android.db.DatabaseManager;
import com.readtracker.android.support.ApplicationSettingsHelper;
import com.squareup.otto.Bus;

/**
 * Base activity
 */
public abstract class BaseActivity extends ActionBarActivity {
  private ReadTrackerApp mApplication;
  private Bus mBus;
  private DatabaseManager mDatabaseManager;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    mApplication = ReadTrackerApp.from(this);
    requestWindowFeatures();

    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    mBus = getApp().getBus();
    mDatabaseManager = getApp().getDatabaseManager();
  }

  ReadTrackerApp getApp() {
    return mApplication;
  }

  @Override
  protected void onResume() {
    super.onResume();
    if(BuildConfig.DEBUG) {
      Log.v(getClass().getSimpleName(), "Registering on bus");
    }
    mBus.register(this);
  }

  @Override
  protected void onPause() {
    super.onPause();
    if(BuildConfig.DEBUG) {
      Log.v(getClass().getSimpleName(), "Unregister from bus");
    }
    mBus.unregister(this);
  }

  /**
   * Returns the current application settings.
   */
  ApplicationSettingsHelper getAppSettings() {
    return getApp().getAppSettings();
  }

  /**
   * Returns a references to the shared preferences.
   */
  public SharedPreferences getPreferences() {
    return getApp().getPreferences();
  }

  /**
   * Override this method to change what features that gets requested for the activity.
   */
  @SuppressWarnings("EmptyMethod")
  protected void requestWindowFeatures() {
    // Nothing
  }

  /**
   * Display a short toast message to the user
   *
   * @param toastMessageId String resources to be displayed
   */
  protected void toast(int toastMessageId) {
    toast(getString(toastMessageId));
  }

  protected void toast(String message) {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
  }

  /**
   * Display a long toast message to the user
   *
   * @param toastMessage Message to be displayed
   */
  protected void toastLong(String toastMessage) {
    Toast.makeText(this, toastMessage, Toast.LENGTH_LONG).show();
  }

  /**
   * Converts a device independent pixel value to pixels.
   *
   * @param dpValue The value in DIP
   * @return the value in pixels
   */
  int getPixels(int dpValue) {
    return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dpValue, getResources().getDisplayMetrics());
  }

  /**
   * Returns the application global event bus.
   */
  public Bus getBus() {
    return mBus;
  }

  /**
   * Returns the application global database manager.
   */
  public DatabaseManager getDatabaseManager() {
    return mDatabaseManager;
  }

  /**
   * Convenient method for posting to the global bus from an activity.
   */
  protected void postEvent(Object event) {
    mBus.post(event);
  }
}
