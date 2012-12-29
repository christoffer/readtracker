package com.readtracker_beta.fragments;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.*;
import android.widget.Button;
import android.widget.TextView;
import com.readtracker_beta.IntentKeys;
import com.readtracker_beta.R;
import com.readtracker_beta.activities.BookActivity;
import com.readtracker_beta.interfaces.SessionTimerEventListener;
import com.readtracker_beta.support.SessionTimerStore;
import com.readtracker_beta.support.Utils;
import com.readtracker_beta.custom_views.PauseableSpinAnimation;
import com.readtracker_beta.custom_views.TimeSpinner;
import com.readtracker_beta.db.LocalReading;
import com.readtracker_beta.thirdparty.SafeViewFlipper;
import com.readtracker_beta.support.SessionTimer;

/**
 * Fragment for managing a reading session
 */
public class ReadingFragment extends Fragment {
  private static final String TAG = ReadingFragment.class.getName();

  // Session controls
  private static Button mButtonStart;
  private static Button mButtonPause;
  private static Button mButtonDone;

  // Time tracking
  private static TextView mTextBillboard;
  private static TimeSpinner mTimeSpinner;

  // Flipper for showing start vs. stop/done
  private static SafeViewFlipper mFlipperSessionControl;

  // Reading to track
  private LocalReading mLocalReading;

  // Timing
  private SessionTimer mSessionTimer;
  private RedrawTimerTask mRedrawTimerTask;

//  // Timestamp of when play/resume was pressed last time
//  private long mTimestampLastStarted = 0;
//
//  // Accumulated elapsed time, not including the time since latest timestamp
//  private long mElapsed = 0;

  // Force reinitialize when returning from an activity that is known
  // to cause data updates.
  private boolean mForceReInitialize;

  // Display child index for flipper session control
  private static final int FLIPPER_PAGE_START_BUTTON = 0;
  private static final int FLIPPER_PAGE_READING_BUTTONS = 1;

  public static Fragment newInstance(LocalReading localReading, SessionTimer initialSessionTimer) {
    Log.d(TAG, "newInstance()");
    ReadingFragment instance = new ReadingFragment();
    if(initialSessionTimer == null) {
      Log.v(TAG, "Initializing with new session timer");
      initialSessionTimer = new SessionTimer(localReading.id);
    }
    instance.setSessionTimer(initialSessionTimer);
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
      SessionTimer sessionTimer = savedInstanceState.getParcelable(IntentKeys.SESSION_TIMER);
      setSessionTimer(sessionTimer);
    }
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    Log.d(TAG, "onCreateView()");
    View view = inflater.inflate(R.layout.fragment_read, container, false);
    bindViews(view);
    bindEvents();

    mFlipperSessionControl.setDisplayedChild(FLIPPER_PAGE_START_BUTTON);

    if(mLocalReading == null) { // TODO investigate when this could happen...
      Log.w(TAG, "Loaded without local reading");
      return view;
    }

    Log.i(TAG, "Loaded with local reading: " + mLocalReading.getInfo());

    // Show the book initialization screen or the read tracker
    if(mLocalReading.hasPageInfo()) {
      setupForTimeTracking();
    } else {
      setupForMissingPages();
    }

    mTimeSpinner.setColor(mLocalReading.getColor());

