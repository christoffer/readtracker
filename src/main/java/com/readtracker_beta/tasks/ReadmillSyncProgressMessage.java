package com.readtracker_beta.tasks;

import com.readtracker_beta.db.LocalReading;

public class ReadmillSyncProgressMessage {
   enum MessageType {
     READING_CHANGED,
     READING_DELETED,
     SYNC_PROGRESS
   }

  private MessageType mMessageType;

  private Float mProgress;
  private LocalReading mLocalReading;
  private String mMessage;

  public static ReadmillSyncProgressMessage readingChanged(LocalReading localReading) {
    return new ReadmillSyncProgressMessage(MessageType.READING_CHANGED, "", localReading, null);
  }

  public static ReadmillSyncProgressMessage readingDeleted(LocalReading localReading) {
    return new ReadmillSyncProgressMessage(MessageType.READING_DELETED, "", localReading, null);
  }

  public static ReadmillSyncProgressMessage syncProgress(float progress, String message) {
    return new ReadmillSyncProgressMessage(MessageType.SYNC_PROGRESS, message, null, null);
  }

  private ReadmillSyncProgressMessage(MessageType messageType, String message, LocalReading localReading, Float progress) {
    mMessage = message;
    mProgress = progress;
    mLocalReading = localReading;
    mMessageType = messageType;
  }

  public MessageType getMessageType() {
    return mMessageType;
  }

  public LocalReading getLocalReading() {
    return mLocalReading;
  }

  public Float getProgress() {
    return mProgress;
  }

  public String getMessage() {
    return mMessage == null ? "" : mMessage;
  }

  public String toString() {
    return getMessage();
  }
}
