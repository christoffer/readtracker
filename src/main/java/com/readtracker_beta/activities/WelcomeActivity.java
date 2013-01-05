package com.readtracker_beta.activities;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.PreparedQuery;
import com.j256.ormlite.stmt.Where;
import com.readtracker_beta.ApplicationReadTracker;
import com.readtracker_beta.IntentKeys;
import com.readtracker_beta.R;
import com.readtracker_beta.db.LocalReading;

import java.sql.SQLException;
import java.util.ArrayList;

/**
 * Splash screen that lets a user sign in or sign up to Readmill
 */
public class WelcomeActivity extends ReadTrackerActivity {

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Log.d(TAG, "Start " + TAG);

    setContentView(R.layout.activity_welcome);

    Button buttonConnectToReadmill = (Button) findViewById(R.id.buttonConnectToReadmill);
    Button buttonOffline = (Button) findViewById(R.id.buttonOffline);

    Button moreAboutReadmill = (Button) findViewById(R.id.buttonMoreAboutReadmill);
    Button moreAboutReadTracker = (Button) findViewById(R.id.buttonMoreAboutReadTracker);

    applyRoboto(R.id.textReadTracker);

    buttonConnectToReadmill.setOnClickListener(new OnClickListener() {
      @Override public void onClick(View view) {
        onAuthorizeClicked();
      }
    });

    buttonOffline.setOnClickListener(new OnClickListener() {
      @Override public void onClick(View view) {
        onOfflineClicked();
      }
    });

    moreAboutReadmill.setOnClickListener(new OnClickListener() {
      @Override public void onClick(View view) {
        visitWebPage("https://readmill.com/about");
      }
    });

    moreAboutReadTracker.setOnClickListener(new OnClickListener() {
      @Override public void onClick(View view) {
        visitWebPage("http://readtracker.com");
      }
    });

    if(getApp().getFirstTimeFlag()) {
      final ViewGroup scrollView = (ViewGroup) findViewById(R.id.scrollviewIntroduction);
      final Button buttonStart = (Button) findViewById(R.id.buttonStartUsing);

      buttonStart.setOnClickListener(new OnClickListener() {
        @Override public void onClick(View view) {
          Animation disappear = AnimationUtils.loadAnimation(WelcomeActivity.this, R.anim.fade_out);
          disappear.setAnimationListener(new Animation.AnimationListener() {
            @Override public void onAnimationStart(Animation animation) {}
            @Override public void onAnimationRepeat(Animation animation) {}
            @Override public void onAnimationEnd(Animation animation) {
              // Can't get rid of the view by just setting the visibility to gone (clicks are still registered)
              // So we remove it all together instead.
              ((ViewGroup) scrollView.getParent()).removeView(scrollView);
            }
          });
          scrollView.startAnimation(disappear);
        }
      });

      Animation appear = AnimationUtils.loadAnimation(this, R.anim.fade_in);
      appear.setStartOffset(500);
      scrollView.startAnimation(appear);
      scrollView.setVisibility(View.VISIBLE);
    }
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
      // Initiate a search for device only readings
      //noinspection unchecked
      (new CheckAnonymousReadingsASync()).execute();
    }
  }

  private void onOfflineClicked() {
    Log.d(TAG, "clicked Start in offline mode");
    exitToHomeScreen();
  }

  private void onAuthorizeClicked() {
    Log.d(TAG, "clicked Authorize");
    Intent intentWebView = new Intent(this, OAuthActivity.class);
    startActivityForResult(intentWebView, ActivityCodes.REQUEST_SIGN_IN);
  }

  private void onCheckAnonymousReadings(int anonymousReadingsCount) {
    Log.d(TAG, "Found " + anonymousReadingsCount + " anonymous readings");
    final long readmillUserId = getCurrentUserId();
    if(anonymousReadingsCount > 0) {
      // pop dialog and ask the user if s/he wants to
      DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
        @Override public void onClick(DialogInterface dialog, int which) {
          switch(which) {
            case DialogInterface.BUTTON_POSITIVE:
              associateAnonymousReadings(readmillUserId);
              break;
            case DialogInterface.BUTTON_NEGATIVE:
              exitToHomeScreen();
              break;
          }
        }
      };

      AlertDialog.Builder builder = new AlertDialog.Builder(this);
      String question = String.format(getString(R.string.associate_anonymous_readings), anonymousReadingsCount);
      builder.setMessage(question).
          setNegativeButton("No", dialogClickListener).
          setPositiveButton("Yes", dialogClickListener).
          show();
    } else {
      // No anon readings to convert, just go to the home screen
      exitToHomeScreen();
    }
  }

  private void associateAnonymousReadings(long readmillUserId) {
    Log.d(TAG, "Association clicked");
    //noinspection unchecked
    (new AssociateAnonymousReadings()).execute(readmillUserId);
  }

  private void exitToHomeScreen() {
    getApp().removeFirstTimeFlag();
    Intent intentWelcome = new Intent(this, HomeActivity.class);
    intentWelcome.putExtra(IntentKeys.SIGNED_IN, true);
    startActivity(intentWelcome);
    finish();
  }

  private void visitWebPage(String url) {
    Uri readmillUrl = Uri.parse(url);
    Intent launchBrowser = new Intent(Intent.ACTION_VIEW, readmillUrl);
    startActivity(launchBrowser);
  }

  private class AssociateAnonymousReadings extends AsyncTask<Long, Void, Void> {

    @Override protected Void doInBackground(Long... integers) {
      long readmillUserId = integers[0];
      Log.i(TAG, "Associating anonymous readings with readmill user id:" + readmillUserId);
      try {
        Dao<LocalReading, Integer> readingDao = ApplicationReadTracker.getReadingDao();
        Where<LocalReading, Integer> stmt = readingDao.queryBuilder().
            where().eq(LocalReading.READMILL_USER_ID_FIELD_NAME, -1);

        ArrayList<LocalReading> anonymousReadings = (ArrayList<LocalReading>) stmt.query();
        Log.d(TAG, "Found " + anonymousReadings.size() + " anonymous readings");

        for(LocalReading reading : anonymousReadings) {
          Log.d(TAG, "Setting user id of reading: " + reading);
          reading.readmillUserId = readmillUserId;
          readingDao.update(reading);
        }
      } catch(SQLException e) {
        Log.e(TAG, "Exception while trying to read number of anonymous readings", e);
      }
      return null;
    }

    @Override protected void onPostExecute(Void aVoid) {
      WelcomeActivity.this.exitToHomeScreen();
    }
  }

  /**
   * Queries the database for a count of readings that are not associated
   * with a user.
   */
  private class CheckAnonymousReadingsASync extends AsyncTask<Void, Void, Integer> {

    @Override protected Integer doInBackground(Void... voids) {
      Log.d(TAG, "Checking for existing anonymous readings on device");
      try {
        Dao<LocalReading, Integer> readingDao = ApplicationReadTracker.getReadingDao();
        PreparedQuery<LocalReading> query = readingDao.queryBuilder().setCountOf(true).
            where().
            eq(LocalReading.READMILL_USER_ID_FIELD_NAME, -1).prepare();
        return (int) readingDao.countOf(query);
      } catch(SQLException e) {
        Log.e(TAG, "Exception while trying to read number of anonymous readings", e);
        return 0;
      }
    }

    @Override protected void onPostExecute(Integer count) {
      WelcomeActivity.this.onCheckAnonymousReadings(count);
    }
  }
}
