package com.readtracker_beta.support;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import com.readtracker_beta.interfaces.SessionTimerEventListener;

import java.util.Date;

/**
 * Represents a current timing session.
 * A timing session is measured by two parts:
 * - the elapsed time, in milliseconds, up until the timer was last stopped
 * - a timestamp when the timer was last started
 * <p/>
 * The timer is considered active when a timestamp since last started is present.
 */
public class SessionTimer implements Parcelable {
  private int mLocalReadingId = -1;
  private long mElapsedBeforeTimestamp = 0;
  private long mActiveTimestamp = 0;
  private SessionTimerEventListener mListener;

  public SessionTimer(int localReadingId, long elapsedMilliseconds, long activeTimestamp) {
    mLocalReadingId = localReadingId;
    mElapsedBeforeTimestamp = elapsedMilliseconds;
    mActiveTimestamp = activeTimestamp;
  }

  public SessionTimer(int localReadingId) {
    this(localReadingId, 0, 0);
  }

  public int getLocalReadingId() {
    return mLocalReadingId;
  }

  public long getElapsedBeforeTimestamp() {
    return mElapsedBeforeTimestamp;
  }

  public long getActiveTimestamp() {
    return mActiveTimestamp;
  }

  public void setEventListener(SessionTimerEventListener listener) {
    mListener = listener;
  }

  /**
   * Indicate if the timing is active or not
   *
   * @return true if active, false otherwise
   */
  public boolean isActive() {
    return mActiveTimestamp > 0;
  }

  /**
   * Get the total elapsed time, including any time passed since the
   * active timestamp.
   */
  public long getTotalElapsed() {
    return getTotalElapsed(System.currentTimeMillis());
  }

  public long getTotalElapsed(long now) {
    final long elapsedSinceTimestamp = isActive() ? (now - mActiveTimestamp) : 0;
    return mElapsedBeforeTimestamp + elapsedSinceTimestamp;
  }

  public String toString() {
    return String.format("SessionTimer for Reading#%d: Elapsed: %dms (%s)", mLocalReadingId, mElapsedBeforeTimestamp, (isActive() ? "Active, started: " + new Date(mActiveTimestamp) : "Inactive"));
  }

  // Parcelable interface

  public static Parcelable.Creator<SessionTimer> CREATOR = new Parcelable.Creator<SessionTimer>() {
    @Override
    public SessionTimer createFromParcel(Parcel parcel) {
      return new SessionTimer(parcel);
    }

    @Override public SessionTimer[] newArray(int size) {
      return new SessionTimer[size];
    }
  };

  public SessionTimer(Parcel parcel) {
    mLocalReadingId = parcel.readInt();
    mElapsedBeforeTimestamp = parcel.readLong();
    mActiveTimestamp = parcel.readLong();
  }

  @Override public int describeContents() { return 0; }

  @Override public void writeToParcel(Parcel parcel, int flags) {
    parcel.writeInt(mLocalReadingId);
    parcel.writeLong(mElapsedBeforeTimestamp);
    parcel.writeLong(mActiveTimestamp);
  }

  /**
   * Pauses the reading state.
   */
  public void stop() {
    stop(System.currentTimeMillis());
  }

  /**
   * Pauses the reading state.
   */
  public void stop(long now) {
    stopTimer(now);
    if(mListener != null) {
      mListener.onStopped();
    }
  }

  /**
   * Start timing
   */
  public void start() {
    start(System.currentTimeMillis());
  }

  public void start(long now) {
    startTimer(now);
    if(mListener != null) {
      mListener.onStarted();
    }
  }

  /**
   * Pause/resume
   */
  public void togglePause() {
    togglePause(System.currentTimeMillis());
  }

  public void togglePause(long now) {
    if(isActive()) {
      stop(now);
    } else {
      start(now);
    }
  }

  /**
   * Set the elapsed time in milliseconds.
   *
   * @param elapsed The elapsed time in milliseconds
   */
  public void setElapsedMillis(int elapsed) {
    if(isActive()) {
      final long now = System.currentTimeMillis();
      stopTimer(now);
      mElapsedBeforeTimestamp = elapsed;
      startTimer(now);
    } else {
      mElapsedBeforeTimestamp = elapsed;
    }
  }

  /**
   * Stops the timer and updates the elapsed time without firing the callback.
   *
   * @param now Current time in epoch milliseconds
   */
  private void stopTimer(long now) {
    if(isActive()) {
      final long elapsedSinceTimeStamp = now - mActiveTimestamp;
      mElapsedBeforeTimestamp += elapsedSinceTimeStamp;
      mActiveTimestamp = 0;
    }
  }

  /**
   * Starts the timer without firing the callback.
   *
   * @param now Current time in epoch milliseconds.
   */
  private void startTimer(long now) {
    mActiveTimestamp = now;
  }
}
