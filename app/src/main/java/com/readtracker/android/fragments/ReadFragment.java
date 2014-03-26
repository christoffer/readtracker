package com.readtracker.android.fragments;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import com.readtracker.android.activities.FinishBookActivity;
import com.readtracker.android.custom_views.PauseableSpinAnimation;
import com.readtracker.android.custom_views.TimeSpinner;
import com.readtracker.android.db.Book;
import com.readtracker.android.support.DrawableGenerator;
import com.readtracker.android.support.SessionTimer;
import com.readtracker.android.support.SimpleAnimationListener;
import com.readtracker.android.support.Utils;
import com.readtracker.android.thirdparty.SafeViewFlipper;
import com.readtracker.android.thirdparty.widget.OnWheelChangedListener;
import com.readtracker.android.thirdparty.widget.WheelView;
import com.readtracker.android.thirdparty.widget.adapters.ArrayWheelAdapter;
import com.squareup.otto.Subscribe;

import java.lang.ref.WeakReference;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Fragment for managing a reading session
 */
public class ReadFragment extends BaseFragment {
  private static final String TAG = ReadFragment.class.getSimpleName();

  private static final String END_SESSION_FRAGMENT_TAG = "end-session-tag";

  private View mRootView;

  // Session controls
  private Button mStartButton;
  private Button mPauseButton;
  private Button mDoneButton;

  // Time tracking
  private TextView mLastPositionText;
  private TimeSpinner mTimeSpinner;

  // Wrap the spinner to apply the pulse animation on pause without
  // disrupting the time spinner animation
  private ViewGroup mTimeSpinnerWrapper;

  // Flipper for showing start vs. stop/done
  private SafeViewFlipper mTimeSpinnerControlsFlipper;

  // Book to track
  private Book mBook;

  private final SessionTimer mSessionTimer = new SessionTimer();

  private final UpdateDurationWheelTimer mUpdateWheelViewTimer = new UpdateDurationWheelTimer(this);

  private WheelView mDurationWheelView;

  private boolean mIsStarted = false;

  // Display child index for flipper session control
  private static final int FLIPPER_PAGE_START_BUTTON = 0;
  private static final int FLIPPER_PAGE_READING_BUTTONS = 1;

  // Actions to be taken as the timer starts and stops
  private SessionTimer.SessionTimerListener mSessionTimerListener = new SessionTimer.SessionTimerListener() {
    @Override
    public void onSessionTimerStarted() {
      mUpdateWheelViewTimer.reschedule();
      startTimeSpinner();
      displayRunningModeControls();
    }

    @Override
    public void onSessionTimerStopped() {
      mUpdateWheelViewTimer.stop();
      pauseTimeSpinner();
      displayPauseModeControls();
    }
  };

  public ReadFragment() {
    mSessionTimer.setOnTimerListener(mSessionTimerListener);
  }

  public static Fragment newInstance() {
    Log.v(TAG, "Creating new instance of ReadFragment");
    return new ReadFragment();
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    Log.d(TAG, "onCreateView()");
    View view = inflater.inflate(R.layout.read_fragment, container, false);

    bindViews(view);

    mTimeSpinnerControlsFlipper.setDisplayedChild(FLIPPER_PAGE_START_BUTTON);

    populateFieldsDeferred();

    if(mSessionTimer.isStarted()) {
      updateWheelWithElapsedTime();
      mSessionTimer.togglePausePlay();
    } else {
      displayStartModeControls();
    }

    return view;
  }

