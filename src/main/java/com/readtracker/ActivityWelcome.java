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
public class ActivityWelcome extends ReadTrackerActivity implements OnClickListener {
  private static final int RQ_AUTHORIZE = 1;
  private static final int RQ_CREATE_ACCOUNT = 2;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Log.d(TAG, "Start " + TAG);

    setContentView(R.layout.welcome);

    Button btnSignUp = (Button) findViewById(R.id.btnSignUp);
    Button btnAuthorization = (Button) findViewById(R.id.btnAuthorization);

    btnSignUp.setOnClickListener(this);
    btnAuthorization.setOnClickListener(this);
  }

  @Override
  protected void requestWindowFeatures() {
    requestWindowFeature(Window.FEATURE_NO_TITLE);
  }

  @Override
  protected void onResume() {
    super.onResume();
    Log.d(TAG, "Resuming " + TAG);
  }

  public void onClick(View clickedView) {
    Intent intentWebview = new Intent(this, ActivitySignInAndAuthorize.class);
    int resultCode = 0;

    switch(clickedView.getId()) {
      case R.id.btnAuthorization:
        Log.d(TAG, "Authorize Application");
        intentWebview.putExtra(IntentKeys.WEB_VIEW_ACTION, IntentKeys.WEB_VIEW_SIGN_IN_AND_AUTHORIZE);
        resultCode = RQ_AUTHORIZE;
        break;
      case R.id.btnSignUp:
        Log.d(TAG, "Create Readmill Account");
        intentWebview.putExtra(IntentKeys.WEB_VIEW_ACTION, IntentKeys.WEB_VIEW_CREATE_ACCOUNT);
        resultCode = RQ_CREATE_ACCOUNT;
        break;
    }

    startActivityForResult(intentWebview, resultCode);
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    boolean validRequestCode = (requestCode == RQ_AUTHORIZE || requestCode == RQ_CREATE_ACCOUNT);
    if(validRequestCode && (resultCode == RESULT_OK)) {
      Intent intentWelcome = new Intent(this, ActivityHome.class);
      intentWelcome.putExtra(IntentKeys.SIGNED_IN, true);
      startActivity(intentWelcome);
      finish();
    }
  }
}
