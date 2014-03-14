package com.readtracker.android.fragments;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
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
import com.readtracker.android.activities.BookActivity;
import com.readtracker.android.activities.EndSessionDialog;
import com.readtracker.android.activities.FinishBookActivity;
import com.readtracker.android.custom_views.PauseableSpinAnimation;
import com.readtracker.android.custom_views.TimeSpinner;
import com.readtracker.android.db.Book;
import com.readtracker.android.support.DrawableGenerator;
import com.readtracker.android.support.SessionTimer;
import com.readtracker.android.support.Utils;
import com.readtracker.android.thirdparty.SafeViewFlipper;
import com.readtracker.android.thirdparty.widget.OnWheelChangedListener;
import com.readtracker.android.thirdparty.widget.WheelView;
import com.readtracker.android.thirdparty.widget.adapters.ArrayWheelAdapter;
import com.squareup.otto.Subscribe;

/**
 * Fragment for managing a reading session
 */
public class ReadFragment extends BaseFragment {
  private static final String TAG = ReadFragment.class.getSimpleName();

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

  // Book to track
  private Book mBook;

  private SessionTimer mSessionTimer;

  private WheelView mElapsedTimeWheelView;

  private boolean mIsStarted = false;

  // Display child index for flipper session control
  private static final int FLIPPER_PAGE_START_BUTTON = 0;
  private static final int FLIPPER_PAGE_READING_BUTTONS = 1;

  public static Fragment newInstance() {
    Log.v(TAG, "Creating new instance of ReadFragment");
    return new ReadFragment();
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    Log.d(TAG, "onCreateView()");
    View view = inflater.inflate(R.layout.fragment_read, container, false);

    bindViews(view);

    mFlipperSessionControl.setDisplayedChild(FLIPPER_PAGE_START_BUTTON);

    populateFieldsDeferred();
    populateTimerDeferred();

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

  @Subscribe public void onBookLoadedEvent(BookActivity.BookLoadedEvent event) {
    Log.v(TAG, "Got book loaded event: " + event);
    mBook = event.getBook();
    populateFieldsDeferred();
  }

  @Subscribe public void onSessionTimerChangedEvent(BookActivity.SessionTimerChangedEvent event) {
    if(event.getSessionTimer() != null) {
      // Flag for assigning timer for the first time
      final boolean initMode = mSessionTimer == null && event.getSessionTimer() != null;
      mSessionTimer = event.getSessionTimer();

      if(mSessionTimer != null) {
        if(mSessionTimer.isRunning()) {
          startTimeSpinner();
          if(!initMode) displayPausableControls();
        } else {
          pauseTimeSpinner();
          if(!initMode) displayResumableControls();
        }
      }

      refreshElapsedTime();
      populateTimerDeferred();
    } else {
      Log.w(TAG, "Session timer unexpectedly reset to null");
    }
  }

  private void populateFieldsDeferred() {
    if(mBook == null || mRootView == null) {
      return;
    }

    Log.v(TAG, "Populating fields for book: " + mBook);

    final int bookColor = Utils.calculateBookColor(mBook);

    mTimeSpinner.setColor(bookColor);
    mTimeSpinner.setMaxSize(500);

    mButtonStart.setBackgroundDrawable(DrawableGenerator.generateButtonBackground(bookColor));
    mButtonPause.setBackgroundDrawable(DrawableGenerator.generateButtonBackground(bookColor));
    mButtonDone.setBackgroundDrawable(DrawableGenerator.generateButtonBackground(bookColor));

    initializeDurationWheel();

    bindEvents();
  }

  private void populateTimerDeferred() {
    if(mSessionTimer == null || mRootView == null) {
      return;
    }

    // Starting or continuing a reading session?
    final long elapsedMs = mSessionTimer.getElapsedMs();

    if(elapsedMs == 0) {
      describeLastPosition();
      setupStartMode();
    } else {
      refreshElapsedTime();
      displayResumableControls();
    }
  }


  private void bindViews(View view) {
    mButtonDone = (Button) view.findViewById(R.id.buttonDone);
    mButtonStart = (Button) view.findViewById(R.id.buttonStart);
    mButtonPause = (Button) view.findViewById(R.id.buttonPause);

    mFlipperSessionControl = (SafeViewFlipper) view.findViewById(R.id.flipperSessionControl);

    mTextBillboard = (TextView) view.findViewById(R.id.textBillboard);
    mTimeSpinner = (TimeSpinner) view.findViewById(R.id.timespinner);

    mElapsedTimeWheelView = (WheelView) view.findViewById(R.id.wheelDuration);

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
      @Override
      public void onClick(View view) {
        onClickedStart();
      }
    });

