package com.readtracker.android.fragments;

import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.TextView;

import com.readtracker.android.R;
import com.readtracker.android.activities.BaseActivity;
import com.readtracker.android.activities.BookActivity;
import com.readtracker.android.activities.EndSessionDialog;
import com.readtracker.android.custom_views.PauseableSpinAnimation;
import com.readtracker.android.custom_views.TimeSpinner;
import com.readtracker.android.db.Book;
import com.readtracker.android.interfaces.SessionTimerEventListener;
import com.readtracker.android.support.DrawableGenerator;
import com.readtracker.android.support.SessionTimer;
import com.readtracker.android.support.SessionTimerStore;
import com.readtracker.android.support.Utils;
import com.readtracker.android.thirdparty.SafeViewFlipper;
import com.readtracker.android.thirdparty.widget.OnWheelChangedListener;
import com.readtracker.android.thirdparty.widget.WheelView;
import com.readtracker.android.thirdparty.widget.adapters.ArrayWheelAdapter;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

/**
 * Fragment for managing a reading session
 */
public class ReadFragment extends Fragment {
  private static final String TAG = ReadFragment.class.getName();
  private static final String KEY_SESSION_TIMER = "SESSION_TIMER";

  private static final String END_SESSION_FRAGMENT_TAG = "end-session-tag";

  private View mRootView;

  // Session controls
  private Button mButtonStart;
  private Button mButtonPause;
  private Button mButtonDone;

  // Time tracking
  private TextView mTextBillboard;
  private TimeSpinner mTimeSpinner;
  // Wrap the spinner to apply the pulse animation on pause without
  // disrupting the time spinner animation
  private ViewGroup mLayoutTimeSpinnerWrapper;

  // Flipper for showing start vs. stop/done
  private SafeViewFlipper mFlipperSessionControl;

  // Book to track reading for
  private Book mBook;

  // Timing
  private SessionTimer mSessionTimer;
  private UpdateDurationTask mUpdateDurationTask;

  private WheelView mWheelDuration;

  private boolean mIsStarted = false;

  // Display child index for flipper session control
  private static final int FLIPPER_PAGE_START_BUTTON = 0;
  private static final int FLIPPER_PAGE_READING_BUTTONS = 1;

  private Bus mBus;


  public static Fragment newInstance() {
    Log.v(TAG, "Creating new instance of ReadFragment");
    return new ReadFragment();
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    Log.v(TAG, "onCreate()");
    super.onCreate(savedInstanceState);
    mSessionTimer = new SessionTimer();
    setSessionTimer(mSessionTimer);
  }

  @Override public void onActivityCreated(Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);

