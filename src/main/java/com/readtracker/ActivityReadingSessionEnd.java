package com.readtracker;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import com.readtracker.customviews.ViewBindingBookHeader;
import com.readtracker.db.LocalReading;
import com.readtracker.db.LocalSession;
import com.readtracker.thirdparty.SafeViewFlipper;
import com.readtracker.thirdparty.widget.OnWheelChangedListener;
import com.readtracker.thirdparty.widget.WheelView;
import com.readtracker.thirdparty.widget.adapters.NumericWheelAdapter;

import java.sql.SQLException;
import java.util.Date;
import java.util.UUID;

/**
 * Screen for input the ending page of a reading session
 */
public class ActivityReadingSessionEnd extends ReadTrackerActivity {
  private static final int PAGE_DISPLAY_DURATION = 0;
  private static final int PAGE_EDIT_DURATION = 1;

  private static WheelView mWheelEndingPage;

  private static TextView mTextWhereDidYouEnd;

  private static ViewGroup mLayoutEndPercent;
  private static WheelView mWheelEndPercentInteger;
  private static WheelView mWheelEndPercentFraction;

  private static Button mButtonSaveReadingSession;
  private static Button mButtonShowDurationPicker;
  private static Button mButtonSetDuration;
  private static Button mButtonCancelSetDuration;

  private static WheelView mWheelDurationHours;
  private static WheelView mWheelDurationMinutes;
  private static WheelView mWheelDurationSeconds;

  private static SafeViewFlipper mFlipperSessionLength;

  private LocalReading mLocalReading;
  private long mSessionLengthMillis;

  @Override
  public void onCreate(Bundle in) {
    super.onCreate(in);
    Log.i(TAG, "onCreate");

    setContentView(R.layout.activity_reading_session_end);

    bindViews();
    bindEvents();

    int currentPage;
    if(in != null) {
      Log.i(TAG, "unfreezing state");
      mLocalReading = in.getParcelable(IntentKeys.LOCAL_READING);
      mSessionLengthMillis = in.getLong(IntentKeys.SESSION_LENGTH_MS);
      boolean buttonEnabled = in.getBoolean(IntentKeys.BUTTON_ENABLED);
      mButtonSaveReadingSession.setEnabled(buttonEnabled);
      currentPage = in.getInt(IntentKeys.PAGE);
    } else {
      Bundle extras = getIntent().getExtras();
      mSessionLengthMillis = extras.getLong(IntentKeys.SESSION_LENGTH_MS);
      mLocalReading = extras.getParcelable(IntentKeys.LOCAL_READING);
      mButtonSaveReadingSession.setEnabled(false);
      currentPage = (int) mLocalReading.currentPage;
    }

    ViewBindingBookHeader.bind(this, mLocalReading);

    initializeWheelViews(mLocalReading.isMeasuredInPercent());
    setCurrentPage(currentPage);

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
    out.putInt(IntentKeys.PAGE, getCurrentPage());
    Log.d(TAG, "Current page" + mWheelEndingPage.getCurrentItem());
  }

  private void initializeWheelViews(boolean isMeasuredInPercent) {
    if(isMeasuredInPercent) {
      setupPercentWheels();
      mTextWhereDidYouEnd.setText("What position did you end on?");
      mWheelEndingPage.setVisibility(View.GONE);
    } else {
      setupEndPageWheels();
      mTextWhereDidYouEnd.setText("What page did you end on?");
      mLayoutEndPercent.setVisibility(View.GONE);
    }
  }

  private void setupPercentWheels() {
    NumericWheelAdapter adapterIntegers = new NumericWheelAdapter(this, 0, 99);
    NumericWheelAdapter adapterFractions = new NumericWheelAdapter(this, 0, 99);

    configureWheelAdapterStyle(adapterIntegers);
    configureWheelAdapterStyle(adapterFractions);

    mWheelEndPercentInteger.setViewAdapter(adapterIntegers);
    mWheelEndPercentFraction.setViewAdapter(adapterFractions);

    configureWheelView(mWheelEndPercentInteger);
    configureWheelView(mWheelEndPercentFraction);
  }

