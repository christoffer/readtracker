package com.readtracker_beta.activities;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import com.readtracker_beta.*;
import com.readtracker_beta.custom_views.ProgressPicker;
import com.readtracker_beta.db.LocalReading;
import com.readtracker_beta.db.LocalSession;
import com.readtracker_beta.support.Utils;
import com.readtracker_beta.thirdparty.SafeViewFlipper;
import com.readtracker_beta.thirdparty.widget.WheelView;
import com.readtracker_beta.thirdparty.widget.adapters.NumericWheelAdapter;

import java.sql.SQLException;
import java.util.Date;
import java.util.UUID;

/**
 * Screen for input the ending page of a reading session
 */
public class EndSessionActivity extends ReadTrackerActivity {
  // View flipper page for editing duration
  private static final int PAGE_EDIT_DURATION = 1;

  private static Button mButtonSaveReadingSession;
  private static Button mButtonShowDurationPicker;
  private static Button mButtonFinish;

  private static WheelView mWheelDurationHours;
  private static WheelView mWheelDurationMinutes;

  private static ProgressPicker mProgressPicker;

  private static SafeViewFlipper mFlipperSessionLength;

  private LocalReading mLocalReading;
  private long mSessionLengthMillis;

  @Override
  public void onCreate(Bundle in) {
    super.onCreate(in);
    Log.i(TAG, "onCreate");

    setContentView(R.layout.activity_end_session);

    bindViews();
    bindEvents();

    int currentPage;
    if(in == null) {
      Bundle extras = getIntent().getExtras();
      mSessionLengthMillis = extras.getLong(IntentKeys.SESSION_LENGTH_MS);
      mLocalReading = extras.getParcelable(IntentKeys.LOCAL_READING);
      mButtonSaveReadingSession.setEnabled(false);
      currentPage = (int) mLocalReading.currentPage;
    } else {
      Log.i(TAG, "unfreezing state");
      mLocalReading = in.getParcelable(IntentKeys.LOCAL_READING);
      mSessionLengthMillis = in.getLong(IntentKeys.SESSION_LENGTH_MS);
      boolean buttonEnabled = in.getBoolean(IntentKeys.BUTTON_ENABLED);
      mButtonSaveReadingSession.setEnabled(buttonEnabled);
      currentPage = in.getInt(IntentKeys.PAGE);
    }

    ViewBindingBookHeader.bindWithDefaultClickHandler(this, mLocalReading);

    if(mLocalReading.isMeasuredInPercent()) {
      mProgressPicker.setupPercentMode(currentPage);
    } else {
      mProgressPicker.setupPagesMode(currentPage, (int) mLocalReading.totalPages);
    }
    mProgressPicker.setText("Ended on");

    findViewById(R.id.dividerTop).setBackgroundColor(mLocalReading.getColor());
    findViewById(R.id.dividerBottom).setBackgroundColor(mLocalReading.getColor());

    Log.i(TAG, "Init for reading : " + mLocalReading.id + " with session length:" + mSessionLengthMillis);

    final String duration = Utils.shortHumanTimeFromMillis(mSessionLengthMillis);
    mButtonShowDurationPicker.setText(duration);
  }

  @Override
  protected void onSaveInstanceState(Bundle out) {
    super.onSaveInstanceState(out);
    Log.d(TAG, "freezing state");
    out.putParcelable(IntentKeys.LOCAL_READING, mLocalReading);
    out.putLong(IntentKeys.SESSION_LENGTH_MS, mSessionLengthMillis);
    out.putBoolean(IntentKeys.BUTTON_ENABLED, mButtonSaveReadingSession.isEnabled());
    out.putInt(IntentKeys.PAGE, mProgressPicker.getPage());
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    switch(requestCode) {
      case ActivityCodes.REQUEST_FINISH_READING:
        if(resultCode == ActivityCodes.RESULT_OK) {
          // User finished the reading, fall through
          Log.v(TAG, "Reading was finished, exit with success");
          mLocalReading = data.getExtras().getParcelable(IntentKeys.LOCAL_READING);
          final long page = mLocalReading.totalPages;
          saveSessionAndExit(page);
        } else {
          // User cancelled the finish
          Log.v(TAG, "Reading was not finished. Ignoring.");
          return;
        }
        break;
    }
  }

