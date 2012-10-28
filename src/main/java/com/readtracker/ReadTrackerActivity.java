package com.readtracker;

import android.content.Context;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.readtracker.readmill.ReadmillApiHelper;
import com.readtracker.value_objects.ReadTrackerUser;

/**
 * Base of read tracker's activities.
 *
 * Hides the title from child applications.
 */
public class ReadTrackerActivity extends FragmentActivity {
  protected final String TAG = this.getClass().getName();
  private ApplicationReadTracker mApplication;

  private Typeface mRobotoThin;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    mApplication = (ApplicationReadTracker) getApplication();
    requestWindowFeatures();
  }

  /**
   * Lazily loads the roboto thin typeface
   * @return
   */
  public Typeface getRobotoThin() {
    if(mRobotoThin == null) {
      mRobotoThin = Typeface.createFromAsset(getAssets(), "Roboto-Thin.ttf");
    }
    return mRobotoThin;
  }

  /**
   * Applies the Roboto Thin font to a given text view.
   * @param textViewId id of text view to apply roboto thin font on
   */
  protected void applyRobotoThin(int textViewId) {
    TextView textView = (TextView) findViewById(textViewId);
    textView.setTypeface(getRobotoThin());
  }

  public final ApplicationReadTracker getApp() {
    return mApplication;
  }

  /**
   * Gets the currently logged in user.
   * @return the currently logged in user or null.
   */
  public ReadTrackerUser getCurrentUser() {
    return getApp().getCurrentUser();
  }

  /**
   * Gets the readmill id of the current user.
   * @return the id of the current user or -1 if no user is signed in.
   */
  public long getCurrentUserId() {
    ReadTrackerUser currentUser = getCurrentUser();
    return currentUser == null ? -1 : currentUser.getReadmillId();
  }

  /**
   * @return Internet connectivity status
   */
  protected boolean isNetworkAvailable() {
    ConnectivityManager connectivityManager  = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
    NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
    return activeNetworkInfo != null;
  }

  /**
   * Override this method to change what features that gets requested for the activity.
   */
  protected void requestWindowFeatures() {
    requestWindowFeature(Window.FEATURE_NO_TITLE);
  }

  /**
   * Display a short toast message to the user
   * @param toastMessage Message to be displayed
   */
  protected void toast(String toastMessage) {
    Toast.makeText(this, toastMessage, Toast.LENGTH_SHORT).show();
  }

  /**
   * Display a long toast message to the user
   * @param toastMessage Message to be displayed
   */
  protected void toastLong(String toastMessage) {
    Toast.makeText(this, toastMessage, Toast.LENGTH_LONG).show();
  }

  /**
   * Sets the content of an image view to the content of an URL through the
   * globally available drawable manager.
   *
   * @param imageView Image view to set image for.
   * @param url The url of the image to set. Will be downloaded if not cached.
   */
  protected void setImageViewUrl(ImageView imageView, String url) {
    ApplicationReadTracker.getDrawableManager().fetchDrawableOnThread(url, imageView);
  }

  /**
   * Delegate access to the application global Readmill object.
   * @return the current readmill api helper or null
   */
  protected ReadmillApiHelper readmillApi() {
    ReadmillApiHelper readmillApiHelper = mApplication.getReadmillApiHelper();
    if(readmillApiHelper == null) {
      Log.e(TAG, "CRITICAL ! The connection to Readmill was not initialized.");
      throw new RuntimeException("CRITICAL ! The connection to Readmill was not initialized.");
    }
    return readmillApiHelper;
  }
}
