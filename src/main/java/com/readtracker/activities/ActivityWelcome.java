package com.readtracker.activities;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.PreparedQuery;
import com.j256.ormlite.stmt.Where;
import com.readtracker.ApplicationReadTracker;
import com.readtracker.IntentKeys;
import com.readtracker.R;
import com.readtracker.db.LocalReading;

import java.sql.SQLException;
import java.util.ArrayList;

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
      // Initiate a search for device only readings
      //noinspection unchecked
      (new CheckAnonymousReadingsASync()).execute();
    }
  }

  private void onOfflineClicked() {
    Log.d(TAG, "clicked Start in offline mode");
    exitToHomeScreen();
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
    Log.d(TAG, "Assocation clicked");
    //noinspection unchecked
    (new AssociateAnonymousReadings()).execute(readmillUserId);
  }

  private void exitToHomeScreen() {
    getApp().removeFirstTimeFlag();
    Intent intentWelcome = new Intent(this, ActivityHome.class);
    intentWelcome.putExtra(IntentKeys.SIGNED_IN, true);
    startActivity(intentWelcome);
    finish();
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
      ActivityWelcome.this.exitToHomeScreen();
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
      ActivityWelcome.this.onCheckAnonymousReadings(count);
    }
  }
}
