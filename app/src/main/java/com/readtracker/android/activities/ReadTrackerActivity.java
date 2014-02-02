package com.readtracker.android.activities;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.ActionBarActivity;
import android.util.TypedValue;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.readtracker.android.ApplicationReadTracker;
import com.readtracker.android.support.ReadTrackerUser;

/**
 * Base of read tracker's activities.
 * <p/>
 * Hides the title from child applications.
 */
public class ReadTrackerActivity extends ActionBarActivity {
  protected final String TAG = this.getClass().getName();
  private ApplicationReadTracker mApplication;

  private static Typeface mRoboto;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    mApplication = (ApplicationReadTracker) getApplication();
    requestWindowFeatures();

    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
  }

  /**
   * Lazily loads the custom typeface
   *
   * @return
   */
  public Typeface getRoboto() {
    if(mRoboto == null) {
      mRoboto = Typeface.createFromAsset(getAssets(), "Roboto-Light.ttf");
    }
    return mRoboto;
  }

  /**
   * Applies the custom font to a given text view.
   *
   * @param textViewId id of text view to apply custom font on
   */
  protected void applyRoboto(int textViewId) {
    TextView textView = (TextView) findViewById(textViewId);
    textView.setTypeface(getRoboto());
  }

  public final ApplicationReadTracker getApp() {
    return mApplication;
  }

  /**
   * Gets the currently logged in user.
   *
   * @return the currently logged in user or null.
   */
  public ReadTrackerUser getCurrentUser() {
    return getApp().getCurrentUser();
  }

  /**
   * Gets the readmill id of the current user.
   *
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
    ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
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
   *
   * @param toastMessage Message to be displayed
   */
  protected void toast(String toastMessage) {
    Toast.makeText(this, toastMessage, Toast.LENGTH_SHORT).show();
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
   * Sets the content of an image view to the content of an URL through the
   * globally available drawable manager.
   *
   * @param imageView Image view to set image for.
   * @param url       The url of the image to set. Will be downloaded if not cached.
   */
  protected void setImageViewUrl(ImageView imageView, String url) {
    ApplicationReadTracker.getDrawableManager().fetchDrawableOnThread(url, imageView);
  }

  /**
   * Converts a device independent pixel value to pixels.
   *
   * @param dpValue The value in DIP
   * @return the value in pixels
   */
  public int getPixels(int dpValue) {
    return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dpValue, getResources().getDisplayMetrics());
  }
}
