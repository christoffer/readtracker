package com.readtracker.value_objects;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Date;

/**
 * Represents a current reading of a local reading.
 * Used to pass state between ActivityHome and ActivityBook
 */
public class ReadingState implements Parcelable {
  private int mLocalReadingId = 0;
  private long mElapsedMilliseconds = 0;
  private long mActiveTimestamp;

  public ReadingState(int localReadingId, long elapsedMilliseconds, long activeTimestamp) {
    mLocalReadingId = localReadingId;
    mElapsedMilliseconds = elapsedMilliseconds;
    mActiveTimestamp = activeTimestamp;
  }

  public int getLocalReadingId() { return mLocalReadingId; }

  public long getElapsedMilliseconds() { return mElapsedMilliseconds; }

  public boolean isActive() { return mActiveTimestamp > 0; }

  public long getActiveTimestamp() { return mActiveTimestamp; }

  public String toString() {
    return String.format("LocalReading (%s) #%d @ %d", (isActive() ? "Active, started: " + new Date(mActiveTimestamp) : "Inactive"), mLocalReadingId, mElapsedMilliseconds);
  }

  // Parcelable interface

  public static Parcelable.Creator<ReadingState> CREATOR = new Parcelable.Creator<ReadingState>() {
    @Override
    public ReadingState createFromParcel(Parcel parcel) {
      return new ReadingState(parcel);
    }

    @Override public ReadingState[] newArray(int size) {
      return new ReadingState[size];
    }
  };

  public ReadingState(Parcel parcel) {
    mLocalReadingId = parcel.readInt();
    mElapsedMilliseconds = parcel.readLong();
    mActiveTimestamp = parcel.readLong();
  }

  @Override public int describeContents() { return 0; }

  @Override public void writeToParcel(Parcel parcel, int flags) {
    parcel.writeInt(mLocalReadingId);
    parcel.writeLong(mElapsedMilliseconds);
    parcel.writeLong(mActiveTimestamp);
  }

  /**
   * Pauses the reading state.
   */
  public void pause() {
    pause(System.currentTimeMillis());
  }

  /**
   * Pauses the reading state.
   */
  public void pause(long now) {
    if(mActiveTimestamp != 0) {
      final long elapsedSinceTimeStamp = now - mActiveTimestamp;
      mElapsedMilliseconds += elapsedSinceTimeStamp;
      mActiveTimestamp = 0;
    }
  }
}
