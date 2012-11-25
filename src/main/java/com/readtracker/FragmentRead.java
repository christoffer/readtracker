package com.readtracker;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.*;
import android.widget.Button;
import android.widget.TextView;
import com.readtracker.customviews.PauseableSpinAnimation;
import com.readtracker.customviews.TimeSpinner;
import com.readtracker.db.LocalReading;
import com.readtracker.thirdparty.SafeViewFlipper;
import com.readtracker.value_objects.ReadingState;

/**
 * Fragment for managing a reading session
 */
public class FragmentRead extends Fragment {
  private static final String TAG = FragmentRead.class.getName();

  // Session controls
  private static Button mButtonStart;
  private static Button mButtonPause;
  private static Button mButtonDone;

  // Time tracking
  private static TextView mTextBillboard;
  private static TimeSpinner mTimeSpinner;

  // Flipper for showing start vs. pause/done
  private static SafeViewFlipper mFlipperSessionControl;

  // Reading to track
  private LocalReading mLocalReading;

  // Timing
  private RedrawTimerTask mRedrawTimerTask;

  // Timestamp of when play/resume was pressed last time
  private long mTimestampLastStarted = 0;

  // Accumulated elapsed time, not including the time since latest timestamp
  private long mElapsed = 0;

  // Force reinitialize when returning from an activity that is known
  // to cause data updates.
  private boolean mForceReInitialize;

  // Display child index for flipper session control
  private static final int PAGE_READING_CONTROLS_INACTIVE = 0;
  private static final int PAGE_READING_CONTROLS_ACTIVE = 1;

  public static Fragment newInstance(LocalReading localReading, long elapsed) {
    Log.d(TAG, "newInstance()");
    FragmentRead instance = new FragmentRead();
    instance.setElapsed(elapsed);
    instance.setLocalReading(localReading);
    instance.setForceReinitialize(true);
    return instance;
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    mRedrawTimerTask = null;
    if(savedInstanceState != null && !mForceReInitialize) {
      Log.d(TAG, "unfreeze state");
      mLocalReading = savedInstanceState.getParcelable(IntentKeys.LOCAL_READING);
    }
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    Log.d(TAG, "onCreateView()");
    View view = inflater.inflate(R.layout.fragment_read, container, false);
    bindViews(view);
    bindEvents();

    mFlipperSessionControl.setDisplayedChild(PAGE_READING_CONTROLS_INACTIVE);

    if(mLocalReading == null) { // TODO investigate when this could happen...
      Log.d(TAG, "Loaded without local reading");
      return view;
    }

    Log.d(TAG, "Loaded with local reading: " + mLocalReading.getInfo());

    // Show the book initialization screen or the read tracker
    if(mLocalReading.hasPageInfo()) {
      setupTimeTracking();
    } else {
      setupMissingPages();
    }

    return view;
  }

  @Override
  public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    Log.d(TAG, "freezing state");
    outState.putParcelable(IntentKeys.LOCAL_READING, mLocalReading);
  }

  @Override
  public void onPause() {
    super.onPause();
    Log.d(TAG, "onPause()");

    if(((ActivityBook) getActivity()).isManualShutdown()) {
      Log.d(TAG, "Parent Activity is shutting down - don't store state");
    } else {
      Log.d(TAG, "Parent Activity not shutting down - store state");
      ReadingStateHandler.store(mLocalReading.id, elapsed(), mTimestampLastStarted);
    }

    stopTrackerUpdates();
    presentTime(elapsed());
  }

  @Override
  public void onResume() {
    super.onResume();
    Log.d(TAG, "onResume()");
    if(!mForceReInitialize) {
      loadTimingState();
    } else {
      Log.d(TAG, "Skipping loading of stored session due to forced initialize");
      mForceReInitialize = false; // avoid re-init when bringing an instance back into focus
    }
  }

  private void bindViews(View view) {
    mButtonDone = (Button) view.findViewById(R.id.buttonDone);
    mButtonStart = (Button) view.findViewById(R.id.buttonStart);
    mButtonPause = (Button) view.findViewById(R.id.buttonPause);

    mFlipperSessionControl = (SafeViewFlipper) view.findViewById(R.id.flipperSessionControl);

    mTextBillboard = (TextView) view.findViewById(R.id.textBillboard);
    mTimeSpinner = (TimeSpinner) view.findViewById(R.id.timespinner);
  }

  private void bindEvents() {
    Log.d(TAG, "bindEvents()");

    mButtonPause.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View view) {
        onClickedPause();
      }
    });

    mButtonStart.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View view) {
        onClickedStart();
      }
    });

    mButtonDone.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View view) {
        onClickedDone();
      }
    });

//    mButtonUpdatePageNumbers.setOnClickListener(new View.OnClickListener() {
//      @Override public void onClick(View view) {
//        onClickedSetPageCount();
//      }
//    });

