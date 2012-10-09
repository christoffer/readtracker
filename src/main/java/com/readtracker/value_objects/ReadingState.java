package com.readtracker.value_objects;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Represents a current reading of a local reading.
 * Used to pass state between ActivityHome and ActivityBook
 */
public class ReadingState implements Parcelable {
  private int mLocalReadingId = 0;
  private long mElapsedMilliseconds = 0;
  private boolean mIsActive;

  public ReadingState(int localReadingId, long elapsedMilliseconds, boolean isActive) {
    mLocalReadingId = localReadingId;
    mElapsedMilliseconds = elapsedMilliseconds;
    mIsActive = isActive;
  }

  public int getLocalReadingId() { return mLocalReadingId; }

  public long getElapsedMilliseconds() { return mElapsedMilliseconds; }

  public boolean getIsActive() { return mIsActive; }

  public String toString() {
    return String.format("LocalReading (%s) #%d @ %d", (mIsActive ? "Active" : "Inactive"), mLocalReadingId, mElapsedMilliseconds);
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
    mIsActive = (parcel.readInt() == 1);
  }

  @Override public void writeToParcel(Parcel parcel, int flags) {
    parcel.writeInt(mLocalReadingId);
    parcel.writeLong(mElapsedMilliseconds);
    parcel.writeInt(mIsActive ? 1 : 0);
  }

  @Override public int describeContents() { return 0; }

}
