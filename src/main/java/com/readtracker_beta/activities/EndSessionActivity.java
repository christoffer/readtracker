package com.readtracker_beta.activities;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import com.readtracker_beta.ApplicationReadTracker;
import com.readtracker_beta.IntentKeys;
import com.readtracker_beta.R;
import com.readtracker_beta.ReadmillTransferIntent;
import com.readtracker_beta.custom_views.ProgressPicker;
import com.readtracker_beta.db.LocalReading;
import com.readtracker_beta.db.LocalSession;
import com.readtracker_beta.support.Utils;
import com.readtracker_beta.thirdparty.SafeViewFlipper;
import com.readtracker_beta.thirdparty.widget.WheelView;
import com.readtracker_beta.thirdparty.widget.adapters.AbstractWheelTextAdapter;
import com.readtracker_beta.thirdparty.widget.adapters.ArrayWheelAdapter;

import java.sql.SQLException;
import java.util.Date;
import java.util.UUID;

/**
 * Screen for input the ending page of a reading session
 */
public class EndSessionActivity extends ReadTrackerActivity {
  private static final int SAVE_BUTTON_PAGE = 0;
  private static final int FINISH_BUTTON_PAGE = 1;

  private static Button mButtonSaveProgress;
  private static Button mButtonFinishBook;

  private static WheelView mWheelDuration;

  private static SafeViewFlipper mFlipperActionButtons;

  private static ProgressPicker mProgressPicker;

  private LocalReading mLocalReading;
  private long mSessionLengthMillis;

  @Override
  public void onCreate(Bundle in) {
    super.onCreate(in);
    Log.i(TAG, "onCreate");

    setContentView(R.layout.activity_end_session);

    bindViews();
    bindEvents();
    initializeWheelViews();

    int currentPage;
    long currentDuration;
    if(in == null) {
      Bundle extras = getIntent().getExtras();
      mLocalReading = extras.getParcelable(IntentKeys.LOCAL_READING);
      mSessionLengthMillis = extras.getLong(IntentKeys.SESSION_LENGTH_MS);
      currentDuration = mSessionLengthMillis;
      mButtonSaveProgress.setEnabled(false);
      currentPage = (int) mLocalReading.currentPage;
    } else {
      Log.i(TAG, "unfreezing state");
      mLocalReading = in.getParcelable(IntentKeys.LOCAL_READING);
      mSessionLengthMillis = in.getLong(IntentKeys.SESSION_LENGTH_MS);
      currentDuration = in.getLong(IntentKeys.CURRENT_DURATION);
      currentPage = in.getInt(IntentKeys.PAGE);
      boolean buttonEnabled = in.getBoolean(IntentKeys.BUTTON_ENABLED);
      mButtonSaveProgress.setEnabled(buttonEnabled);
    }

    setupDuration(currentDuration);

    ViewBindingBookHeader.bindWithDefaultClickHandler(this, mLocalReading);

    mProgressPicker.setupForLocalReading(mLocalReading);
    mProgressPicker.setCurrentPage(currentPage);

    final boolean onLastPage = currentPage == mLocalReading.totalPages;
    toggleFinishButton(onLastPage);

    findViewById(R.id.dividerBottom).setBackgroundColor(mLocalReading.getColor());

    Log.i(TAG, "Init for reading : " + mLocalReading.id + " with session length:" + mSessionLengthMillis);
  }

  @Override
  protected void onSaveInstanceState(Bundle out) {
    super.onSaveInstanceState(out);
    Log.d(TAG, "freezing state");
    out.putParcelable(IntentKeys.LOCAL_READING, mLocalReading);
    out.putLong(IntentKeys.SESSION_LENGTH_MS, mSessionLengthMillis);
    out.putLong(IntentKeys.CURRENT_DURATION, getCurrentDuration());
    out.putBoolean(IntentKeys.BUTTON_ENABLED, mButtonSaveProgress.isEnabled());
    out.putInt(IntentKeys.PAGE, mProgressPicker.getCurrentPage());
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
          saveSessionAndExit(page, getCurrentDuration());
        } else {
          // User cancelled the finish
          Log.v(TAG, "Reading was not finished. Ignoring.");
          return;
        }
        break;
    }
  }

  private void bindViews() {
    mButtonSaveProgress = (Button) findViewById(R.id.buttonSaveProgress);
    mButtonFinishBook = (Button) findViewById(R.id.buttonFinishBook);

    mWheelDuration = (WheelView) findViewById(R.id.wheelDuration);

    mFlipperActionButtons = (SafeViewFlipper) findViewById(R.id.flipperActionButtons);

    mProgressPicker = (ProgressPicker) findViewById(R.id.progressPicker);
  }

  private void bindEvents() {
    mButtonSaveProgress.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        saveSessionAndExit(mProgressPicker.getCurrentPage(), getCurrentDuration());
      }
    });

    mButtonFinishBook.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View view) {
        exitToFinishReading(mLocalReading);
      }
    });

    mProgressPicker.setOnProgressChangeListener(new ProgressPicker.OnProgressChangeListener() {
      @Override
      public void onChangeProgress(int newPage) {
        boolean hasChanged = mLocalReading.currentPage != newPage;
        mButtonSaveProgress.setEnabled(hasChanged);

        final boolean onLastPage = newPage == mLocalReading.totalPages;
        toggleFinishButton(onLastPage);
      }
    });
  }

  private void toggleFinishButton(boolean finishMode) {
    if(finishMode && mFlipperActionButtons.getDisplayedChild() != FINISH_BUTTON_PAGE) {
      mFlipperActionButtons.setDisplayedChild(FINISH_BUTTON_PAGE);
    } else if(!finishMode && mFlipperActionButtons.getDisplayedChild() != SAVE_BUTTON_PAGE) {
      mFlipperActionButtons.setDisplayedChild(SAVE_BUTTON_PAGE);
    }
  }

  private void initializeWheelViews() {
    ArrayWheelAdapter hoursAdapter = createDurationWheelAdapter(24 * 60);
    mWheelDuration.setVisibleItems(3);
    mWheelDuration.setViewAdapter(hoursAdapter);
    mWheelDuration.setCalliperMode(WheelView.CalliperMode.NO_CALLIPERS);
  }

  private void setupDuration(long sessionLengthMillis) {
    int minutes = (int) (sessionLengthMillis / (1000 * 60));
    Log.v(TAG, "Setting duration: " + sessionLengthMillis);
    mWheelDuration.setCurrentItem(minutes);
  }

  private long getCurrentDuration() {
    Log.v(TAG, "Current duration: " + mWheelDuration.getCurrentItem() * 60 * 1000);
    return mWheelDuration.getCurrentItem() * 60 * 1000;
  }

  private ArrayWheelAdapter createDurationWheelAdapter(int maxMinutes) {
    String[] labels = new String[maxMinutes];
    for(int minute = 0; minute < maxMinutes; minute++) {
      labels[minute] = Utils.hoursAndMinutesFromMillis(minute * 60 * 1000);
    }

    ArrayWheelAdapter<String> adapter = new ArrayWheelAdapter<String>(this, labels);
    adapter.setTextColor(getResources().getColor(R.color.text_color_primary));
    adapter.setTypeFace(Typeface.DEFAULT);
    adapter.setTypeStyle(Typeface.NORMAL);
    float fontSizePixels = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 12, getResources().getDisplayMetrics());
    adapter.setTextSize((int) fontSizePixels);
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

  private void saveSessionAndExit(long page, long durationMillis) {
    Log.v(TAG, "Exiting with page " + page + " and duration " + durationMillis);
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