//    if(!mLocalReading.hasPageInfo()) {
//      mTextNumberOfPages.setText(String.format(getString(R.string.please_enter_number_of_pages, mLocalReading.title)));
//    }
  }

  /* Public API */

  private void setElapsed(long elapsed) {
    mElapsed = elapsed;
  }

  public void setLocalReading(LocalReading localReading) {
    mLocalReading = localReading;
  }

  /* Private API */

  private void setupTimeTracking() {
    // Starting or continuing a reading session?
    if(mElapsed == 0) {
      describeLastPosition(mLocalReading);
    } else {
      presentTime(elapsed());
    }

    mTextBillboard.setEnabled(true);
    mButtonStart.setText("Start");
  }

  private void setupMissingPages() {
    mTextBillboard.setText("Please add page count");
    mTextBillboard.setEnabled(false);
    mButtonStart.setText("Edit book");
  }

  /**
   * Updates the text header and summary to show where the user last left off.
   * Handles pages/percent and shows a special text for first session.
   *
   * @param localReading The local reading to describe last position off
   */
  private void describeLastPosition(LocalReading localReading) {
    boolean isFirstRead = localReading.currentPage == 0;

    if(isFirstRead) {
      mTextBillboard.setText("First session");
      return;
    }

    if(localReading.measureInPercent) {
      int currentInteger = (int) localReading.currentPage / 100;
      int currentFraction = (int) localReading.currentPage - currentInteger * 100;
      mTextBillboard.setText(String.format("Last at %d.%d%%", currentInteger, currentFraction));
    } else {
      mTextBillboard.setText(String.format("Last on page %d", localReading.currentPage));
    }
  }

  /**
   * Provides outside access to the current reading state
   *
   * @return the current reading state as a value object
   */
  public ReadingState getReadingState() {
    if(mLocalReading == null) return null;
    return new ReadingState(mLocalReading.id, elapsed(), mTimestampLastStarted);
  }

  /**
   * Called when the start button is clicked
   */
  private void onClickedStart() {
    final Animation disappear = AnimationUtils.loadAnimation(getActivity(), R.anim.fade_out);
    final Animation appear = AnimationUtils.loadAnimation(getActivity(), R.anim.slide_up_appear);

    disappear.setAnimationListener(new Animation.AnimationListener() {
      @Override public void onAnimationStart(Animation animation) { }
      @Override public void onAnimationRepeat(Animation animation) { }
      @Override public void onAnimationEnd(Animation animation) {
        startTiming();
        ((ActivityBook) getActivity()).setDirty(true);
        presentTime(elapsed());
        mTextBillboard.startAnimation(appear);
      }
    });

    mTextBillboard.startAnimation(disappear);
    mFlipperSessionControl.setDisplayedChild(PAGE_READING_CONTROLS_ACTIVE);
  }

  /**
   * Called when the pause button is clicked
   */
  private void onClickedPause() {
    if(isTiming()) {
      stopTimingAndUpdateElapsed();
      activatePause();
    } else {
      startTiming();
      deactivatePause();
    }
  }

  /**
   * Called when the done button is clicked
   */
  private void onClickedDone() {
    if(isTiming()) {
      activatePause();
    }
    stopTimingAndUpdateElapsed();
    ((ActivityBook) getActivity()).exitToSessionEndScreen(mElapsed);
  }

  // Causes the activity to reload the LocalReading
  private void setForceReinitialize(boolean forceReinitialize) {
    mForceReInitialize = forceReinitialize;
  }

  /**
   * Loads and restores a timing state from preferences
   */
  private void loadTimingState() {
    Log.d(TAG, "Loading stored timing state");
    final ReadingState readingState = ReadingStateHandler.load();

    if(readingState == null) {
      Log.d(TAG, "... Not found");
    } else {
      restoreTimingState(readingState);
    }
  }

  /**
   * Restores the current timing state to a given one
   *
   * @param readingState The reading state to restore to
   */
  public void restoreTimingState(ReadingState readingState) {
    Log.i(TAG, "Restoring session: " + readingState);

    mTimestampLastStarted = readingState.getActiveTimestamp();
    mElapsed = readingState.getElapsedMilliseconds();

    // Notify to the parent activity that our data is dirty so it can store
    // the state if the user leaves the activity.
    ((ActivityBook) getActivity()).setDirty(true);

    mFlipperSessionControl.setDisplayedChild(PAGE_READING_CONTROLS_ACTIVE);

    // Check if we should automatically start the timer
    if(readingState.isActive()) {
      Log.d(TAG, "Got active reading state, starting timer");
      deactivatePause();
      startTrackerUpdates();
    } else {
      Log.d(TAG, "Got inactive reading state");
      activatePause();
    }
    presentTime(elapsed());
  }

  /**
   * Called when save has been clicked on the set number of pages dialog
   */
