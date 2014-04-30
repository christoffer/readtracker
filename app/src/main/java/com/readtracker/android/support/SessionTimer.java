package com.readtracker.android.support;

import android.content.SharedPreferences;
import android.util.Log;

import com.readtracker.android.db.Session;

/**
 * A timer for reading sessions.
 * Can be started and stopped and keeps track of total elapsed time.
 * Provides convenience methods for writing or loading from a SharedPreference object.
 */
public class SessionTimer {
  private static final String TAG = SessionTimer.class.getSimpleName();

  private static final String PREF_TIMESTAMP = "TIMESTAMP";
  private static final String PREF_ELAPSED = "ELAPSED";

  private SessionTimerListener mListener;
  private final TimeProvider mTimeProvider;

  private static final long STOPPED_TIME = -1;

  private long mStartTimestampMs;
  private long mElapsedMs;
  private boolean mIsStartedManually = false;

  public SessionTimer() {
    this(new SystemTimeProvider());
  }

  public SessionTimer(TimeProvider timeProvider) {
    mTimeProvider = timeProvider;
    mStartTimestampMs = STOPPED_TIME;
    mElapsedMs = 0;
  }

  @Override public String toString() {
    if(isStarted()) {
      return String.format("<%s> (%s) Accumulated elapsed time: %s",
          SessionTimer.class.getSimpleName(),
          isRunning() ? "Running since " + mStartTimestampMs : "Stopped",
          mElapsedMs);
    } else {
      return String.format("<%s> (Not yet started)", Session.class.getSimpleName());
    }
  }

  /** Sets the listener to be notified when this timer starts or stops. */
  public void setOnTimerListener(SessionTimerListener listener) {
    mListener = listener;
  }

  /** Starts or resumes the timer. */
  public void start() {
    if(!isRunning()) {
      mIsStartedManually = true;
      mStartTimestampMs = mTimeProvider.getMilliseconds();
      notifyStart();
    }
  }

  /** Stops the timer. */
  public void stop() {
    if(isRunning()) {
      mElapsedMs = getElapsedMs();
      mStartTimestampMs = STOPPED_TIME;
      notifyStop();
    }
  }

  /** Stops the timer if running, otherwise starts it. */
  public void togglePausePlay() {
    if(isRunning()) {
      stop();
    } else {
      start();
    }
  }


  /** Returns the total milliseconds elapsed. */
  public long getElapsedMs() {
    if(isRunning()) {
      return mElapsedMs + (mTimeProvider.getMilliseconds() - mStartTimestampMs);
    } else {
      return mElapsedMs;
    }
  }

  /** Returns true if the timer has been started. */
  public boolean isStarted() {
    // Use a flag + elapsed here since the elapsed ms might be zero when checked immediately
    // after the timer has been started, leading to a false negative.
    return mIsStartedManually || getElapsedMs() > 0;
  }

  /** Returns true if the timer is currently running. */
  public boolean isRunning() {
    return mStartTimestampMs != STOPPED_TIME;
  }

  /** Loads the timer state from preferences. If no stored timer was found, the timer is reset. */
  public void initializeFromPreferences(SharedPreferences prefs) {
    Log.d(TAG, "Loading from preferences");
    if(prefs.contains(PREF_ELAPSED) && prefs.contains(PREF_TIMESTAMP)) {
      final boolean wasRunning = isRunning();

      mElapsedMs = prefs.getLong(PREF_ELAPSED, 0);
      mStartTimestampMs = prefs.getLong(PREF_TIMESTAMP, STOPPED_TIME);

      if(isRunning() && !wasRunning) {
        notifyStart();
      } else if(!isRunning() && wasRunning) {
        notifyStop();
      }
    } else {
      reset();
    }
  }

  /** Saves the timer state to preferences. */
  public void saveToPreferences(SharedPreferences prefs) {
    Log.d(TAG, "Saving to preferences");
    prefs.edit()
      .putLong(PREF_ELAPSED, mElapsedMs)
      .putLong(PREF_TIMESTAMP, mStartTimestampMs)
      .commit();
  }

  /** Removes any timer state from the preferences. */
  public void clearFromPreferences(SharedPreferences prefs) {
    Log.d(TAG, "Clearing from preferences");
    prefs.edit()
      .remove(PREF_ELAPSED)
      .remove(PREF_TIMESTAMP)
      .commit();
  }

  /** Resets the elapsed time to zero. */
  public void reset() {
    reset(0);
  }

  /** Reset the elapsed time for the timer. */
  public void reset(long elapsedMs) {
    mElapsedMs = elapsedMs;
    if(isRunning()) {
      // Restart timer
      mStartTimestampMs = mTimeProvider.getMilliseconds();
    } else {
      mStartTimestampMs = STOPPED_TIME;
    }
  }

  /** Callback interface for getting notified as the timer is started and stopped. */
  public static interface SessionTimerListener {
    /** Called when the timer start. */
    public void onSessionTimerStarted();

    /** Called when the timer stops. */
    public void onSessionTimerStopped();
  }

  /** Interface for a provider of time. */
  public static interface TimeProvider {
    public abstract long getMilliseconds();
  }

  /** Simple TimeProvider that returns the system time. */
  public static class SystemTimeProvider implements TimeProvider {
    @Override public long getMilliseconds() {
      return System.currentTimeMillis();
    }
  }

  private void notifyStart() {
    if(mListener != null) {
      mListener.onSessionTimerStarted();
    }
  }

  private void notifyStop() {
    if(mListener != null) {
      mListener.onSessionTimerStopped();
    }
  }
}