  @Override public void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if(requestCode == BookActivity.REQUEST_FINISH_BOOK && resultCode == Activity.RESULT_OK) {
      String closingRemark = data.getStringExtra(FinishBookActivity.KEY_CLOSING_REMARK);
      Log.d(TAG, String.format("Finishing %s with closing remark: %s", mBook.getTitle(), closingRemark));
      getBus().post(new BookFinishedEvent(closingRemark));
    }
  }

  @Override public void onStart() {
    super.onStart();
    if(!mSessionTimer.loadFromPreferences(getPreferences())) {
      Log.v(TAG, "No stored sessions found, resetting timer");
      mSessionTimer.reset();
    } else {
      Log.v(TAG, "Using reset timer state: " + mSessionTimer);
    }
  }

  @Override public void onStop() {
    super.onStop();
    if(getActivity().isFinishing()) {
      mSessionTimer.clearFromPreferences(getPreferences());
    } else {
      mSessionTimer.saveToPreferences(getPreferences());
    }
  }

  @Subscribe public void onBookLoadedEvent(BookActivity.BookLoadedEvent event) {
    Log.v(TAG, "Got book loaded event: " + event);
    mBook = event.getBook();
    populateFieldsDeferred();
  }

  private void populateFieldsDeferred() {
    if(mBook == null || mRootView == null) {
      return;
    }

    Log.v(TAG, "Populating fields for book: " + mBook);

    final int bookColor = Utils.calculateBookColor(mBook);

    mTimeSpinner.setColor(bookColor);
    mTimeSpinner.setMaxSize(500);

    mStartButton.setBackgroundDrawable(DrawableGenerator.generateButtonBackground(bookColor));
    mPauseButton.setBackgroundDrawable(DrawableGenerator.generateButtonBackground(bookColor));
    mDoneButton.setBackgroundDrawable(DrawableGenerator.generateButtonBackground(bookColor));

    initializeDurationWheel();

    mLastPositionText.setText(getLastPositionDescription());

    bindEvents();
  }

  private void bindViews(View view) {
    mDoneButton = (Button) view.findViewById(R.id.done_button);
    mStartButton = (Button) view.findViewById(R.id.start_button);
    mPauseButton = (Button) view.findViewById(R.id.pause_button);

    mTimeSpinnerControlsFlipper = (SafeViewFlipper) view.findViewById(R.id.time_spinner_controls_flipper);

    mLastPositionText = (TextView) view.findViewById(R.id.last_position_text);
    mTimeSpinner = (TimeSpinner) view.findViewById(R.id.timespinner);

    mDurationWheelView = (WheelView) view.findViewById(R.id.duration_wheel_view);

    mTimeSpinnerWrapper = (ViewGroup) view.findViewById(R.id.time_spinner_wrapper);

    mRootView = view;
  }

  private void bindEvents() {
    mPauseButton.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View view) {
        mSessionTimer.togglePausePlay();
      }
    });

    mStartButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        startTimer();
      }
    });

    mDoneButton.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View view) {
        endSession();
      }
    });


    ViewTreeObserver viewTreeObserver = mTimeSpinner.getViewTreeObserver();
    if(viewTreeObserver != null && viewTreeObserver.isAlive()) {
      viewTreeObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
        @Override public void onGlobalLayout() {
          mTimeSpinner.getViewTreeObserver().removeGlobalOnLayoutListener(this);
          prepareTimeSpinnerAnimation();
        }
      });
    }

    mTimeSpinner.setOnTouchListener(new View.OnTouchListener() {
      @Override
      public boolean onTouch(View view, MotionEvent motionEvent) {
        // The start wheel is not active as a click target when the timing is started
        // this is due to the inability to have both the TimeSpinner and the underlying
        // wheel view receive touch events prior to android 11.
        if(mDurationWheelView == null || mDurationWheelView.isEnabled()) {
          return false;
        }

        int action = motionEvent.getAction();
        if(action == MotionEvent.ACTION_DOWN) {
          mTimeSpinner.setHighlighted(true);
          return true;
        } else if(action == MotionEvent.ACTION_UP) {
          if(mIsStarted) {
            mSessionTimer.togglePausePlay();
          } else {
            startTimer();
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

  private void prepareTimeSpinnerAnimation() {
    if(mTimeSpinner.getAnimation() == null) {
      final float offsetX = mTimeSpinner.getWidth() * 0.5f;
      final float offsetY = mTimeSpinner.getHeight() * 0.5f;

      PauseableSpinAnimation spinAnimation = new PauseableSpinAnimation(0, 360, offsetX, offsetY);
      spinAnimation.setRepeatMode(Animation.RESTART);
      spinAnimation.setRepeatCount(Animation.INFINITE);
      spinAnimation.setDuration(60 * 1000);
      spinAnimation.setInterpolator(new LinearInterpolator());
      spinAnimation.setFillAfter(true);

      mTimeSpinner.setAnimation(spinAnimation);
      spinAnimation.pause();
    }
  }

  /**
   * Flips the button view flipper to the given page.
   * Does not change if the given page is already active (to avoid re-activating animations).
   */
  private void flipToButtonPage(int page) {
    if(mTimeSpinnerControlsFlipper.getDisplayedChild() != page) {
      mTimeSpinnerControlsFlipper.setDisplayedChild(page);
    }
  }

  /**
   * Initializes the wheel view for displaying the reading session duration
   */
  private void initializeDurationWheel() {
    ArrayWheelAdapter hoursAdapter = createDurationWheelAdapter(24 * 60);
    mDurationWheelView.setVisibleItems(3);
    mDurationWheelView.setViewAdapter(hoursAdapter);
    mDurationWheelView.setCalliperMode(WheelView.CalliperMode.NO_CALLIPERS);

    // Have the wheel duration initially invisible, and show it once timing starts
    mDurationWheelView.setVisibility(View.INVISIBLE);
    mDurationWheelView.setEnabled(false);

    mDurationWheelView.addChangingListener(new OnWheelChangedListener() {
      @Override
      public void onChanged(WheelView wheel, int oldValue, int newValue) {
        final long newElapsedMs = newValue * 60 * 1000;
        final boolean wasRunning = mSessionTimer.isRunning();
        mSessionTimer.reset(newElapsedMs);
        if(wasRunning) {
          mSessionTimer.start();
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

  private String getLastPositionDescription() {
    if(!mBook.hasCurrentPosition()) {
      return getString(R.string.reading_click_to_start);
    } else if(mBook.hasPageNumbers()) {
      return getString(R.string.reading_last_on_page, mBook.getCurrentPageName());
    } else {
      return getString(R.string.reading_last_at, mBook.getCurrentPageName());
    }
  }

  /**
   * Called when the start button is clicked
   */
  private void startTimer() {
    final Animation disappear = AnimationUtils.loadAnimation(getActivity(), R.anim.fade_out);
    final Animation appear = AnimationUtils.loadAnimation(getActivity(), R.anim.slide_up_appear);

    //noinspection ConstantConditions
    disappear.setAnimationListener(new SimpleAnimationListener() {
      @Override public void onAnimationEnd(Animation animation) {
        mSessionTimer.start();
        updateWheelWithElapsedTime();
        mLastPositionText.setVisibility(View.INVISIBLE);
        mDurationWheelView.startAnimation(appear);
        mDurationWheelView.setVisibility(View.VISIBLE);
      }
    });

    mTimeSpinnerControlsFlipper.setDisplayedChild(FLIPPER_PAGE_READING_BUTTONS);
    mLastPositionText.startAnimation(disappear);

    mIsStarted = true;
  }

  private void endSession() {
    mSessionTimer.stop();

    EndSessionDialog dialog = EndSessionDialog.newInstance(mBook);

    // Response will be relayed to the activity, requiring it to implement EndSessionDialogListener
    FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
    dialog.show(fragmentManager, END_SESSION_FRAGMENT_TAG);
  }

  private void displayStartModeControls() {
    Log.v(TAG, "Setting timer controls to start mode");
    mStartButton.setText(R.string.reading_start);
    flipToButtonPage(FLIPPER_PAGE_START_BUTTON);
  }

  private void displayPauseModeControls() {
    Log.v(TAG, "Setting timer controls to pause mode");
    mPauseButton.setText(R.string.reading_resume);
    mDurationWheelView.setEnabled(false);
    flipToButtonPage(FLIPPER_PAGE_READING_BUTTONS);
    final Animation pulse = AnimationUtils.loadAnimation(getActivity(), R.anim.pulse);
    mTimeSpinnerWrapper.startAnimation(pulse);
  }

  private void displayRunningModeControls() {
    Log.v(TAG, "Setting timer controls to running mode");
    mPauseButton.setText(R.string.reading_pause);
    flipToButtonPage(FLIPPER_PAGE_READING_BUTTONS);
    mTimeSpinnerWrapper.clearAnimation();
  }

  private void updateWheelWithElapsedTime() {
    final long elapsedMilliseconds = mSessionTimer.getElapsedMs();
    Log.i(TAG, "Updating duration: " + elapsedMilliseconds);
    int elapsedMinutes = (int) (elapsedMilliseconds / (1000 * 60));
    int currentItem = mDurationWheelView.getCurrentItem();
    if(elapsedMinutes != currentItem) {
      mDurationWheelView.setCurrentItem(elapsedMinutes, false, false);
    }
  }

  private void startTimeSpinner() {
    PauseableSpinAnimation spinAnimation = (PauseableSpinAnimation) mTimeSpinner.getAnimation();
    if(spinAnimation != null) {
      spinAnimation.resume();
    }

    mDurationWheelView.setEnabled(true);
    mDurationWheelView.setVisibility(View.VISIBLE);
  }

  private void pauseTimeSpinner() {
    PauseableSpinAnimation spinAnimation = (PauseableSpinAnimation) mTimeSpinner.getAnimation();
    if(spinAnimation != null) {
      spinAnimation.pause();
    }

    mDurationWheelView.setEnabled(false);
  }

  private SharedPreferences getPreferences() {
    return ((BaseActivity) getActivity()).getPreferences();
  }

  /**
   * Signals that the current book was finished.
   */
  public static class BookFinishedEvent {
    final private String closingRemark;

    public BookFinishedEvent(String closingRemark) {
      this.closingRemark = closingRemark;
    }

    public String getClosingRemark() {
      return closingRemark;
    }
  }

  /** Helper class for updating the duration wheel at suitable times. */
  private static class UpdateDurationWheelTimer extends Timer {
    private static final int MS_PER_MINUTE = 1000 * 60;
    private final WeakReference<ReadFragment> mReadFragmentRef;
    TimerTask mTask;

    // Make sure the callback happens on the main thread
    Handler mHandler = new Handler(Looper.getMainLooper());
    Runnable mUpdateRunnable = new Runnable() {
      @Override public void run() {
        onUpdate();
      }
    };

    public UpdateDurationWheelTimer(ReadFragment readFragment) {
      mReadFragmentRef = new WeakReference<ReadFragment>(readFragment);
    }

    public void reschedule() {
      if(mTask != null) mTask.cancel();

      ReadFragment fragment = mReadFragmentRef.get();
      if(fragment == null) {
        stop();
        return;
      }

      final long elapsedMs = fragment.mSessionTimer.getElapsedMs();
      final long millisUntilNextMinute = 1000 - (elapsedMs - (elapsedMs / MS_PER_MINUTE) * MS_PER_MINUTE);
      mTask = new TimerTask() {
        @Override public void run() {
          mHandler.post(mUpdateRunnable);
        }
      };
      scheduleAtFixedRate(mTask, millisUntilNextMinute, MS_PER_MINUTE);
    }

    // Cancels the running task (if available) and purges the queue.
    public void stop() {
      if(mTask != null) mTask.cancel();
      purge();
    }

    private void onUpdate() {
      ReadFragment fragment = mReadFragmentRef.get();
      if(fragment != null) {
        fragment.updateWheelWithElapsedTime();
      }
    }
  }
}