//  private void onClickedSetPageCount() {
//    int pageNumbers = Utils.parseInt(mEditPageCount.getText().toString(), 0);
//
//    if(pageNumbers < 1) {
//      ((ReadTrackerActivity) getActivity()).toast(getString(R.string.enter_page_count));
//      mEditPageCount.requestFocus();
//      return;
//    }
//
//    mLocalReading.totalPages = pageNumbers;
//
//    ((ActivityBook) getActivity()).onLocalReadingChanged();
//
//    SaveLocalReadingTask.save(mLocalReading, new SaveLocalReadingListener() {
//      @Override
//      public void onLocalReadingSaved(LocalReading localReading) {
//        // Flip back to active reading state
//        mFlipperReadingSession.setDisplayedChild(PAGE_READING_SESSION);
//      }
//    });
//  }

  /**
   * Changes UI to pause mode
   */
  private void activatePause() {
    mButtonPause.setText("Resume");
    Animation fadeOutHalf = AnimationUtils.loadAnimation(getActivity(), R.anim.fade_out_half);
    fadeOutHalf.setAnimationListener(new EnableReadingControls(false));
    mButtonDone.startAnimation(fadeOutHalf);
//    mLayoutSessionTimer.startAnimation(fadeOutHalf);
  }

  /**
   * Changes UI to resumed mode
   */
  private void deactivatePause() {
    mButtonPause.setText("Pause");
    Animation fadeInHalf = AnimationUtils.loadAnimation(getActivity(), R.anim.fade_in_half);
    fadeInHalf.setAnimationListener(new EnableReadingControls(true));
    mButtonDone.startAnimation(fadeInHalf);
//    mLayoutSessionTimer.startAnimation(fadeInHalf);
  }

  private void stopTimingAndUpdateElapsed() {
    mElapsed += elapsedSinceTimestamp();
    mTimestampLastStarted = 0;
    stopTrackerUpdates();
  }

  private void startTiming() {
    mTimestampLastStarted = System.currentTimeMillis();
    startTrackerUpdates();
  }

  // Timing events

  private boolean isTiming() {
    return mTimestampLastStarted > 0;
  }

  private long elapsedSinceTimestamp() {
    if(isTiming()) {
      return System.currentTimeMillis() - mTimestampLastStarted;
    }
    return 0;
  }

  private long elapsed() {
    return mElapsed + elapsedSinceTimestamp();
  }

  // Sets the billboard to show the elapsed time
  private void presentTime(long milliseconds) {
    int[] hms = Utils.convertMillisToHoursMinutesSeconds(milliseconds);
    final int hours = hms[0];
    final int minutes = hms[1];

    String summary;
    if(hours > 0) {
      summary = String.format("%s, %s",
        Utils.pluralizeWithCount(hours, "hour"),
        Utils.pluralizeWithCount(minutes, "minute")
      );
    } else if(minutes > 0) {
      summary = Utils.pluralizeWithCount(minutes, "minute");
    } else {
      summary = "< 1 minute";
    }
    mTextBillboard.setText(summary);
  }

  private void startTrackerUpdates() {
    stopTrackerUpdates();

    PauseableSpinAnimation spinAnimation = (PauseableSpinAnimation) mTimeSpinner.getAnimation();
    if(spinAnimation == null) {
      final float offsetX = mTimeSpinner.getWidth() / 2.0f;
      final float offsetY = mTimeSpinner.getHeight() / 2.0f;
      spinAnimation = new PauseableSpinAnimation(0, 360, offsetX, offsetY) {{
        setRepeatMode(Animation.RESTART);
        setRepeatCount(Animation.INFINITE);
        setDuration(60 * 1000);
        setInterpolator(new LinearInterpolator());
        setFillAfter(true);
      }};
      mTimeSpinner.setAnimation(spinAnimation);
      spinAnimation.start();
    } else {
      spinAnimation.resume();
    }

    mRedrawTimerTask = new RedrawTimerTask();
    //noinspection unchecked
    mRedrawTimerTask.execute();
  }

  private void stopTrackerUpdates() {
    PauseableSpinAnimation spinAnimation = (PauseableSpinAnimation) mTimeSpinner.getAnimation();
    if(spinAnimation != null) {
      spinAnimation.pause();
    }

    if(mRedrawTimerTask != null) {
      mRedrawTimerTask.cancel(true);
      mRedrawTimerTask = null;
    }
  }

  private class RedrawTimerTask extends AsyncTask<Void, Void, Void> {
    private static final int UPDATE_INTERVAL = 1000;

    // TODO display a notification while reading is active

    @Override
    protected Void doInBackground(Void... voids) {
      try {
        while(!isCancelled()) {
          //noinspection unchecked
          publishProgress();
          Thread.sleep(UPDATE_INTERVAL);
        }
        return null;
      } catch(InterruptedException ignored) {
        return null;
      }
    }

    @Override
    protected void onProgressUpdate(Void... values) {
      presentTime(elapsed());
    }
  }

  private class EnableReadingControls implements Animation.AnimationListener {
    private boolean enabled = false;

    public EnableReadingControls(boolean enabled) {
      this.enabled = enabled;
    }

    @Override public void onAnimationEnd(Animation animation) {
      if(!this.enabled) {
        mButtonDone.setBackgroundDrawable(getResources().getDrawable(R.drawable.default_button_no_states));
        mButtonDone.setEnabled(false);
      }
    }

    @Override public void onAnimationStart(Animation animation) {
      if(this.enabled) {
        mButtonDone.setEnabled(true);
        mButtonDone.setBackgroundDrawable(getResources().getDrawable(R.drawable.default_button_background));
      }
    }

    @Override public void onAnimationRepeat(Animation animation) {
    }
  }
}
