package com.readtracker.android.fragments;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.fragment.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.NumberPicker;
import android.widget.TextView;

import com.readtracker.BuildConfig;
import com.readtracker.R;
import com.readtracker.android.activities.BaseActivity;
import com.readtracker.android.activities.BookActivity;
import com.readtracker.android.custom_views.PauseableSpinAnimation;
import com.readtracker.android.custom_views.TimeSpinner;
import com.readtracker.android.db.Book;
import com.readtracker.android.support.CircleImageTransformation;
import com.readtracker.android.support.ColorUtils;
import com.readtracker.android.support.SessionTimer;
import com.readtracker.android.support.SimpleAnimationListener;
import com.readtracker.android.thirdparty.SafeViewFlipper;
import com.readtracker.databinding.ReadFragmentBinding;
import com.squareup.otto.Subscribe;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import org.jetbrains.annotations.NotNull;

import java.lang.ref.WeakReference;
import java.util.Timer;
import java.util.TimerTask;

import static com.readtracker.android.support.StringUtils.hoursAndMinutesFromMillis;

/** Displays a timer and controls for the user to start, pause and stop a reading session. */
public class ReadFragment extends BaseFragment {
  private static final String TAG = ReadFragment.class.getSimpleName();

  /** Views */
  private Button mStartButton;
  private Button mEditButton;
  private Button mPauseButton;
  private Button mDoneButton;
  private TextView mLastPositionText;
  private TimeSpinner mTimeSpinner;
  private ViewGroup mTimeSpinnerWrapper;
  private SafeViewFlipper mTimeSpinnerControlsFlipper;
  private ImageView coverImage;
  private NumberPicker mDurationPicker;

  // Book to track
  private Book mBook;

  private final SessionTimer mSessionTimer = new SessionTimer();

  private final UpdateDurationWheelTimer mUpdateWheelViewTimer = new UpdateDurationWheelTimer(this);


  // Display child index for flipper session control
  private static final int FLIPPER_PAGE_START_BUTTON = 0;
  private static final int FLIPPER_PAGE_READING_BUTTONS = 1;

  private ReadFragmentBinding binding;

  public ReadFragment() {
    mSessionTimer.setOnTimerListener(new SessionTimer.SessionTimerListener() {
      @Override
      public void onSessionTimerStarted() {
        mUpdateWheelViewTimer.reschedule();
        refreshTimerUI();
      }

      @Override
      public void onSessionTimerStopped() {
        mUpdateWheelViewTimer.stop();
        refreshTimerUI();
      }
    });
  }

  public static Fragment newInstance() {
    Log.v(TAG, "Creating new instance of ReadFragment");
    return new ReadFragment();
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    Log.d(TAG, "onCreateView()");
    binding = ReadFragmentBinding.inflate(inflater, container, false);

    mStartButton = binding.startButton;
    mEditButton = binding.editButton;
    mPauseButton = binding.pauseButton;
    mDoneButton = binding.doneButton;
    mLastPositionText = binding.lastPositionText;
    mTimeSpinner = binding.timespinner;
    mTimeSpinnerWrapper = binding.timeSpinnerWrapper;
    mTimeSpinnerControlsFlipper = binding.timeSpinnerControlsFlipper;
    coverImage = binding.coverImage;
    mDurationPicker = binding.durationPicker;

    initializeTimerUI();
    mTimeSpinnerControlsFlipper.setDisplayedChild(FLIPPER_PAGE_START_BUTTON);

    return binding.getRoot();
  }

  @Override public void onDestroyView() {
    super.onDestroyView();
    binding = null;
  }

  @Override public void onViewCreated(@NotNull View view, Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    populateFieldsDeferred();
  }

  @Override public void onStart() {
    super.onStart();
    final SharedPreferences prefs = getPreferences();
    if(prefs != null) {
      mSessionTimer.initializeFromPreferences(getPreferences());
    } else {
      Log.d(TAG, "Started ReadFragment without access to preferences");
    }

    refreshDurationWheel();
  }

