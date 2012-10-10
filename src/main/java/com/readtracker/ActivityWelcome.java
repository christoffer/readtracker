package com.readtracker;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;

/**
 * Splash screen that lets a user sign in or sign up to Readmill
 */
public class ActivityWelcome extends ReadTrackerActivity {

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Log.d(TAG, "Start " + TAG);

    setContentView(R.layout.activity_welcome);

    Button btnSignUp = (Button) findViewById(R.id.btnSignUp);
    Button btnAuthorization = (Button) findViewById(R.id.btnAuthorization);
    Button buttonOffline = (Button) findViewById(R.id.buttonOffline);

    btnSignUp.setOnClickListener(new OnClickListener() {
      @Override public void onClick(View view) {
        onCreateAccountClicked();
      }
    });

    btnAuthorization.setOnClickListener(new OnClickListener() {
      @Override public void onClick(View view) {
        onSignInClicked();
      }
    });

    buttonOffline.setOnClickListener(new OnClickListener() {
      @Override public void onClick(View view) {
        onOfflineClicked();
      }
    });
  }

  @Override
  protected void requestWindowFeatures() {
    requestWindowFeature(Window.FEATURE_NO_TITLE);
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    boolean validRequestCode = (
      requestCode == ActivityCodes.REQUEST_SIGN_IN ||
      requestCode == ActivityCodes.REQUEST_CREATE_ACCOUNT
    );

    if(validRequestCode && (resultCode == RESULT_OK)) {
      Intent intentWelcome = new Intent(this, ActivityHome.class);
      intentWelcome.putExtra(IntentKeys.SIGNED_IN, true);
      startActivity(intentWelcome);
      finish();
    }
  }

  private void onOfflineClicked() {
    toast("Not implemented");
  }

  private void onSignInClicked() {
    Log.d(TAG, "clicked Sign in");
    Intent intentWebView = new Intent(this, ActivitySignInAndAuthorize.class);
    intentWebView.putExtra(IntentKeys.WEB_VIEW_ACTION, IntentKeys.WEB_VIEW_SIGN_IN_AND_AUTHORIZE);
    startActivityForResult(intentWebView, ActivityCodes.REQUEST_SIGN_IN);
  }

  private void onCreateAccountClicked() {
    Log.d(TAG, "clicked Create Account");
    Intent intentWebView = new Intent(this, ActivitySignInAndAuthorize.class);
    intentWebView.putExtra(IntentKeys.WEB_VIEW_ACTION, IntentKeys.WEB_VIEW_CREATE_ACCOUNT);
    startActivityForResult(intentWebView, ActivityCodes.REQUEST_CREATE_ACCOUNT);
  }
}
