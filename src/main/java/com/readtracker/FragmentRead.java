package com.readtracker;

import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import com.readtracker.db.LocalReading;
import com.readtracker.interfaces.SaveLocalReadingListener;
import com.readtracker.tasks.SaveLocalReadingTask;
import com.readtracker.thirdparty.SafeViewFlipper;
import com.readtracker.thirdparty.widget.WheelView;
import com.readtracker.thirdparty.widget.adapters.NumericWheelAdapter;
import com.readtracker.value_objects.ReadingState;

/**
 * Fragment for managing a reading session
 */
public class FragmentRead extends Fragment {
  private static final String TAG = FragmentRead.class.getName();

  private static Button mButtonStart;
  private static Button mButtonPause;
  private static Button mButtonDone;

  private static EditText mEditPageCount;

  // Flipper for showing reading setup vs. reading session
  private static SafeViewFlipper mFlipperReadingSession;

  private static TextView mTextNumberOfPages;
  private static Button mButtonUpdatePageNumbers;

  private static ViewGroup mLayoutSessionTimer;

  // Flipper for showing start vs. pause/done
  private static SafeViewFlipper mFlipperSessionControl;

  private static TextView mTextHeader;
  private static TextView mTextSummary;

  // Reading to track
  private LocalReading mLocalReading;

  // Timing
  private RedrawTimerTask mRedrawTimerTask;
  // Timestamp of when play/resume was pressed last time
  private long mTimestampLastStarted = 0;
  // Accumulated elapsed time, not including the time since latest timestamp
  private long mElapsed = 0;

  // Force reinitialize when returning from an activity that is known
  // to cause data updates (notably, the create highlight activity)
  private boolean mForceReInitialize;

  // Display child index for flipper session control
  private static final int START = 0;
  private static final int PAUSE_DONE = 1;

  // Pages for flipper between reading session and page information entry
  private static final int PAGE_READING_SESSION = 0;
  private static final int PAGE_PAGE_ENTRY = 1;

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

    if(mLocalReading != null) {
      Log.d(TAG, "Loaded with local reading: " + mLocalReading.getInfo());
      // Show the book initialization screen or the read tracker
      if(mLocalReading.hasPageInfo()) {
        mFlipperReadingSession.setDisplayedChild(PAGE_READING_SESSION);
      } else {
        String filledNumberOfPagesText = String.format(getString(R.string.please_enter_number_of_pages), mLocalReading.title);
        mTextNumberOfPages.setText(filledNumberOfPagesText);
        mFlipperReadingSession.setDisplayedChild(PAGE_PAGE_ENTRY);
      }
    } else {
      Log.d(TAG, "Loaded without local reading");
    }

    mFlipperSessionControl.setDisplayedChild(START);

    // Rig for a fresh session
    if(mLocalReading != null) {
      if(mElapsed == 0) {
        mTextHeader.setText("Continuing from");
        mTextSummary.setText(mLocalReading.describeCurrentPosition());
      } else {
        refreshElapsedTime();
        mTextHeader.setText(getString(R.string.paused_at));
      }
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
    refreshElapsedTime();
  }

  @Override
  public void onResume() {
    super.onResume();
    Log.d(TAG, "onResume()");
    if(!mForceReInitialize) {
      loadTimingState();
    } else {
      Log.d(TAG, "Skipping loading of stored session due to forced initialize");
    }
  }