  @Override public void onStop() {
    super.onStop();

    mUpdateWheelViewTimer.stop();

    final Activity attachedActivity = getActivity();
    final SharedPreferences prefs = getPreferences();
    if(attachedActivity == null || prefs == null) {
      Log.d(TAG, "onStop() with detached activity and/or without access to preferences");
      return;
    }

    if(attachedActivity.isFinishing()) {
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

  private void refreshTimerUI() {
    final PauseableSpinAnimation timerAnimation = (PauseableSpinAnimation) mTimeSpinner.getAnimation();

    if(timerAnimation == null) {
      // Animation isn't ready yet. Exit and rely on the animation callback to call us again.
      Log.d(TAG, "Animation not ready yet, bailing out.");
      return;
    }

    Log.d(TAG, String.format("Showing UI for timer: %s", mSessionTimer));
    if(mSessionTimer.isStarted()) {
      displayDurationWheel(false);

      if(mSessionTimer.isRunning()) {
        timerAnimation.resume();
        displayControlsForRunningTimer();
      } else {
        timerAnimation.pause();
        displayControlsForPausedTimer();
      }
    } else {
      mDurationPicker.setVisibility(View.INVISIBLE);
      timerAnimation.pause();
      timerAnimation.reset();
      displayControlsForUnstartedTimer();
    }
  }

  /** Populates the fields of the UI as soon as both the UI and the book are available. */
  private void populateFieldsDeferred() {
    if(mBook == null || getView() == null) {
      return;
    }

    final Context context = getContext();
    if(context == null) {
      return; // detached?
    }

    Log.v(TAG, "Populating fields for book: " + mBook);
    final int bookColor = ColorUtils.getColorForBook(mBook);
    mTimeSpinner.setColor(bookColor);

    Picasso.with(context)
        .load(mBook.getCoverImageUrl())
        .transform(new CircleImageTransformation())
        .into(coverImage, new Callback() {
          @Override public void onSuccess() {}

          @Override public void onError() {
            mEditButton.setText(R.string.refresh_cover);
            mEditButton.setVisibility(View.VISIBLE);
          }
        });
    ColorUtils.setNumberPickerDividerColorUsingHack(mDurationPicker, 0x00000000);
    mLastPositionText.setText(getLastPositionDescription());

    // Don't bother users that has already customized the book with a large button in the UI.
    final boolean hasPersonalization = mBook.getCoverImageUrl() != null;
    Log.d(TAG, String.format("Book cover url: %s", mBook.getCoverImageUrl()));
    mEditButton.setText(R.string.personalize);
    mEditButton.setVisibility(hasPersonalization ? View.GONE : View.VISIBLE);

    bindEvents();
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
        transitionFromStartModeToRunningMode();
      }
    });

