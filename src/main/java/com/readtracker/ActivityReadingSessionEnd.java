package com.readtracker;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
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
  private static WheelView mWheelEndingPage;
  private static Button mButtonSaveReadingSession;
  private static Button mButtonShowDurationPicker;
  private static Button mButtonSetDuration;
  private static Button mButtonCancelSetDuration;

  private static WheelView mWheelDurationHours;
  private static WheelView mWheelDurationMinutes;
  private static WheelView mWheelDurationSeconds;

  private static NumericWheelAdapter mDurationHoursAdapter;
  private static NumericWheelAdapter mDurationMinutesAdapter;
  private static NumericWheelAdapter mDurationSecondsAdapter;

  private static SafeViewFlipper mFlipperSessionLength;

  private Animation mSlideUp;
  private Animation mSlideDown;
  private Animation mFadeIn;
  private Animation mFadeOut;

  private LocalReading mLocalReading;
  private long mSessionLengthMillis;

  @Override
  public void onCreate(Bundle in) {
    super.onCreate(in);
    Log.i(TAG, "onCreate");

    setContentView(R.layout.reading_session_end);

    bindViews();

    bindEvents();

    if(in != null) {
      Log.i(TAG, "unfreezing state");
      mLocalReading = in.getParcelable(IntentKeys.LOCAL_READING);
      mSessionLengthMillis = in.getLong(IntentKeys.SESSION_LENGTH_MS);
      boolean buttonEnabled = in.getBoolean(IntentKeys.BUTTON_ENABLED);
      mButtonSaveReadingSession.setEnabled(buttonEnabled);
      mWheelEndingPage.setTag(in.getInt(IntentKeys.PAGE));
    } else {
      Bundle extras = getIntent().getExtras();
      mSessionLengthMillis = extras.getLong(IntentKeys.SESSION_LENGTH_MS);
      mLocalReading = extras.getParcelable(IntentKeys.LOCAL_READING);
      mButtonSaveReadingSession.setEnabled(false);
      mWheelEndingPage.setTag((int) mLocalReading.currentPage);
    }

    initialize();

    Log.i(TAG, "Init for reading : " + mLocalReading.id + " with session length:" + mSessionLengthMillis);

    mButtonShowDurationPicker.setText(Utils.shortHumanTimeFromMillis(mSessionLengthMillis));
  }

  @Override
  protected void onSaveInstanceState(Bundle out) {
    super.onSaveInstanceState(out);
    Log.d(TAG, "freezing state");
    out.putParcelable(IntentKeys.LOCAL_READING, mLocalReading);
    out.putLong(IntentKeys.SESSION_LENGTH_MS, mSessionLengthMillis);
    out.putBoolean(IntentKeys.BUTTON_ENABLED, mButtonSaveReadingSession.isEnabled());
    out.putInt(IntentKeys.PAGE, mWheelEndingPage.getCurrentItem());
    Log.d(TAG, "Current page" + mWheelEndingPage.getCurrentItem());
  }

  private void initialize() {
    ViewBindingBookHeader.bind(this, mLocalReading);

    NumericWheelAdapter endingPageAdapter = new NumericWheelAdapter(this, 0, (int) mLocalReading.totalPages);

    mWheelEndingPage.setViewAdapter(endingPageAdapter);
    mWheelEndingPage.setCurrentItem((Integer) mWheelEndingPage.getTag());
    mWheelEndingPage.setInterpolator(null);
    mWheelEndingPage.setVisibleItems(3);

    mWheelEndingPage.addChangingListener(new OnWheelChangedListener() {
      @Override
      public void onChanged(WheelView wheel, int oldValue, int newValue) {
        mButtonSaveReadingSession.setEnabled(true);
      }
    });
  }

  private void bindViews() {
    mButtonSaveReadingSession = (Button) findViewById(R.id.btnSaveReadingSession);
    mButtonShowDurationPicker = (Button) findViewById(R.id.buttonShowDurationPicker);
    mWheelEndingPage = (WheelView) findViewById(R.id.wheelEndingPage);

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
    closeDurationPicker();
  }

  private void showDurationPicker() {
    // Lazily initalize the adapters, since they'll probably be used very seldom

    if(mDurationHoursAdapter == null) {
      mDurationHoursAdapter = new NumericWheelAdapter(this, 0, 48, "%sh");
    }

    if(mDurationMinutesAdapter == null) {
      mDurationMinutesAdapter = new NumericWheelAdapter(this, 0, 59, "%sm");
    }

    if(mDurationSecondsAdapter == null) {
      mDurationSecondsAdapter = new NumericWheelAdapter(this, 0, 59, "%ss");
    }

    mWheelDurationHours.setViewAdapter(mDurationHoursAdapter);
    mWheelDurationMinutes.setViewAdapter(mDurationMinutesAdapter);
    mWheelDurationSeconds.setViewAdapter(mDurationSecondsAdapter);

    mWheelDurationHours.setCurrentItem(Utils.getHoursFromMillis(mSessionLengthMillis));
    mWheelDurationMinutes.setCurrentItem(Utils.getMinutesFromMillis(mSessionLengthMillis));
    mWheelDurationSeconds.setCurrentItem(Utils.getSecondsFromMillis(mSessionLengthMillis));

    openDurationPicker();
  }

  private void closeDurationPicker() {
    if(mSlideDown == null) {
      mSlideDown = AnimationUtils.loadAnimation(this, R.anim.slide_down_appear);
    }
    if(mFadeIn == null) {
      mFadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);
    }
    mFlipperSessionLength.setOutAnimation(mSlideDown);
    mFlipperSessionLength.setInAnimation(mFadeIn);
    mFlipperSessionLength.setDisplayedChild(0);
  }

  private void openDurationPicker() {
    if(mSlideUp == null) {
      mSlideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up_appear);
      mSlideUp.setDuration(350);
    }
    if(mFadeOut == null) {
      mFadeOut = AnimationUtils.loadAnimation(this, R.anim.fade_out);
    }
    mFlipperSessionLength.setOutAnimation(mFadeOut);
    mFlipperSessionLength.setInAnimation(mSlideUp);
    mFlipperSessionLength.setDisplayedChild(1);
  }


  private void bindEvents() {

    mButtonSaveReadingSession.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) { handleSave(); }
    });

    mButtonShowDurationPicker.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) { showDurationPicker(); }
    });

    mButtonSetDuration.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        updateSessionLength();
      }
    });

    mButtonCancelSetDuration.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        closeDurationPicker();
      }
    });
  }

  private void handleSave() {
    long sessionEndPage;

    sessionEndPage = mWheelEndingPage.getCurrentItem();

    mLocalReading.currentPage = sessionEndPage;
    mLocalReading.refreshProgress();
    mLocalReading.lastReadAt = (new Date()).getTime() / 1000; // Convert to seconds
    mLocalReading.timeSpentMillis += mSessionLengthMillis;

    ObjectBundle objectBundle = new ObjectBundle(mLocalReading, mSessionLengthMillis);

    // Send off to background task
    (new UpdateAndCreateSession()).execute(objectBundle);
  }

  private void onPostCreateAndUpdate(Boolean result) {
    Log.i(TAG, "onPostCreateAndUpdate with success: " + result.toString());
    if(result) {
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

  // --------------------------------------------------------------------
  // AsyncTask Class
  // --------------------------------------------------------------------

  private class UpdateAndCreateSession extends AsyncTask<ObjectBundle, Void, Boolean> {

    @Override
    protected Boolean doInBackground(ObjectBundle... sessions) {
      ObjectBundle session = sessions[0];
      Log.i(TAG, "Saving reading with id " + session.mLocalReading.id);
      return updateLocalReading(session.mLocalReading) && createReadingSession(session.generateReadingSession());
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

    private boolean createReadingSession(LocalSession session) {
      Log.i(TAG, "Creating Session for LocalReading: " + session.readingId);
      try {
        ApplicationReadTracker.getSessionDao().create(session);
        return true;
      } catch(SQLException e) {
        Log.e(TAG, "Failed to create Session", e);
        return false;
      }
    }

    @Override
    protected void onPostExecute(Boolean result) {
      onPostCreateAndUpdate(result);
    }
  }

  // Object for passing two objects at once the the background task
  class ObjectBundle {

    public LocalReading mLocalReading;
    public long mSessionDurationMillis;

    public ObjectBundle(LocalReading localReading, long sessionDurationMillis) {
      this.mLocalReading = localReading;
      this.mSessionDurationMillis = sessionDurationMillis;
    }

    public LocalSession generateReadingSession() {
      long durationSeconds = (long) Math.floor((double) this.mSessionDurationMillis / 1000.0);

      LocalSession session = new LocalSession();

      session.readingId = mLocalReading.id;
      session.readmillReadingId = mLocalReading.readmillReadingId;
      session.durationSeconds = durationSeconds;
      session.progress = mLocalReading.progress;
      session.endedOnPage = (int) mLocalReading.currentPage;
      session.sessionIdentifier = UUID.randomUUID().toString();
      session.occurredAt = new Date();

      Log.i(TAG, "Creating ReadingSession with duration: " + durationSeconds + ", started on " + session.startedOnPage + " to " + session.endedOnPage + " progress: " + session.progress);
      return session;
    }
  }
}
