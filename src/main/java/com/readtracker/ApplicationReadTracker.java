package com.readtracker;

import android.app.ActivityManager;
import android.app.Application;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import com.j256.ormlite.dao.Dao;
import com.readmill.api.Environment;
import com.readmill.api.ReadmillWrapper;
import com.readmill.api.Token;
import com.readmill.api.TokenChangeListener;
import com.readtracker.db.*;
import com.readtracker.readmill.ReadmillApiHelper;
import com.readtracker.thirdparty.DrawableManager;
import com.readtracker.value_objects.ReadTrackerUser;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;
import java.sql.SQLException;
import java.util.List;

/**
 * Shared state for the entire application
 */
public class ApplicationReadTracker extends Application implements TokenChangeListener {
  public static final String PREF_FILE_NAME = "ReadTrackerPrefFile";

  // Readmill integration
  ReadmillApiHelper mReadmillApiHelper;

  // Keys for saving information in Settings
  public static final String KEY_TOKEN = "json:token:rt2";
  public static final String KEY_CURRENT_USER = "json:currentuser:rt2";

  // Logging
  private static final String TAG = ApplicationReadTracker.class.getName();

  // Access to application instance
  private static ApplicationReadTracker mInstance = null;

  private SharedPreferences mPreferences;
  private DrawableManager mDrawableManager;
  private DatabaseHelper mDatabaseHelper;

  private ReadTrackerUser mCurrentUser;

  private ProgressDialog mProgressDialog;

  public ApplicationReadTracker() { }

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

    // Get previous login information
    Token token = getStoredToken();

    mCurrentUser = loadReadmillUser();

    setupReadmillApi(token);

    // Setup persistence
    mDatabaseHelper = new DatabaseHelper(this);