  private void bindViews() {
    mButtonSaveReadingSession = (Button) findViewById(R.id.btnSaveReadingSession);
    mButtonShowDurationPicker = (Button) findViewById(R.id.buttonShowDurationPicker);

    mWheelDurationHours = (WheelView) findViewById(R.id.wheelDurationHours);
    mWheelDurationMinutes = (WheelView) findViewById(R.id.wheelDurationMinutes);

    mFlipperSessionLength = (SafeViewFlipper) findViewById(R.id.flipperSessionLength);
    mProgressPicker = (ProgressPicker) findViewById(R.id.progressPicker);

    mButtonFinish = (Button) findViewById(R.id.buttonFinish);
  }


  private void bindEvents() {
    mButtonSaveReadingSession.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        final long page = mProgressPicker.getPage();
        saveSessionAndExit(page);
      }
    });

    mButtonShowDurationPicker.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        showDurationPicker();
      }
    });

    mButtonFinish.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        exitToFinishReading(mLocalReading);
      }
    });

    mProgressPicker.setOnProgressChangeListener(new ProgressPicker.OnProgressChangeListener() {
      @Override
      public void onChangeProgress(int newPage) {
        boolean hasChanged = mLocalReading.currentPage != newPage;
        mButtonSaveReadingSession.setEnabled(hasChanged);
      }
    });
  }

  private void configureWheelAdapterStyle(NumericWheelAdapter wheelAdapter) {
    wheelAdapter.setTextColor(getResources().getColor(R.color.text_color_primary));
    wheelAdapter.setTypeFace(Typeface.DEFAULT);
    wheelAdapter.setTypeStyle(Typeface.NORMAL);
  }

  private void showDurationPicker() {
    boolean needInitialize = (
      mWheelDurationHours.getViewAdapter() == null || mWheelDurationMinutes.getViewAdapter() == null
    );

    if(needInitialize) {
      NumericWheelAdapter hoursAdapter = createDurationWheelAdapter(24, "%s hour[s?]");
      NumericWheelAdapter minutesAdapter = createDurationWheelAdapter(59, "%s minute[s?]");

      mWheelDurationHours.setCalliperMode(WheelView.CalliperMode.LEFT_CALLIPER);
      mWheelDurationMinutes.setCalliperMode(WheelView.CalliperMode.RIGHT_CALLIPER);

      mWheelDurationHours.setVisibleItems(3);
      mWheelDurationMinutes.setVisibleItems(3);

      mWheelDurationHours.setViewAdapter(hoursAdapter);
      mWheelDurationMinutes.setViewAdapter(minutesAdapter);
    }

    mWheelDurationHours.setCurrentItem(Utils.getHoursFromMillis(mSessionLengthMillis));
    mWheelDurationMinutes.setCurrentItem(Utils.getMinutesFromMillis(mSessionLengthMillis));

    mFlipperSessionLength.setDisplayedChild(PAGE_EDIT_DURATION);
  }

  private NumericWheelAdapter createDurationWheelAdapter(int maxValue, String format) {
    NumericWheelAdapter adapter = new NumericWheelAdapter(this, 0, maxValue, format);
    configureWheelAdapterStyle(adapter);
    adapter.setTextSize(14);
    return adapter;
  }

  /**
   * Start the FinishBookActivity with the current reading and await the result.
   *
   * @param localReading The current local reading to finish.
   */
  private void exitToFinishReading(LocalReading localReading) {
    Intent finishActivity = new Intent(this, FinishBookActivity.class);
    finishActivity.putExtra(IntentKeys.LOCAL_READING, localReading);
    startActivityForResult(finishActivity, ActivityCodes.REQUEST_FINISH_READING);
  }

  private void saveSessionAndExit(long page) {
    final long durationMillis = mSessionLengthMillis;

    mLocalReading.setCurrentPage(page);
    mLocalReading.lastReadAt = (new Date()).getTime() / 1000; // Convert to seconds

    // Send off to background task
    UpdateAndCreateSession.createSession(mLocalReading, durationMillis,
      new UpdateAndCreateSession.OnCompleteListener() {
        @Override
        public void onCompleted(LocalSession localSession) {
          onSessionSaved(localSession);
        }
      }
    );
  }

  private void onSessionSaved(LocalSession localSession) {
    Log.i(TAG, "onSessionSaved: " + localSession);
    if(localSession == null) {
      toastLong("An error occurred while saving your data.");
      return;
    }

    Log.i(TAG, "Saved locally, initializing process of queued pings...");
    startService(new Intent(this, ReadmillTransferIntent.class));
    setResult(RESULT_OK);
    finish();
  }

  // --------------------------------------------------------------------
  // ASyncTask Class
  // --------------------------------------------------------------------

  private static class UpdateAndCreateSession extends AsyncTask<Void, Void, LocalSession> {
    private static final String TAG = UpdateAndCreateSession.class.getName();
    private LocalReading mLocalReading;
    private long mSessionLength;
    private OnCompleteListener mListener;

    public interface OnCompleteListener {
      public void onCompleted(LocalSession localSession);
    }

    public static void createSession(LocalReading localReading, long sessionLengthMillis, OnCompleteListener listener) {
      UpdateAndCreateSession instance = new UpdateAndCreateSession(localReading, sessionLengthMillis, listener);
      //noinspection unchecked
      instance.execute();
    }

    private UpdateAndCreateSession(LocalReading localReading, long sessionLength, OnCompleteListener listener) {
      mLocalReading = localReading;
      mSessionLength = sessionLength;
      mListener = listener;
    }

    @Override
    protected LocalSession doInBackground(Void... args) {
      Log.i(TAG, "Saving reading with id " + mLocalReading.id);
      LocalSession newSession = generateReadingSession(mLocalReading, mSessionLength);
      Log.d(TAG, "Created session: " + newSession);
      boolean success = updateLocalReading(mLocalReading, mSessionLength) && saveReadingSession(newSession);
      return success ? newSession : null;
    }

    private LocalSession generateReadingSession(final LocalReading localReading, final long sessionLength) {
      final long sessionDurationSeconds = (long) Math.floor((double) sessionLength / 1000.0);

      return new LocalSession() {{
        readingId = localReading.id;
        readmillReadingId = localReading.readmillReadingId;
        durationSeconds = sessionDurationSeconds;
        progress = localReading.progress;
        endedOnPage = (int) localReading.currentPage;
        sessionIdentifier = UUID.randomUUID().toString();
        occurredAt = new Date();
      }};
    }

    private boolean updateLocalReading(LocalReading localReading, long addedDurationMillis) {
      Log.i(TAG, "Updating LocalReading: " + localReading.id + ", adding: " + addedDurationMillis + " milliseconds to time spent");
      try {
        localReading.timeSpentMillis += addedDurationMillis;
        ApplicationReadTracker.getReadingDao().update(localReading);
        return true;
      } catch(SQLException e) {
        Log.e(TAG, "Failed to update data", e);
        return false;
      }
    }

    private boolean saveReadingSession(LocalSession session) {
      Log.i(TAG, "Saving session: " + session.readingId);
      try {
        ApplicationReadTracker.getSessionDao().create(session);
        return true;
      } catch(SQLException e) {
        Log.e(TAG, "Failed to create Session", e);
        return false;
      }
    }

    @Override
    protected void onPostExecute(LocalSession localSession) {
      mListener.onCompleted(localSession);
    }
  }
}
