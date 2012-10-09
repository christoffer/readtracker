package com.readtracker.tasks;

import com.readtracker.db.LocalReading;

public class ReadmillSyncProgressMessage {

  private Float mProgress;
  private LocalReading mLocalReading;
  private String mMessage;

  public ReadmillSyncProgressMessage(LocalReading localReading) {
    mLocalReading = localReading;
  }

  public ReadmillSyncProgressMessage(String message, Float progress) {
    mMessage = message;
    mProgress = progress;
  }

  public LocalReading getLocalReading() {
    return mLocalReading;
  }

  public Float getProgress() {
    return mProgress;
  }

  public String toString() {
    return mMessage == null ? "" : mMessage;
  }
}
