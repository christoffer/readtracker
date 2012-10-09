package com.readtracker.db;

import android.os.Parcel;
import android.os.Parcelable;
import com.j256.ormlite.field.DatabaseField;

import java.util.Date;

/**
 * A locally made highlight.
 * Might be connected to a remote (Readmill) highlight.
 */
public class LocalHighlight implements Parcelable {
  public static final String READING_ID_FIELD_NAME = "reading_id";

  public static final String CONTENT_FIELD_NAME = "content";
  public static final String HIGHLIGHTED_AT_FIELD_NAME = "highlighted_at";
  public static final String POSITION_FIELD_NAME = "position";


  public static final String SYNCED_AT_FIELD_NAME = "synced_at";
  public static final String READMILL_HIGHLIGHT_ID_FIELD_NAME = "rm_highlight_id";
  public static final String READMILL_USER_ID_FIELD_NAME = "rm_user_id";
  public static final String READMILL_READING_ID_FIELD_NAME = "rm_reading_id";
  private static final String READMILL_PERMALINK_URL_FIELD_NAME = "rm_permalink";

  // Database => Member bindings

  @DatabaseField(generatedId = true)                              public int id = 0;

  @DatabaseField(columnName = READING_ID_FIELD_NAME)              public long readingId = -1;

  @DatabaseField(columnName = CONTENT_FIELD_NAME)                 public String content;
  @DatabaseField(columnName = HIGHLIGHTED_AT_FIELD_NAME)          public Date highlightedAt = null;
  @DatabaseField(columnName = POSITION_FIELD_NAME)                public double position = 0.0f;

  @DatabaseField(columnName = SYNCED_AT_FIELD_NAME)               public Date syncedAt = null;
  @DatabaseField(columnName = READMILL_HIGHLIGHT_ID_FIELD_NAME)   public long readmillHighlightId = -1;
  @DatabaseField(columnName = READMILL_READING_ID_FIELD_NAME)     public long readmillReadingId = -1;
  @DatabaseField(columnName = READMILL_USER_ID_FIELD_NAME)        public long readmillUserId = -1;
  @DatabaseField(columnName = READMILL_PERMALINK_URL_FIELD_NAME)  public String readmillPermalinkUrl;

  public LocalHighlight() {}

  @Override
  public String toString() {
    return "[Highlight id: " + id + " content: <" + content + "> Readmill Id: " + readmillHighlightId + " url: " + readmillPermalinkUrl + "]";
  }

  public static Parcelable.Creator<LocalHighlight> CREATOR = new Parcelable.Creator<LocalHighlight>() {
    @Override
    public LocalHighlight createFromParcel(Parcel parcel) { return new LocalHighlight(parcel); }

    @Override
    public LocalHighlight[] newArray(int size) { return new LocalHighlight[size]; }
  };

  public boolean isConnected() {
    return readmillReadingId > 0;
  }

  @Override
  public void writeToParcel(Parcel parcel, int i) {
    parcel.writeInt(id);
    parcel.writeLong(readingId);
    parcel.writeString(content);
    parcel.writeLong(highlightedAt.getTime());
    parcel.writeDouble(position);
    parcel.writeLong(syncedAt == null ? 0 : syncedAt.getTime());
    parcel.writeLong(readmillHighlightId);
    parcel.writeLong(readmillReadingId);
    parcel.writeLong(readmillUserId);
    parcel.writeString(readmillPermalinkUrl == null ? "" : readmillPermalinkUrl);
  }

  public LocalHighlight(Parcel parcel) {
    id = parcel.readInt();
    readingId = parcel.readLong();
    content = parcel.readString();
    highlightedAt = new Date(parcel.readLong());
    position = parcel.readDouble();
    long storedSyncedAt = parcel.readLong();
    syncedAt = storedSyncedAt == 0 ? null : new Date(storedSyncedAt);
    readmillHighlightId = parcel.readLong();
    readmillReadingId = parcel.readLong();
    readmillUserId = parcel.readLong();
    readmillPermalinkUrl = parcel.readString();
  }

  @Override
  public int describeContents() {
    return 0;
  }
}