  private void bindViews(View view) {
    mFlipperReadingSession = (SafeViewFlipper) view.findViewById(R.id.flipperReadingSession);
    mEditPageCount = (EditText) view.findViewById(R.id.editPageCount);
    mTextNumberOfPages = (TextView) view.findViewById(R.id.textEnterNumberOfPages);
    mButtonUpdatePageNumbers = (Button) view.findViewById(R.id.buttonUpdatePageNumbers);

    mButtonDone = (Button) view.findViewById(R.id.buttonDone);
    mButtonStart = (Button) view.findViewById(R.id.buttonStart);
    mButtonPause = (Button) view.findViewById(R.id.buttonPause);

    mFlipperSessionControl = (SafeViewFlipper) view.findViewById(R.id.flipperSessionControl);
    mLayoutSessionTimer = (ViewGroup) view.findViewById(R.id.layoutSessionTimer);

    mTextHeader = (TextView) mLayoutSessionTimer.findViewById(R.id.textHeader);
    mTextSummary = (TextView) mLayoutSessionTimer.findViewById(R.id.textSummary);
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

    mButtonUpdatePageNumbers.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View view) {
        onClickedSetPageCount();
      }
    });

    if(!mLocalReading.hasPageInfo()) {
      mTextNumberOfPages.setText(String.format(getString(R.string.please_enter_number_of_pages, mLocalReading.title)));
    }
  }

  private void setElapsed(long elapsed) {
    mElapsed = elapsed;
  }

  public void setLocalReading(LocalReading localReading) {
    mLocalReading = localReading;
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
    final Animation disappearSummary = AnimationUtils.loadAnimation(getActivity(), R.anim.fade_out);

    final Animation appear = AnimationUtils.loadAnimation(getActivity(), R.anim.slide_up_appear);
    final Animation appearSummary = AnimationUtils.loadAnimation(getActivity(), R.anim.slide_up_appear);

    disappear.setAnimationListener(new Animation.AnimationListener() {
      @Override public void onAnimationStart(Animation animation) { }
      @Override public void onAnimationRepeat(Animation animation) { }

      @Override public void onAnimationEnd(Animation animation) {
        mTextHeader.startAnimation(appear);
        mTextHeader.setText(getString(R.string.reading_for));
      }
    });

    disappearSummary.setAnimationListener(new Animation.AnimationListener() {
      @Override public void onAnimationStart(Animation animation) { }
      @Override public void onAnimationRepeat(Animation animation) { }

      @Override public void onAnimationEnd(Animation animation) {
        appearSummary.setStartOffset(100);
        mTextSummary.startAnimation(appearSummary);
        startTiming();
      }
    });

    mTextHeader.startAnimation(disappear);
    disappearSummary.setStartOffset(200);
    mTextSummary.startAnimation(disappearSummary);

    ((ActivityBook) getActivity()).setDirty(true);
    mFlipperSessionControl.setDisplayedChild(PAUSE_DONE);
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

  private void configureSessionTimer() {
    //    configureWheelForMaximumValue(mWheelHours, 48);
    //    configureWheelForMaximumValue(mWheelMinutes, 59);
    //    configureWheelForMaximumValue(mWheelSeconds, 59);
    //
    //    mWheelHours.setCyclic(false);
    //    mWheelMinutes.setCyclic(true);
    //    mWheelSeconds.setCyclic(true);
  }

  private void configureWheelForMaximumValue(WheelView view, int maxNum) {
    NumericWheelAdapter adapter = new NumericWheelAdapter(getActivity(), 0, maxNum);

    adapter.setTextColor(getResources().getColor(R.color.text_color_primary));
    adapter.setTypeFace(Typeface.DEFAULT);
    adapter.setTypeStyle(Typeface.NORMAL);
    adapter.setTextSize(36);

    view.setInterpolator(new DecelerateInterpolator());
    view.setScrollingSpeed(200);

    view.setViewAdapter(adapter);
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

    // Reset to a "reading" state
    mButtonStart.setVisibility(View.INVISIBLE);
    mButtonPause.setVisibility(View.VISIBLE);
    mButtonDone.setVisibility(View.VISIBLE);

    // Check if we should automatically start the timer
    if(readingState.isActive()) {
      Log.d(TAG, "Got active reading state, starting timer");
      deactivatePause();
      startTrackerUpdates();
    } else {
      Log.d(TAG, "Got inactive reading state");
      activatePause();
    }
    refreshElapsedTime();
  }

  /**
   * Called when save has been clicked on the set number of pages dialog
   */
  private void onClickedSetPageCount() {
    int pageNumbers = Utils.parseInt(mEditPageCount.getText().toString(), 0);

    if(pageNumbers < 1 || pageNumbers > 10000) {
      ((ReadTrackerActivity) getActivity()).toast("Please input a reasonable number of pages");
      mEditPageCount.requestFocus();
      return;
    }

    mLocalReading.totalPages = pageNumbers;

    ((ActivityBook) getActivity()).onLocalReadingChanged();

    SaveLocalReadingTask.save(mLocalReading, new SaveLocalReadingListener() {
      @Override
      public void onLocalReadingSaved(LocalReading localReading) {
        // Flip back to active reading state
        mFlipperReadingSession.setDisplayedChild(1);
      }
    });
  }

  /**
   * Changes UI to pause mode
   */
  private void activatePause() {
    mButtonPause.setText("Resume");
    Animation fadeOutHalf = AnimationUtils.loadAnimation(getActivity(), R.anim.fade_out_half);
    fadeOutHalf.setAnimationListener(new EnableReadingControls(false));
    mButtonDone.startAnimation(fadeOutHalf);
    mLayoutSessionTimer.startAnimation(fadeOutHalf);
  }

  /**
   * Changes UI to resumed mode
   */
  private void deactivatePause() {
    mButtonPause.setText("Pause");
    Animation fadeInHalf = AnimationUtils.loadAnimation(getActivity(), R.anim.fade_in_half);
    fadeInHalf.setAnimationListener(new EnableReadingControls(true));
    mButtonDone.startAnimation(fadeInHalf);
    mLayoutSessionTimer.startAnimation(fadeInHalf);
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

  private void refreshElapsedTime() {
    long[] hms = Utils.convertMillisToHoursMinutesSeconds(elapsed());
    int hours = (int) hms[0];
    int minutes = (int) hms[1];
    int seconds = (int) hms[2];

    String summary = "";
    if(hours > 0) {
      summary = String.format("%s\n%s",
          Utils.pluralizeWithCount(hours, "hour"),
          Utils.pluralizeWithCount(minutes, "minute")
      );
    } else if(minutes > 0) {
      summary = Utils.pluralizeWithCount(minutes, "minute");
    } else {
      summary = seconds + " seconds";
    }

    mTextHeader.setText(getString(R.string.reading_for));
    mTextSummary.setText(summary);
  }


  private void startTrackerUpdates() {
    stopTrackerUpdates();
    mRedrawTimerTask = new RedrawTimerTask();
    //noinspection unchecked
    mRedrawTimerTask.execute();
  }

  private void stopTrackerUpdates() {
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
      refreshElapsedTime();
    }
  }

  /**
   * Disable/enable reading controls (pause/done) after or before their
   * animations has completed.
   */
  private class EnableReadingControls implements Animation.AnimationListener {
    private boolean enabled = false;

    public EnableReadingControls(boolean enabled) {
      this.enabled = enabled;
    }

    private void setControlsEnabled(boolean enabled) {
    }

    @Override public void onAnimationEnd(Animation animation) {
      if(!this.enabled) {
        mButtonDone.setBackgroundDrawable(getResources().getDrawable(R.drawable.default_button_no_states));
        mTextHeader.setText(getString(R.string.paused_at));
        mButtonDone.setEnabled(false);
      }
    }

    @Override public void onAnimationStart(Animation animation) {
      if(this.enabled) {
        mButtonDone.setEnabled(true);
        mButtonDone.setBackgroundDrawable(getResources().getDrawable(R.drawable.default_button));
      }
    }

    @Override public void onAnimationRepeat(Animation animation) {
    }
  }
}
