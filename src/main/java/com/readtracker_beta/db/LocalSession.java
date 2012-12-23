package com.readtracker_beta.db;

import android.os.Parcel;
import android.os.Parcelable;
import com.j256.ormlite.field.DatabaseField;

import java.util.Date;

public class LocalSession implements Parcelable {

  // Database Column names

  public static final String SESSION_IDENTIFIER_FIELD_NAME = "sessionIdentifier";
  public static final String PROGRESS_FIELD_NAME = "progress";
  public static final String READING_ID_FIELD_NAME = "readingId";
  public static final String DURATION_SECONDS_FIELD_NAME = "durationSeconds";
  public static final String READMILL_READING_ID_FIELD_NAME = "readmillReadingId";
  public static final String OCCURRED_AT_FIELD_NAME = "occurredAt";
  public static final String SYNCED_WITH_READMILL_FIELD_NAME = "synced_with_readmill";
  public static final String STARTED_ON_PAGE_FIELD_NAME = "started_on_page";
  public static final String ENDED_ON_PAGE_FIELD_NAME = "ended_on_page";
  public static final String NEEDS_RECONNECT_FIELD_NAME = "needs_reconnect";
  public static final String IS_READTRACKER_SESSION_FIELD_NAME = "is_rt_session";

  // Database => Member bindings

  @DatabaseField(generatedId = true)                              public int id;

  @DatabaseField(columnName = SESSION_IDENTIFIER_FIELD_NAME)      public String sessionIdentifier = "";
  @DatabaseField(columnName = PROGRESS_FIELD_NAME)                public double progress = -1;
  @DatabaseField(columnName = READING_ID_FIELD_NAME)              public int readingId = -1;
  @DatabaseField(columnName = DURATION_SECONDS_FIELD_NAME)        public long durationSeconds = 0;
  @DatabaseField(columnName = READMILL_READING_ID_FIELD_NAME)     public long readmillReadingId = -1;
  @DatabaseField(columnName = OCCURRED_AT_FIELD_NAME)             public Date occurredAt = null;
  @DatabaseField(columnName = SYNCED_WITH_READMILL_FIELD_NAME)    public boolean syncedWithReadmill = false;
  @DatabaseField(columnName = STARTED_ON_PAGE_FIELD_NAME)         public int startedOnPage = -1;
  @DatabaseField(columnName = ENDED_ON_PAGE_FIELD_NAME)           public int endedOnPage = -1;
  // TODO Investigate if this field is unnecessary
  @DatabaseField(columnName = NEEDS_RECONNECT_FIELD_NAME)         public boolean needsReconnect = false;
  @DatabaseField(columnName = IS_READTRACKER_SESSION_FIELD_NAME)  public boolean isReadTrackerSession = true;
  public LocalSession() {}

  @Override
  public String toString() {
    return String.format("ReadingSession: %d seconds for ReadmillReading #%d. Occurred @ %s with SessionId: %s", durationSeconds, readmillReadingId, occurredAt.toString(), sessionIdentifier);
  }

  public boolean isConnected() {
    return readmillReadingId > 0;
  }

  public static Parcelable.Creator<LocalSession> CREATOR = new Parcelable.Creator<LocalSession>() {
    @Override
    public LocalSession createFromParcel(Parcel parcel) { return new LocalSession(parcel); }

    @Override
    public LocalSession[] newArray(int size) { return new LocalSession[size]; }
  };

  @Override
  public void writeToParcel(Parcel parcel, int i) {
    parcel.writeInt(id);
    parcel.writeString(sessionIdentifier);
    parcel.writeDouble(progress);
    parcel.writeInt(readingId);
    parcel.writeLong(durationSeconds);
    parcel.writeLong(readmillReadingId);
    parcel.writeLong(occurredAt.getTime());
    parcel.writeInt(syncedWithReadmill ? 1 : 0);
    parcel.writeInt(startedOnPage);
    parcel.writeInt(endedOnPage);
    parcel.writeInt(needsReconnect ? 1 : 0);
    parcel.writeInt(isReadTrackerSession ? 1 : 0);
  }

  public LocalSession(Parcel parcel) {
    id = parcel.readInt();
    sessionIdentifier = parcel.readString();
    progress = parcel.readDouble();
    readingId = parcel.readInt();
    durationSeconds = parcel.readLong();
    readmillReadingId = parcel.readLong();
    occurredAt = new Date(parcel.readLong());
    syncedWithReadmill = parcel.readInt() == 1;
    startedOnPage = parcel.readInt();
    endedOnPage = parcel.readInt();
    needsReconnect = parcel.readInt() == 1;
    isReadTrackerSession = parcel.readInt() == 1;
  }

  @Override
  public int describeContents() {
    return 0;
  }
}
