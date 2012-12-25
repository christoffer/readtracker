package com.readtracker_beta;

import android.app.Application;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.util.Log;
import com.j256.ormlite.dao.Dao;
import com.readmill.api.Environment;
import com.readmill.api.ReadmillWrapper;
import com.readmill.api.Token;
import com.readmill.api.TokenChangeListener;
import com.readtracker_beta.activities.WelcomeActivity;
import com.readtracker_beta.db.DatabaseHelper;
import com.readtracker_beta.db.LocalHighlight;
import com.readtracker_beta.db.LocalReading;
import com.readtracker_beta.db.LocalSession;
import com.readtracker_beta.support.ReadmillApiHelper;
import com.readtracker_beta.thirdparty.DrawableManager;
import com.readtracker_beta.support.ReadTrackerUser;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Shared state for the entire application
 */
public class ApplicationReadTracker extends Application implements TokenChangeListener {
  private static final String TAG = ApplicationReadTracker.class.getName();

  public static final String PREF_FILE_NAME = "ReadTrackerPrefFile";

  // Readmill integration
  ReadmillApiHelper mReadmillApiHelper;

  // Keys for saving information in Settings
  public static final String KEY_TOKEN = "json:token:rt2";
  public static final String KEY_CURRENT_USER = "json:currentuser:rt2";
  public static final String KEY_FIRST_TIME = "first-time";

  // Access to application instance
  private static ApplicationReadTracker mInstance = null;

  // Store reference to allow cheaper look-ups
  private SharedPreferences mPreferences;

  // Global drawable manager for keeping cached images across activities
  private DrawableManager mDrawableManager;

  // Access to database
  private DatabaseHelper mDatabaseHelper;

  // Currently logged in user
  private ReadTrackerUser mCurrentUser;

  // Keep a reference to the progress bar in a global state, so we can dismiss
  // it from any activity
  private ProgressDialog mProgressDialog;

  // Flag for first time usage of ReadTracker
  private boolean mFirstTimeFlag;

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

    // Flag first time starting the app
    mFirstTimeFlag = mPreferences.getBoolean(KEY_FIRST_TIME, true);

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

    /*
    NOTE Readmill properties are not included in the repo.

    Properties are read from the file: <PROJECT-ROOT>/assets/readmill.properties

    The following properties are used:
       client-id       - The Readmill client id (from readmill.com/you/apps)
       client-secret   - The Readmill client secret (from readmill.com/you/apps)
       readmill-domain - The base domain of the readmill server to use (default: "readmill.com")
       use-https       - Whether or not to use https in API requests (default: "true")

     */
    Resources resources = this.getResources();
    AssetManager assetManager = resources.getAssets();

    String readmillDomain;
    boolean useHTTPS;
    String clientId;
    String clientSecret;

    try {
      InputStream inputStream = assetManager.open("readmill.properties");
      Properties properties = new Properties();
      properties.load(inputStream);

      readmillDomain = properties.getProperty("readmill-domain", "readmill.com");
      useHTTPS = properties.getProperty("use-https", "true").equals("true");
      clientId = properties.getProperty("client-id");
      clientSecret = properties.getProperty("client-secret");

      if(clientId == null)
        throw new RuntimeException("readmill.properties did not include property: 'client-id'");
      if(clientSecret == null)
        throw new RuntimeException("readmill.properties did not include property: 'client-secret'");
    } catch(IOException e) {
      Log.e(TAG, "Could not load properties file: /assets/readmill.properties", e);
      throw new RuntimeException("Failed to load readmill properties file");
    }

    // The setup for Readmill is fetched from a resource file. Check sample_credentials.xml for how to set it up.
    environment = new Environment("api." + readmillDomain, "m." + readmillDomain, useHTTPS);
    wrapper = new ReadmillWrapper(clientId, clientSecret, environment);

    URI redirectURI = URI.create(getString(R.string.readmill_callback));
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

    // Clear local state
    mCurrentUser = null;
    mReadmillApiHelper.setToken(null);
  }

  public void signOut() {
    clearSettings();
    Intent intentWelcome = new Intent(this, WelcomeActivity.class);
    intentWelcome.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    intentWelcome.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
    startActivity(intentWelcome);
  }
}