    mButtonDone.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View view) {
        onClickedDone();
      }
    });


    ViewTreeObserver viewTreeObserver = mTimeSpinner.getViewTreeObserver();
    if(viewTreeObserver != null && viewTreeObserver.isAlive()) {
      viewTreeObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
        @Override public void onGlobalLayout() {
          mTimeSpinner.getViewTreeObserver().removeGlobalOnLayoutListener(this);
          setTimeSpinnerAnimation();
        }
      });
    }

    mTimeSpinner.setOnTouchListener(new View.OnTouchListener() {
      @Override
      public boolean onTouch(View view, MotionEvent motionEvent) {
        // The start wheel is not active as a click target when the timing is started
        // this is due to the inability to have both the TimeSpinner and the underlying
        // wheel view receive touch events prior to android 11.
        if(mElapsedTimeWheelView == null || mElapsedTimeWheelView.isEnabled()) {
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

  private void setTimeSpinnerAnimation() {
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
    }
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
    mElapsedTimeWheelView.setVisibleItems(3);
    mElapsedTimeWheelView.setViewAdapter(hoursAdapter);
    mElapsedTimeWheelView.setCalliperMode(WheelView.CalliperMode.NO_CALLIPERS);

    // Have the wheel duration initially invisible, and show it once timing starts
    mElapsedTimeWheelView.setVisibility(View.INVISIBLE);
    mElapsedTimeWheelView.setEnabled(false);

    mElapsedTimeWheelView.addChangingListener(new OnWheelChangedListener() {
      @Override
      public void onChanged(WheelView wheel, int oldValue, int newValue) {
        if(mSessionTimer != null) {
          final long newElapsedMs = newValue * 60 * 1000;
          mSessionTimer.reset(newElapsedMs);
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
   * Called when the start button is clicked
   */
  private void onClickedStart() {
    final Animation disappear = AnimationUtils.loadAnimation(getActivity(), R.anim.fade_out);
    final Animation appear = AnimationUtils.loadAnimation(getActivity(), R.anim.slide_up_appear);

    if(mSessionTimer == null) {
      Log.w(TAG, "Received click on start while session timer is unassigned.");
      return;
    }

    //noinspection ConstantConditions
    disappear.setAnimationListener(new Animation.AnimationListener() {
      @Override
      public void onAnimationStart(Animation animation) {
      }

      @Override
      public void onAnimationRepeat(Animation animation) {
      }

      @Override
      public void onAnimationEnd(Animation animation) {
        mSessionTimer.start();
        refreshElapsedTime();
        mTextBillboard.setVisibility(View.INVISIBLE);
        mElapsedTimeWheelView.startAnimation(appear);
        mElapsedTimeWheelView.setVisibility(View.VISIBLE);
      }
    });

    mFlipperSessionControl.setDisplayedChild(FLIPPER_PAGE_READING_BUTTONS);
    mTextBillboard.startAnimation(disappear);
    mIsStarted = true;
  }

  private void onClickedPauseResume() {
    if(mSessionTimer != null) {
      mSessionTimer.stop();
    } else {
      Log.w(TAG, "Received click on pause button while session timer is unassigned.");
    }
  }

  private void onClickedDone() {
    if(mSessionTimer != null) {
      mSessionTimer.stop();
    } else {
      Log.w(TAG, "Stopped clicked while the session timer was unassigned");
    }

    EndSessionDialog dialog = EndSessionDialog.newInstance(mBook);

    // Response will be relayed to the activity, requiring it to implement EndSessionDialogListener
    FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
    dialog.show(fragmentManager, END_SESSION_FRAGMENT_TAG);
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
    mElapsedTimeWheelView.setEnabled(false);
    flipToButtonPage(FLIPPER_PAGE_READING_BUTTONS);
    final Animation pulse = AnimationUtils.loadAnimation(getActivity(), R.anim.pulse);
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

  private void refreshElapsedTime() {
    final long elapsedMilliseconds = mSessionTimer.getElapsedMs();
    Log.i(TAG, "Updating duration: " + elapsedMilliseconds);
    int elapsedMinutes = (int) (elapsedMilliseconds / (1000 * 60));
    int currentItem = mElapsedTimeWheelView.getCurrentItem();
    if(elapsedMinutes != currentItem) {
      mElapsedTimeWheelView.setCurrentItem(elapsedMinutes, false, false);
    }
  }

  private void startTimeSpinner() {
    PauseableSpinAnimation spinAnimation = (PauseableSpinAnimation) mTimeSpinner.getAnimation();
    if(spinAnimation != null) {
      spinAnimation.resume();
    }

    mElapsedTimeWheelView.setEnabled(true);
    mElapsedTimeWheelView.setVisibility(View.VISIBLE);
  }

  private void pauseTimeSpinner() {
    PauseableSpinAnimation spinAnimation = (PauseableSpinAnimation) mTimeSpinner.getAnimation();
    if(spinAnimation != null) {
      spinAnimation.pause();
    }

    mElapsedTimeWheelView.setEnabled(false);
  }

  /** Signals that a new position has been set for the current book. */
  public static class NewPositionEvent {
    final private float position;

    public NewPositionEvent(float position) {
      this.position = position;
    }

    public float getPosition() { return position; }
  }

  /** Signals that the current book was finished. */
  public static class BookFinishedEvent {
    final private String closingRemark;

    public BookFinishedEvent(String closingRemark) {
      this.closingRemark = closingRemark;
    }

    public String getClosingRemark() {
      return closingRemark;
    }
  }
}