  private void setupEndPageWheels() {
    NumericWheelAdapter endingPageAdapter = new NumericWheelAdapter(this, 0, (int) mLocalReading.totalPages);
    configureWheelAdapterStyle(endingPageAdapter);

    mWheelEndingPage.setViewAdapter(endingPageAdapter);
    configureWheelView(mWheelEndingPage);
  }

  private void bindViews() {
    mButtonSaveReadingSession = (Button) findViewById(R.id.btnSaveReadingSession);
    mButtonShowDurationPicker = (Button) findViewById(R.id.buttonShowDurationPicker);

    mTextWhereDidYouEnd = (TextView) findViewById(R.id.textWhereDidYouEnd);

    // Wheel for normal pages
    mWheelEndingPage = (WheelView) findViewById(R.id.wheelEndingPage);

    // Wheel for percents
    mLayoutEndPercent = (ViewGroup) findViewById(R.id.layoutEndPercent);
    mWheelEndPercentInteger = (WheelView) findViewById(R.id.wheelEndPercentInteger);
    mWheelEndPercentFraction = (WheelView) findViewById(R.id.wheelEndPercentFraction);

    mButtonSetDuration = (Button) findViewById(R.id.buttonSetDuration);
    mButtonCancelSetDuration = (Button) findViewById(R.id.buttonCancelSetDuration);

    mWheelDurationHours = (WheelView) findViewById(R.id.wheelDurationHours);
    mWheelDurationMinutes = (WheelView) findViewById(R.id.wheelDurationMinutes);
    mWheelDurationSeconds = (WheelView) findViewById(R.id.wheelDurationSeconds);

    mFlipperSessionLength = (SafeViewFlipper) findViewById(R.id.flipperSessionLength);
  }

  private void updateSessionLength() {
    mSessionLengthMillis = mWheelDurationHours.getCurrentItem() * 3600;
    mSessionLengthMillis += mWheelDurationMinutes.getCurrentItem() * 60;
    mSessionLengthMillis += mWheelDurationSeconds.getCurrentItem();
    mSessionLengthMillis *= 1000;
    mButtonShowDurationPicker.setText(Utils.shortHumanTimeFromMillis(mSessionLengthMillis));
    mFlipperSessionLength.setDisplayedChild(PAGE_DISPLAY_DURATION);
  }

  private void configureWheelAdapterStyle(NumericWheelAdapter wheelAdapter) {
    wheelAdapter.setTextColor(getResources().getColor(R.color.text_color_primary));
    wheelAdapter.setTypeFace(Typeface.DEFAULT);
    wheelAdapter.setTypeStyle(Typeface.NORMAL);
  }

  private void configureWheelView(WheelView wheelView) {
    wheelView.setInterpolator(null);
    wheelView.setVisibleItems(3);

    wheelView.addChangingListener(new OnWheelChangedListener() {
      @Override
      public void onChanged(WheelView wheel, int oldValue, int newValue) {
        boolean hasChanged = mLocalReading.currentPage != getCurrentPage();
        mButtonSaveReadingSession.setEnabled(hasChanged);
      }
    });
  }