    mBus = ((BaseActivity) getActivity()).getBus();
    mBus.register(this);
  }

  @Override public void onDetach() {
    super.onDetach();
    mBus.unregister(this);
  }

  @Subscribe public void onBookLoadedEvent(BookActivity.BookLoadedEvent event) {
    Log.v(TAG, "Got book loaded event: " + event);
    mBook = event.getBook();
    populateFieldsDeferred();
  }

  @Override
  public void onPause() {
    super.onPause();
    Log.d(TAG, "onPause()");

    // TODO replace with events
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

    Log.d(TAG, "Loading stored timing state");
    SessionTimer storedSessionTimer = SessionTimerStore.load();

    if(storedSessionTimer == null) {
      Log.d(TAG, "... Not found");
      storedSessionTimer = new SessionTimer();
    }

    setSessionTimer(storedSessionTimer);
    restoreTimingState(storedSessionTimer);
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    Log.d(TAG, "onCreateView()");
    View view = inflater.inflate(R.layout.fragment_read, container, false);

    bindViews(view);

    mFlipperSessionControl.setDisplayedChild(FLIPPER_PAGE_START_BUTTON);

    populateFieldsDeferred();

    return view;
  }

  private void populateFieldsDeferred() {
    if(mBook == null || mRootView == null) {
      return;
    }

    Log.v(TAG, "Populating fields for book: " + mBook);

    // Show the book initialization screen or the read tracker
    if(mBook.hasPageNumbers()) {
      setupForTimeTracking();
    } else {
      setupForMissingPages();
    }

    final int bookColor = Utils.calculateBookColor(mBook);

    mTimeSpinner.setColor(bookColor);
    mTimeSpinner.setMaxSize(500);

    mButtonStart.setBackgroundDrawable(DrawableGenerator.generateButtonBackground(bookColor));
    mButtonPause.setBackgroundDrawable(DrawableGenerator.generateButtonBackground(bookColor));
    mButtonDone.setBackgroundDrawable(DrawableGenerator.generateButtonBackground(bookColor));

    initializeDurationWheel();

    bindEvents();
  }

  private void setSessionTimer(SessionTimer sessionTimer) {
    Log.v(TAG, "Setting session timer: " + sessionTimer);

    sessionTimer.setEventListener(new SessionTimerEventListener() {
      @Override public void onStarted() {
        startTrackerUpdates();
        displayPausableControls();
      }

      @Override public void onStopped() {
        stopTrackerUpdates();
        displayResumableControls();
      }
    });

    mSessionTimer = sessionTimer;
  }

  private void bindViews(View view) {
    mButtonDone = (Button) view.findViewById(R.id.buttonDone);
    mButtonStart = (Button) view.findViewById(R.id.buttonStart);
    mButtonPause = (Button) view.findViewById(R.id.buttonPause);

    mFlipperSessionControl = (SafeViewFlipper) view.findViewById(R.id.flipperSessionControl);

    mTextBillboard = (TextView) view.findViewById(R.id.textBillboard);
    mTimeSpinner = (TimeSpinner) view.findViewById(R.id.timespinner);

    mWheelDuration = (WheelView) view.findViewById(R.id.wheelDuration);

    mLayoutTimeSpinnerWrapper = (ViewGroup) view.findViewById(R.id.layoutTimeSpinnerWrapper);

    mRootView = view;
  }

  private void bindEvents() {
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

    /*
      The spinner animation depends on knowledge of the size of the time tracker
      widget since it pivots around the center of it.
      This seems to be the most reliant way of knowing what that dimension is available.
    */
    ViewTreeObserver obs = mTimeSpinner.getViewTreeObserver();
    obs.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
      @Override public void onGlobalLayout() {
        if(mTimeSpinner.getWidth() == 0) {
          return; // width not available yet
        }
        PauseableSpinAnimation spinAnimation = (PauseableSpinAnimation) mTimeSpinner.getAnimation();
        if(spinAnimation == null) {
          Log.v(TAG, "View has layout: Setting up spinner animation");
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
          Log.v(TAG, "Session timer already active, starting animation");
          spinAnimation.start();
        } else {
          Log.v(TAG, "Session timer not active, pausing animation");
          spinAnimation.pause();
        }
      }
    });

    mTimeSpinner.setOnTouchListener(new View.OnTouchListener() {
      @Override public boolean onTouch(View view, MotionEvent motionEvent) {
        // The start wheel is not active as a click target when the timing is started
        // this is due to the inability to have both the TimeSpinner and the underlying
        // wheel view receive touch events prior to android 11.
        if(mWheelDuration == null || mWheelDuration.isEnabled()) {
          return false;
        }

        int action = motionEvent.getAction();
        if(action == MotionEvent.ACTION_DOWN) {
          mTimeSpinner.setHighlighted(true);
          return true;
        } else if(action == MotionEvent.ACTION_UP) {
          if(mIsStarted) {
            onClickedPauseResume();
          } else {
            onClickedStart();
          }
          mTimeSpinner.setHighlighted(false);
          return true;
        } else if(action == MotionEvent.ACTION_CANCEL) {
          mTimeSpinner.setHighlighted(false);
          return true;
        }
        return false;
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

  /**
   * Initializes the wheel view for displaying the reading session duration
   */
  private void initializeDurationWheel() {
    ArrayWheelAdapter hoursAdapter = createDurationWheelAdapter(24 * 60);
    mWheelDuration.setVisibleItems(3);
    mWheelDuration.setViewAdapter(hoursAdapter);
    mWheelDuration.setCalliperMode(WheelView.CalliperMode.NO_CALLIPERS);

    // Have the wheel duration initially invisible, and show it once timing starts
    mWheelDuration.setVisibility(View.INVISIBLE);
    mWheelDuration.setEnabled(false);

    mWheelDuration.addChangingListener(new OnWheelChangedListener() {
      @Override
      public void onChanged(WheelView wheel, int oldValue, int newValue) {
        if(mSessionTimer != null) {
          int elapsed = newValue * 60 * 1000;
          mSessionTimer.setElapsedMillis(elapsed);
          PauseableSpinAnimation currentAnimation = (PauseableSpinAnimation) mTimeSpinner.getAnimation();
          if(currentAnimation != null) {
            mTimeSpinner.startAnimation(currentAnimation);
          }
        }
      }
    });
  }

  /**
   * Creates an adapter to display the duration in a human readable form.
   *
   * @param maxMinutes the maximum time (in minutes) to show
   * @return an ArrayWheelAdapter for showing duration
   */
  private ArrayWheelAdapter createDurationWheelAdapter(int maxMinutes) {
    String[] labels = new String[maxMinutes];
    for(int minute = 0; minute < maxMinutes; minute++) {
      labels[minute] = Utils.hoursAndMinutesFromMillis(minute * 60 * 1000);
    }

    ArrayWheelAdapter<String> adapter = new ArrayWheelAdapter<String>(getActivity(), labels);
    adapter.setTextColor(getResources().getColor(R.color.text_color_primary));
    adapter.setTypeFace(Typeface.DEFAULT);
    adapter.setTypeStyle(Typeface.NORMAL);
    float fontSizePixels = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 12, getResources().getDisplayMetrics());
    adapter.setTextSize((int) fontSizePixels);

    return adapter;
  }

  /**
   * Sets up the UI for tracking the time of a book.
   */
  private void setupForTimeTracking() {
    // Starting or continuing a reading session?
    final long totalElapsed = getElapsed();

    if(totalElapsed == 0) {
      describeLastPosition();
      setupStartMode();
    } else {
      updateDuration(totalElapsed);
      displayResumableControls();
    }
  }

  /**
   * Sets up the UI for asking the user for the number of pages in the book.
   */
  private void setupForMissingPages() {
    mTextBillboard.setText(R.string.reading_one_more_step);
    mTextBillboard.setEnabled(false);
    mButtonStart.setText(R.string.reading_set_book_lenght);
  }

  /**
   * Updates the text header and summary to show where the user last left off.
   * Handles pages/percent and shows a special text for first session.
   */
  private void describeLastPosition() {
    if(mBook.hasCurrentPosition()) {
      mTextBillboard.setText(R.string.reading_click_to_start);
      return;
    }

    if(mBook.hasPageNumbers()) {
      mTextBillboard.setText(getString(R.string.reading_last_on_page, mBook.getCurrentPageName()));
    } else {
      mTextBillboard.setText(getString(R.string.reading_last_at, mBook.getCurrentPageName()));
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
    if(!mBook.hasPageNumbers()) {
      // TODO Replace with event
      ((BookActivity) getActivity()).exitToBookEditScreen(mBook);
      return;
    }

    final Animation disappear = AnimationUtils.loadAnimation(getActivity(), R.anim.fade_out);
    final Animation appear = AnimationUtils.loadAnimation(getActivity(), R.anim.slide_up_appear);

    //noinspection ConstantConditions
    disappear.setAnimationListener(new Animation.AnimationListener() {
      @Override public void onAnimationStart(Animation animation) { }

      @Override public void onAnimationRepeat(Animation animation) { }

      @Override public void onAnimationEnd(Animation animation) {
        mSessionTimer.start();
        updateDuration(getElapsed());
        mTextBillboard.setVisibility(View.INVISIBLE);
        mWheelDuration.startAnimation(appear);
        mWheelDuration.setVisibility(View.VISIBLE);
      }
    });

    mFlipperSessionControl.setDisplayedChild(FLIPPER_PAGE_READING_BUTTONS);
    mTextBillboard.startAnimation(disappear);
    mIsStarted = true;
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

    EndSessionDialog dialog = new EndSessionDialog();

    Bundle arguments = new Bundle();
    arguments.putInt(EndSessionDialog.ARG_BOOK_ID, mBook.getId());
    arguments.putLong(EndSessionDialog.ARG_SESSION_LENGTH_MS, elapsed);
    dialog.setArguments(arguments);

    FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
    dialog.show(fragmentManager, END_SESSION_FRAGMENT_TAG);
  }

  /**
   * Restores the current timing state to a given one
   *
   * @param sessionTimer The reading state to restore to
   */
  public void restoreTimingState(SessionTimer sessionTimer) {
    Log.i(TAG, "Restoring session: " + sessionTimer);

    mFlipperSessionControl.setDisplayedChild(FLIPPER_PAGE_READING_BUTTONS);
    mTextBillboard.setVisibility(View.GONE);
    mWheelDuration.setVisibility(View.VISIBLE);

    mIsStarted = true;

    // Check if we should automatically start the timer
    if(sessionTimer.isActive()) {
      Log.d(TAG, "Got active reading state");
      displayPausableControls();
      startTrackerUpdates();
    } else {
      Log.d(TAG, "Got inactive reading state");
      displayResumableControls();
    }

    updateDuration(getElapsed());
  }

  /**
   * Changes UI to start mode
   */
  private void setupStartMode() {
    mButtonStart.setText(R.string.reading_start);
    flipToButtonPage(FLIPPER_PAGE_START_BUTTON);
  }

  /**
   * Changes UI to pause mode
   */
  private void displayResumableControls() {
    mButtonPause.setText(R.string.reading_resume);
    mWheelDuration.setEnabled(false);
    flipToButtonPage(FLIPPER_PAGE_READING_BUTTONS);
    Animation pulse = AnimationUtils.loadAnimation(getActivity(), R.anim.pulse);
    mLayoutTimeSpinnerWrapper.startAnimation(pulse);
  }

  /**
   * Changes UI to resumed mode
   */
  private void displayPausableControls() {
    mButtonPause.setText(R.string.reading_pause);
    flipToButtonPage(FLIPPER_PAGE_READING_BUTTONS);
    mLayoutTimeSpinnerWrapper.setAnimation(null); // Cancel the pulse
  }

  // Timing events

  /**
   * Gets the elapsed time in milliseconds.
   *
   * @return the elapsed time in milliseconds.
   */
  private long getElapsed() {
    return mSessionTimer.getTotalElapsed();
  }

  private void updateDuration(long elapsedMilliseconds) {
    Log.i(TAG, "Updating duration: " + elapsedMilliseconds);
    int elapsedMinutes = (int) (elapsedMilliseconds / (1000 * 60));
    int currentItem = mWheelDuration.getCurrentItem();
    if(elapsedMinutes != currentItem) {
      mWheelDuration.setCurrentItem(elapsedMinutes, false, false);
    }
  }

  private void startTrackerUpdates() {
    stopTrackerUpdates();

    PauseableSpinAnimation spinAnimation = (PauseableSpinAnimation) mTimeSpinner.getAnimation();
    if(spinAnimation != null) {
      spinAnimation.resume();
    }

    mWheelDuration.setEnabled(true);
    mWheelDuration.setVisibility(View.VISIBLE);

    mUpdateDurationTask = new UpdateDurationTask();

    final float minutes = ((getElapsed() / 1000.0f) / 60.0f);
    int millisecondsToNextFullMinute = (int) ((1.0f - (minutes - (int) minutes)) * 60000);

    // Add a little padding to avoid rounding errors which can cause the update
    // to miss the minute change and have to wait a whole minute for the next update.
    millisecondsToNextFullMinute = Math.min(60000, millisecondsToNextFullMinute + 100);
    mUpdateDurationTask.execute(millisecondsToNextFullMinute);
  }

  private void stopTrackerUpdates() {
    PauseableSpinAnimation spinAnimation = (PauseableSpinAnimation) mTimeSpinner.getAnimation();
    if(spinAnimation != null) {
      spinAnimation.pause();
    }

    // Clear out the redraw timer
    if(mUpdateDurationTask != null) {
      mUpdateDurationTask.cancel(true);
      mUpdateDurationTask = null;
    }

    mWheelDuration.setEnabled(false);
  }

  private class UpdateDurationTask extends AsyncTask<Integer, Void, Void> {
    private static final int UPDATE_INTERVAL = 60 * 1000;

    // TODO display a notification while reading is active

    @Override
    protected Void doInBackground(Integer... initialDelay) {
      int delay = initialDelay[0];
      try {
        while(!isCancelled()) {
          publishProgress();
          Log.d(TAG, "Next update in: " + delay + " milliseconds");
          Thread.sleep(delay);
          delay = UPDATE_INTERVAL;
        }
        return null;
      } catch(InterruptedException ignored) {
        return null;
      }
    }

    @Override
    protected void onProgressUpdate(Void... voids) {
      Log.v(TAG, "Updated progress");
      updateDuration(getElapsed());
    }
  }
}