    return view;
  }

  @Override
  public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    Log.d(TAG, "freezing state");
    outState.putParcelable(IntentKeys.LOCAL_READING, mLocalReading);
    outState.putParcelable(IntentKeys.SESSION_TIMER, mSessionTimer);
  }

  @Override
  public void onPause() {
    super.onPause();
    Log.d(TAG, "onPause()");

    if(((BookActivity) getActivity()).isManualShutdown()) {
      Log.d(TAG, "Parent Activity is shutting down - don't store state");
    } else {
      if(getElapsed() > 0) {
        Log.d(TAG, "Parent Activity not shutting down and has active state - store state");
        SessionTimerStore.store(mSessionTimer);
      }
    }

    stopTrackerUpdates();
  }

  @Override
  public void onResume() {
    super.onResume();
    Log.d(TAG, "onResume() with " + (mForceReInitialize ? "forced" : "non-forced") + " initialize");
    if(!mForceReInitialize) {
      Log.d(TAG, "Loading stored timing state");
      SessionTimer storedSessionTimer = SessionTimerStore.load();

      if(storedSessionTimer == null) {
        Log.d(TAG, "... Not found");
      } else {
        setSessionTimer(storedSessionTimer);
        restoreTimingState(storedSessionTimer);
      }
    } else {
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
        onClickedPauseResume();
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

    // The spinner animation depends on knowledge of the size of the time tracker
    // widget since it pivots around the center of it.
    // This seems to be the most reliant way of knowing what that dimension is available.
    ViewTreeObserver obs = mTimeSpinner.getViewTreeObserver();
    obs.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
      @Override public void onGlobalLayout() {
        if(mTimeSpinner.getWidth() == 0) {
          return; // width not available yet
        }
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
        }

        if(mSessionTimer.isActive()) {
          spinAnimation.start();
        } else {
          spinAnimation.pause();
        }
      }
    });
  }

  /**
   * Flips the button view flipper to the given page.
   * Does not change if the given page is already active (to avoid re-activating animations).
   */
  private void flipToButtonPage(int page) {
    if(mFlipperSessionControl.getDisplayedChild() != page) {
      mFlipperSessionControl.setDisplayedChild(page);
    }
  }

  private void setSessionTimer(SessionTimer sessionTimer) {
    Log.v(TAG, "Setting session timer: " + sessionTimer);

    sessionTimer.setEventListener(new SessionTimerEventListener() {
      @Override public void onStarted() {
        startTrackerUpdates();
        setupPauseMode();
      }
      @Override public void onStopped() {
        stopTrackerUpdates();
        setupResumeMode();
      }
    });

    mSessionTimer = sessionTimer;
  }

  public void setLocalReading(LocalReading localReading) {
    mLocalReading = localReading;
  }

  private void setupForTimeTracking() {
    // Starting or continuing a reading session?
    final long totalElapsed = getElapsed();

    if(totalElapsed == 0) {
      describeLastPosition(mLocalReading);
      setupStartMode();
    } else {
      presentTime(totalElapsed);
      setupResumeMode();
    }
  }

  private void setupForMissingPages() {
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
  public SessionTimer getSessionTimer() {
    return mSessionTimer;
  }

  /**
   * Called when the start button is clicked
   */
  private void onClickedStart() {
    // Handle clicking "Edit book"
    if(!mLocalReading.hasPageInfo()) {
      ((BookActivity) getActivity()).exitToBookInfoScreen(mLocalReading);
      return;
    }

    final Animation disappear = AnimationUtils.loadAnimation(getActivity(), R.anim.fade_out);
    final Animation appear = AnimationUtils.loadAnimation(getActivity(), R.anim.slide_up_appear);

    disappear.setAnimationListener(new Animation.AnimationListener() {
      @Override public void onAnimationStart(Animation animation) { }
      @Override public void onAnimationRepeat(Animation animation) { }
      @Override public void onAnimationEnd(Animation animation) {
        mSessionTimer.start();
        presentTime(getElapsed());
        mTextBillboard.startAnimation(appear);
      }
    });

    mTextBillboard.startAnimation(disappear);
    mFlipperSessionControl.setDisplayedChild(FLIPPER_PAGE_READING_BUTTONS);
  }

  /**
   * Called when the pause button is clicked
   */
  private void onClickedPauseResume() {
    mSessionTimer.togglePause();
  }

  /**
   * Called when the done button is clicked
   */
  private void onClickedDone() {
    mSessionTimer.stop();
    final long elapsed = mSessionTimer.getTotalElapsed();
    ((BookActivity) getActivity()).exitToSessionEndScreen(elapsed);
  }

  // Causes the activity to reload the LocalReading
  private void setForceReinitialize(boolean forceReinitialize) {
    mForceReInitialize = forceReinitialize;
  }

  /**
   * Restores the current timing state to a given one
   *
   * @param sessionTimer The reading state to restore to
   */
  public void restoreTimingState(SessionTimer sessionTimer) {
    Log.i(TAG, "Restoring session: " + sessionTimer);

    mFlipperSessionControl.setDisplayedChild(FLIPPER_PAGE_READING_BUTTONS);

    // Check if we should automatically start the timer
    if(sessionTimer.isActive()) {
      Log.d(TAG, "Got active reading state");
      setupPauseMode();
      startTrackerUpdates();
    } else {
      Log.d(TAG, "Got inactive reading state");
      setupResumeMode();
    }

    presentTime(getElapsed());
  }

  /**
   * Changes UI to start mode
   */
  private void setupStartMode() {
    mButtonStart.setText("Start");
    flipToButtonPage(FLIPPER_PAGE_START_BUTTON);
  }

  /**
   * Changes UI to pause mode
   */
  private void setupResumeMode() {
    mButtonPause.setText("Resume");
    flipToButtonPage(FLIPPER_PAGE_READING_BUTTONS);
    Animation fadeOutHalf = AnimationUtils.loadAnimation(getActivity(), R.anim.fade_out_half);
    fadeOutHalf.setAnimationListener(new EnableReadingControls(false));
    mButtonDone.startAnimation(fadeOutHalf);
  }

  /**
   * Changes UI to resumed mode
   */
  private void setupPauseMode() {
    mButtonPause.setText("Pause");
    flipToButtonPage(FLIPPER_PAGE_READING_BUTTONS);
    Animation fadeInHalf = AnimationUtils.loadAnimation(getActivity(), R.anim.fade_in_half);
    fadeInHalf.setAnimationListener(new EnableReadingControls(true));
    mButtonDone.startAnimation(fadeInHalf);
  }

  // Timing events

  private long getElapsed() {
    return mSessionTimer.getTotalElapsed();
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
      summary = "Tracking...";
    }
    mTextBillboard.setText(summary);
  }

  private void startTrackerUpdates() {
    stopTrackerUpdates();

    PauseableSpinAnimation spinAnimation = (PauseableSpinAnimation) mTimeSpinner.getAnimation();
    if(spinAnimation != null) {
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

    // Clear out the redraw timer
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
      presentTime(getElapsed());
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
        mButtonDone.setBackgroundDrawable(getResources().getDrawable(R.drawable.default_toggle_button_background));
      }
    }

    @Override public void onAnimationRepeat(Animation animation) {
    }
  }
}