  private void showDurationPicker() {
    // Lazily initialize the adapters
    boolean needInitialize = (
        mWheelDurationHours.getViewAdapter() == null ||
            mWheelDurationMinutes.getViewAdapter() == null ||
            mWheelDurationSeconds.getViewAdapter() == null
    );

    if(needInitialize) {
      NumericWheelAdapter hoursAdapter = new NumericWheelAdapter(this, 0, 48, "%sh");
      NumericWheelAdapter minutesAdapter = new NumericWheelAdapter(this, 0, 59, "%sm");
      NumericWheelAdapter secondsAdapter = new NumericWheelAdapter(this, 0, 59, "%ss");
      configureWheelAdapterStyle(hoursAdapter);
      configureWheelAdapterStyle(minutesAdapter);
      configureWheelAdapterStyle(secondsAdapter);

      mWheelDurationHours.setViewAdapter(hoursAdapter);
      mWheelDurationMinutes.setViewAdapter(minutesAdapter);
      mWheelDurationSeconds.setViewAdapter(secondsAdapter);
    }

    mWheelDurationHours.setCurrentItem(Utils.getHoursFromMillis(mSessionLengthMillis));
    mWheelDurationMinutes.setCurrentItem(Utils.getMinutesFromMillis(mSessionLengthMillis));
    mWheelDurationSeconds.setCurrentItem(Utils.getSecondsFromMillis(mSessionLengthMillis));

    mFlipperSessionLength.setDisplayedChild(PAGE_EDIT_DURATION);
  }

  private void bindEvents() {

    mButtonSaveReadingSession.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View view) {
        onClickedSave();
      }
    });

    mButtonShowDurationPicker.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View view) {
        showDurationPicker();
      }
    });

    mButtonSetDuration.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View view) {
        updateSessionLength();
      }
    });

    mButtonCancelSetDuration.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        mFlipperSessionLength.setDisplayedChild(PAGE_DISPLAY_DURATION);
      }
    });
  }

  private void onClickedSave() {
    mLocalReading.currentPage = getCurrentPage();
    mLocalReading.refreshProgress();
    mLocalReading.lastReadAt = (new Date()).getTime() / 1000; // Convert to seconds
    mLocalReading.timeSpentMillis += mSessionLengthMillis;

    // Send off to background task
    UpdateAndCreateSession.createSession(mLocalReading, mSessionLengthMillis,
        new UpdateAndCreateSession.OnCompleteListener() {
          @Override public void onCompleted(LocalSession localSession) {
            onSessionSaved(localSession);
          }
        }
    );
  }

  private void onSessionSaved(LocalSession localSession) {
    Log.i(TAG, "onSessionSaved: " + localSession);
    if(localSession != null) {
      Log.i(TAG, "Saved locally, initializing process of queued pings...");
      startService(new Intent(this, ReadmillTransferIntent.class));
      setResult(RESULT_OK);
      finish();
    } else {
      Builder alert = new AlertDialog.Builder(this);
      alert.setMessage("Failed to save the data");
      alert.setIcon(android.R.drawable.ic_dialog_alert);
      alert.show();
    }
  }

  /**
   * Read current page from the wheel controls
   *
   * @return the current page
   */
  private int getCurrentPage() {
    if(mLocalReading.isMeasuredInPercent()) {
      int percentInteger = mWheelEndPercentInteger.getCurrentItem();
      int percentFraction = mWheelEndPercentFraction.getCurrentItem();
      // Concatenate 45 and 17 => 4517
      return percentInteger * 100 + percentFraction;
    }
    return mWheelEndingPage.getCurrentItem();
  }

  /**
   * Sets the wheel controls to display the current page.
   *
   * @param currentPage the current page to use
   */
  private void setCurrentPage(int currentPage) {
    if(mLocalReading.isMeasuredInPercent()) {
      // Split 578 => 5 and 78
      int currentInteger = currentPage / 100;
      int currentFraction = currentPage - currentInteger * 100;

      mWheelEndPercentInteger.setCurrentItem(Math.min(99, currentInteger));
      mWheelEndPercentFraction.setCurrentItem(Math.min(99, currentFraction));
    } else {
      mWheelEndingPage.setCurrentItem(currentPage);
    }
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
      boolean success = updateLocalReading(mLocalReading) && saveReadingSession(newSession);
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

    private boolean updateLocalReading(LocalReading localReading) {
      Log.i(TAG, "Updating LocalReading: " + localReading.id);
      try {
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