    // Assign singleton
    mInstance = this;
  }

  public ReadmillApiHelper getReadmillApiHelper() {
    return mReadmillApiHelper;
  }

  private void setupReadmillApi(Token token) {
    Environment environment;
    ReadmillWrapper wrapper;

    // The setup for Readmill is fetched from a resource file. Check sample_credentials.xml for how to set it up.
    String readmillDomain = getString(R.string.readmill_domain);
    boolean useHttps = getResources().getBoolean(R.bool.readmill_https);

    environment = new Environment("api." + readmillDomain, "m." + readmillDomain, useHttps);
    wrapper = new ReadmillWrapper(getString(R.string.client_id), getString(R.string.client_secret), environment);

    URI redirectURI = URI.create(getString(R.string.redirect_uri));
    wrapper.setRedirectURI(redirectURI);
    wrapper.setScope("non-expiring");

    Log.i(TAG, "Setting up wrapper for environment: " + environment.toString());
    Log.i(TAG, "                  with credentials: " + wrapper.getClientId() + ":" + wrapper.getClientSecret());
    Log.i(TAG, "Using stored token: " + (token == null ? "NULL" : token.toString()));

    wrapper.setToken(token);
    wrapper.setTokenChangeListener(this);

    mReadmillApiHelper = new ReadmillApiHelper(wrapper);
  }

  public static SharedPreferences getApplicationPreferences() {
    return mInstance.mPreferences;
  }

  /**
   * Gets the stored token
   *
   * @return The stored Token or null
   */
  protected Token getStoredToken() {
    Log.d(TAG, "Attempting to load token from preferences");
    String storedToken = mPreferences.getString(KEY_TOKEN, null);
    return storedToken == null ? null : new Token(storedToken);
  }

  /**
   * Saves the token information to preferences.
   * Clears the previously stored token if the given token is null.
   *
   * @param token Token to save
   */
  protected void storeToken(Token token) {
    SharedPreferences.Editor editor = mPreferences.edit();
    if(token == null) {
      editor.remove(KEY_TOKEN);
    } else {
      editor.putString(KEY_TOKEN, token.getAccessToken());
    }
    editor.commit();
  }

  /**
   * Triggers a fetch of the current user from Readmill and
   * stores the new token.
   */
  @Override
  public void onTokenChanged(Token token) {
    Log.i(TAG, "Got new token: " + (token == null ? "NULL" : token.toString()));

    if(token != null) {
      // Update the current user from Readmill with the new token
      JSONObject jsonUser = mReadmillApiHelper.getCurrentUser();
      try {
        mCurrentUser = new ReadTrackerUser(jsonUser);
        Log.i(TAG, "Received new user: " + mCurrentUser.toString());
      } catch(JSONException e) {
        Log.w(TAG, "Failed to retrieve the new current user from Readmill", e);
        mCurrentUser = null;
      }
    }

    storeToken(token);
    storeReadmillUser(mCurrentUser);
  }

  /**
   * Returns true if the activity is being hidden due to another app taking
   * over or false otherwise.
   *
   * @param context Context to
   * @return True of the application is brought to foreground, false otherwise
   */
  public static boolean isApplicationBroughtToBackground(final Context context) {
    ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
    List<ActivityManager.RunningTaskInfo> tasks = am.getRunningTasks(1);
    if(!tasks.isEmpty()) {
      ComponentName topActivity = tasks.get(0).topActivity;
      if(!topActivity.getPackageName().equals(context.getPackageName())) {
        return true;
      }
    }

    return false;
  }

  /**
   * Globally drawable manager (share caching of Drawables)
   *
   * @return the global DrawableManager instance
   */
  public static DrawableManager getDrawableManager() {
    if(mInstance == null) {
      throw new RuntimeException("getDrawableManager() called before Application initialized");
    }

    if(mInstance.mDrawableManager == null) {
      mInstance.mDrawableManager = new DrawableManager();
      mInstance.mDrawableManager.persistDrawables(true);
    }

    return mInstance.mDrawableManager;
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
      mProgressDialog.dismiss();
    }
    mProgressDialog = null;
  }

  private static void assertInstance() {
    if(mInstance == null) {
      throw new RuntimeException("Application ReadTracker has not been initialized");
    }
  }

  public static Dao<LocalReading, Integer> getReadingDao() throws SQLException {
    assertInstance();
    return mInstance.getDatabaseHelper().getReadingDao();
  }

  public static Dao<LocalSession, Integer> getSessionDao() throws SQLException {
    assertInstance();
    return mInstance.getDatabaseHelper().getSessionDao();
  }

  public static Dao<LocalHighlight, Integer> getHighlightDao() throws SQLException {
    assertInstance();
    return mInstance.getDatabaseHelper().getHighlightDao();
  }

  public static ReadmillApiHelper getReadmillApi() {
    assertInstance();
    return mInstance.getReadmillApiHelper();
  }

  /**
   * Load a stored user from the preferences
   *
   * @return The stored user or null if the data was empty or invalid
   */
  private ReadTrackerUser loadReadmillUser() {
    try {
      String currentUserJSON = mPreferences.getString(KEY_CURRENT_USER, "");
      if(currentUserJSON.length() > 0) {
        JSONObject jsonLoadedUser = new JSONObject(currentUserJSON);
        return new ReadTrackerUser(jsonLoadedUser);
      } else {
        Log.i(TAG, "Did not find stored current user");
      }
    } catch(JSONException e) {
      Log.w(TAG, "Currently stored user is invalid JSON", e);
    }
    return null;
  }

  /**
   * Persist the given user to the application preferences file.
   * <p/>
   * The current user will be cleared if this method is called with null.
   *
   * @param user ReadTrackerUser instance to save
   */
  public void storeReadmillUser(ReadTrackerUser user) {
    SharedPreferences.Editor editor = mPreferences.edit();
    if(user == null) {
      editor.remove(KEY_CURRENT_USER);
    } else {
      editor.putString(KEY_CURRENT_USER, user.toJSON());
    }
    editor.commit();
  }

  public final ReadTrackerUser getCurrentUser() {
    return mCurrentUser;
  }

  /**
   * Clears both the token and the user from settings
   */
  public void clearSettings() {
    SharedPreferences.Editor editor = mPreferences.edit();
    editor.remove(KEY_CURRENT_USER);
    editor.remove(KEY_TOKEN);
    editor.commit();
  }

  /**
   * Checks if a user is currently signed in to Readmill.
   * This is true if a user and a token is stored in the preferences.
   *
   * @return True if user and token are available
   */
  public boolean userSignedIn() {
    return mCurrentUser != null && mReadmillApiHelper.hasToken();
  }
}
