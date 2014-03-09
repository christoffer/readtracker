package com.readtracker.android.support;

import android.content.SharedPreferences;

/**
 * A timer for reading sessions.
 * Can be started and stopped and keeps track of total elapsed time.
 * Provides convenience methods for writing or loading from a SharedPreference object.
 */
public class SessionTimer {
  private static final String PREF_TIMESTAMP = "TIMESTAMP";
  private static final String PREF_ELAPSED = "ELAPSED";
  private SessionTimerListener mListener;
  private final TimeProvider mTimeProvider;

  private static final long STOPPED_TIME = -1;

  private long mStartTimestamp;
  private long mElapsed;

  public SessionTimer() {
    this(new SystemTimeProvider());
  }

  public SessionTimer(TimeProvider timeProvider) {
    mTimeProvider = timeProvider;
  }

  /** Sets the listener to be notified when this timer starts or stops. */
  public void setOnTimerListener(SessionTimerListener listener) {
    mListener = listener;
  }

  /** Starts or resumes the timer. */
  public void start() {
    if(!isRunning()) {
      mStartTimestamp = mTimeProvider.getTime();
    }
  }

  /** Stops the timer. */
  public void stop() {
    if(isRunning()) {
      mElapsed = getElapsed();
      mStartTimestamp = STOPPED_TIME;
    }
  }

  /** Returns the total time elapsed. */
  public long getElapsed() {
    if(isRunning()) {
      return mElapsed + (mTimeProvider.getTime() - mStartTimestamp);
    } else {
      return mElapsed;
    }
  }

  /** Returns true if the timer is currently running. */
  public boolean isRunning() {
    return mStartTimestamp != STOPPED_TIME;
  }

  /** Returns true if the timer has been started and stopped, and is currently not running. */
  public boolean isPaused() {
    return !isRunning() && mElapsed > 0;
  }

  /** Loads the timer state from preferences. Returns true if it was loaded. */
  public boolean loadFromPreferences(SharedPreferences prefs) {
    if(prefs.contains(PREF_ELAPSED) && prefs.contains(PREF_TIMESTAMP)) {
      mElapsed = prefs.getLong(PREF_ELAPSED, 0);
      mStartTimestamp = prefs.getLong(PREF_TIMESTAMP, STOPPED_TIME);
      return true;
    }
    return false;
  }


  /** Saves the timer state to preferences. */
  public void saveToPreferences(SharedPreferences prefs) {
    prefs.edit()
      .putLong(PREF_ELAPSED, mElapsed)
      .putLong(PREF_TIMESTAMP, mStartTimestamp)
      .commit();
  }

  /** Removes any timer state from the prefernces. */
  public void clearFromPreferences(SharedPreferences prefs) {
    prefs.edit()
      .remove(PREF_ELAPSED)
      .remove(PREF_TIMESTAMP)
      .commit();
  }

  /** Callback interface for getting notified as the timer is started and stopped. */
  public static interface SessionTimerListener {
    /** Called when the timer start. */
    public void onStarted();

    /** Called when the timer stops. */
    public void onStopped();
  }

  /** Interface for a provider of time. */
  public static interface TimeProvider {
    public abstract long getTime();
  }

  /** Simple TimeProvider that returns the system time. */
  public static class SystemTimeProvider implements TimeProvider {
    @Override public long getTime() {
      return System.currentTimeMillis();
    }
  }
}