    mEditButton.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View view) {
        getBus().post(new BookActivity.BookEditRequestedEvent());
      }
    });

    mDoneButton.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View view) {
        refreshNumberPickerIfEditing();
        endSession();
      }
    });

    ViewTreeObserver viewTreeObserver = mTimeSpinner.getViewTreeObserver();
    if(viewTreeObserver != null && viewTreeObserver.isAlive()) {
      viewTreeObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
        @Override public void onGlobalLayout() {
          //noinspection deprecation
          mTimeSpinner.getViewTreeObserver().removeGlobalOnLayoutListener(this);
          initializeTimerAnimation();
          refreshTimerUI();
        }
      });
    }

    // NOTE(christoffer) Casting to View to get Android linter to realize we're calling
    // view.performClick() below.
    //noinspection RedundantCast
    ((View) mTimeSpinner).setOnTouchListener(new View.OnTouchListener() {
      @Override
      public boolean onTouch(View view, MotionEvent motionEvent) {
        if(mDurationPicker.getVisibility() == View.VISIBLE) {
          return false;
        }

        switch(motionEvent.getActionMasked()) {
          case MotionEvent.ACTION_DOWN:
            mTimeSpinner.setHighlighted(true);
            return true;
          case MotionEvent.ACTION_UP:
            view.performClick();
            if(mSessionTimer.isStarted()) {
              mSessionTimer.togglePausePlay();

            } else {
              transitionFromStartModeToRunningMode();
            }
            mTimeSpinner.setHighlighted(false);
            return true;
          case MotionEvent.ACTION_CANCEL:
            mTimeSpinner.setHighlighted(false);
            return true;
        }
        return false;
      }
    });
  }

  private void initializeTimerAnimation() {
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

  /**
   * Flips the button view flipper to the given page.
   * Does not change if the given page is already active (to avoid re-activating animations).
   */
  private void flipToButtonPage(int page) {
    if(mTimeSpinnerControlsFlipper.getDisplayedChild() != page) {
      mTimeSpinnerControlsFlipper.setDisplayedChild(page);
    }
  }

  /** Initialize duration wheel and timer. */
  private void initializeTimerUI() {
    initializeDurationWheel();
  }

  private void initializeDurationWheel() {
    final Context context = getContext();
    if(context == null) {
      return;
    }

    final int maxHours = 24;
    final int numMinutesInWheel = maxHours * 60;
    String[] wheelLabels = new String[numMinutesInWheel];
    for(int minute = 0; minute < numMinutesInWheel; minute++) {
      wheelLabels[minute] = hoursAndMinutesFromMillis(minute * 60 * 1000, context);
    }

    mDurationPicker.setMaxValue(numMinutesInWheel - 1);
    mDurationPicker.setDisplayedValues(wheelLabels);

    // Don't show the duration wheel until the timer has started
    mDurationPicker.setVisibility(View.INVISIBLE);
    mDurationPicker.setWrapSelectorWheel(false);

    mDurationPicker.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
      @Override public void onValueChange(NumberPicker picker, int oldValue, int newValue) {
        final long newElapsedMs = newValue * 60 * 1000;
        mSessionTimer.reset(newElapsedMs);
      }
    });
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

  /** Starts the timer with a UI transition into a running mode. */
  private void transitionFromStartModeToRunningMode() {
    final Context context = getContext();
    if(context == null) {
      return; // detached
    }

    final Animation disappear = AnimationUtils.loadAnimation(context, R.anim.fade_out);

    disappear.setAnimationListener(new SimpleAnimationListener() {
      @Override public void onAnimationEnd(Animation animation) {
        mSessionTimer.start();
        refreshDurationWheel();
        displayDurationWheel(true);
      }
    });

    mTimeSpinnerControlsFlipper.setDisplayedChild(FLIPPER_PAGE_READING_BUTTONS);
    mLastPositionText.startAnimation(disappear);
  }

  private void endSession() {
    mSessionTimer.stop();
    // Report back to whoever is listening that we're done with this session
    getBus().post(new SessionDoneEvent(mSessionTimer.getElapsedMs()));
  }

  private void refreshNumberPickerIfEditing() {
    if(mDurationPicker.hasFocus()) {
      // Explicitly clear the focus to trigger an update if the user hasn't closed
      // the keyboard before clicking the done button.
      mDurationPicker.clearFocus();
    }
  }

  private void displayDurationWheel(boolean animated) {
    if(animated) {
      final Animation appear = AnimationUtils.loadAnimation(getActivity(), R.anim.slide_up_appear);
      mDurationPicker.startAnimation(appear);
    }

    mLastPositionText.setVisibility(View.INVISIBLE);
    mDurationPicker.setVisibility(View.VISIBLE);
  }

  private void displayControlsForUnstartedTimer() {
    mStartButton.setText(R.string.reading_start);
    flipToButtonPage(FLIPPER_PAGE_START_BUTTON);
  }

  private void displayControlsForPausedTimer() {
    mPauseButton.setText(R.string.reading_resume);
    flipToButtonPage(FLIPPER_PAGE_READING_BUTTONS);
    final Animation pulse = AnimationUtils.loadAnimation(getActivity(), R.anim.pulse);
    mTimeSpinnerWrapper.startAnimation(pulse);
  }

  private void displayControlsForRunningTimer() {
    mPauseButton.setText(R.string.reading_pause);
    flipToButtonPage(FLIPPER_PAGE_READING_BUTTONS);
    mTimeSpinnerWrapper.clearAnimation();
  }

  private void refreshDurationWheel() {
    final long elapsedMilliseconds = mSessionTimer.getElapsedMs();
    Log.v(TAG, "Updating duration: " + elapsedMilliseconds);
    int elapsedMinutes = (int) (elapsedMilliseconds / (1000 * 60));
    int currentItem = mDurationPicker.getValue();
    if(elapsedMinutes != currentItem) {
      mDurationPicker.setValue(elapsedMinutes);
    }
  }

  private SharedPreferences getPreferences() {
    final BaseActivity activity = (BaseActivity) getActivity();
    if(activity != null) {
      return activity.getPreferences();
    }
    return null;
  }

  /** Updates the duration wheel at suitable times. */
  private static class UpdateDurationWheelTimer extends Timer {
    private static final int UPDATE_INTERVAL_MS = BuildConfig.DEBUG ? (1000) : (1000 * 60);
    private final WeakReference<ReadFragment> mReadFragmentRef;
    TimerTask mTask;

    // Make sure the callback happens on the main thread
    final Handler mHandler = new Handler(Looper.getMainLooper());
    final Runnable mUpdateRunnable = new Runnable() {
      @Override public void run() {
        onUpdate();
      }
    };

    UpdateDurationWheelTimer(ReadFragment readFragment) {
      mReadFragmentRef = new WeakReference<>(readFragment);
    }

    public void reschedule() {
      if(mTask != null) mTask.cancel();

      ReadFragment fragment = mReadFragmentRef.get();
      if(fragment == null) {
        stop();
        return;
      }

      final long elapsedMs = fragment.mSessionTimer.getElapsedMs();
      final long millisUntilNextMinute = 1000 - (elapsedMs - (elapsedMs / UPDATE_INTERVAL_MS) * UPDATE_INTERVAL_MS);
      mTask = new TimerTask() {
        @Override public void run() {
          mHandler.post(mUpdateRunnable);
        }
      };
      scheduleAtFixedRate(mTask, Math.max(0, millisUntilNextMinute), UPDATE_INTERVAL_MS);
    }

    // Cancels the running task (if available) and purges the queue.
    public void stop() {
      if(mTask != null) mTask.cancel();
      purge();
    }

    private void onUpdate() {
      ReadFragment fragment = mReadFragmentRef.get();
      if(fragment != null) {
        fragment.refreshDurationWheel();
      }
    }
  }

  /** Event emitted when the user is done with a reading session. */
  public static class SessionDoneEvent {
    private final long mDurationMillis;

    SessionDoneEvent(long durationMillis) {
      mDurationMillis = durationMillis;
    }

    public long getDurationMillis() {
      return mDurationMillis;
    }
  }
}
