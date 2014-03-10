package com.readtracker.android.support;

import android.content.SharedPreferences;
import android.util.Log;

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

  public SessionTimer() {
    this(new SystemTimeProvider());
  }

  public SessionTimer(TimeProvider timeProvider) {
    mTimeProvider = timeProvider;
  }

  @Override public String toString() {
    return String.format("<%s> (%s) Accumulated elapsed time: %s",
      SessionTimer.class.getSimpleName(),
      isRunning() ? "Running since " + mStartTimestampMs : "Stopped",
      mElapsedMs);
  }

  /** Sets the listener to be notified when this timer starts or stops. */
  public void setOnTimerListener(SessionTimerListener listener) {
    mListener = listener;
  }

  /** Starts or resumes the timer. */
  public void start() {
    if(!isRunning()) {
      mStartTimestampMs = mTimeProvider.getMilliseconds();
      if(mListener != null) {
        mListener.onSessionTimerStarted();
      }
    }
  }

  /** Stops the timer. */
  public void stop() {
    if(isRunning()) {
      mElapsedMs = getElapsedMs();
      mStartTimestampMs = STOPPED_TIME;
      if(mListener != null) {
        mListener.onSessionTimerStopped();
      }
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

  /** Returns true if the timer is currently running. */
  public boolean isRunning() {
    return mStartTimestampMs != STOPPED_TIME;
  }

  /** Returns true if the timer has been started and stopped, and is currently not running. */
  public boolean isPaused() {
    return !isRunning() && mElapsedMs > 0;
  }

  /** Loads the timer state from preferences. Returns true if it was loaded. */
  public boolean loadFromPreferences(SharedPreferences prefs) {
    Log.d(TAG, "Loading from prefences");
    if(prefs.contains(PREF_ELAPSED) && prefs.contains(PREF_TIMESTAMP)) {
      mElapsedMs = prefs.getLong(PREF_ELAPSED, 0);
      mStartTimestampMs = prefs.getLong(PREF_TIMESTAMP, STOPPED_TIME);
      return true;
    }
    return false;
  }

  /** Saves the timer state to preferences. */
  public void saveToPreferences(SharedPreferences prefs) {
    Log.d(TAG, "Saving to prefences");
    prefs.edit()
      .putLong(PREF_ELAPSED, mElapsedMs)
      .putLong(PREF_TIMESTAMP, mStartTimestampMs)
      .commit();
  }

  /** Removes any timer state from the prefernces. */
  public void clearFromPreferences(SharedPreferences prefs) {
    Log.d(TAG, "Clearing from prefences");
    prefs.edit()
      .remove(PREF_ELAPSED)
      .remove(PREF_TIMESTAMP)
      .commit();
  }

  /** Resets the timer to zero. */
  public void reset() {
    reset(0);
  }

  /** Resets the timer to a specific elapsed time. */
  public void reset(long elapsedMs) {
    mElapsedMs = elapsedMs;
    mStartTimestampMs = STOPPED_TIME;
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
}
