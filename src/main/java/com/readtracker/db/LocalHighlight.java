package com.readtracker.db;

import android.os.Parcel;
import android.os.Parcelable;
import com.j256.ormlite.dao.Dao;
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

  public static final String EDITED_AT_FIELD_NAME = "edited_at";
  public static final String SYNCED_AT_FIELD_NAME = "synced_at";
  public static final String DELETED_BY_USER_FIELD_NAME = "user_deleted";

  public static final String READMILL_HIGHLIGHT_ID_FIELD_NAME = "rm_highlight_id";
  public static final String READMILL_USER_ID_FIELD_NAME = "rm_user_id";
  public static final String READMILL_READING_ID_FIELD_NAME = "rm_reading_id";
  public static final String READMILL_PERMALINK_URL_FIELD_NAME = "rm_permalink";

  public static final String COMMENT_FIELD_NAME = "comment";
  public static final String COMMENT_COUNT_FIELD_NAME = "comment_count";
  public static final String LIKE_COUNT_FIELD_NAME = "like_count";

  // Database => Member bindings

  @DatabaseField(generatedId = true)                              public int id = 0;

  @DatabaseField(columnName = READING_ID_FIELD_NAME)              public long readingId = -1;

  @DatabaseField(columnName = CONTENT_FIELD_NAME)                 public String content;
  @DatabaseField(columnName = HIGHLIGHTED_AT_FIELD_NAME)          public Date highlightedAt = null;
  @DatabaseField(columnName = POSITION_FIELD_NAME)                public double position = 0.0f;

  @DatabaseField(columnName = EDITED_AT_FIELD_NAME)               public Date editedAt = null;
  @DatabaseField(columnName = SYNCED_AT_FIELD_NAME)               public Date syncedAt = null;
  @DatabaseField(columnName = DELETED_BY_USER_FIELD_NAME)         public boolean deletedByUser = false;

  @DatabaseField(columnName = READMILL_HIGHLIGHT_ID_FIELD_NAME)   public long readmillHighlightId = -1;
  @DatabaseField(columnName = READMILL_READING_ID_FIELD_NAME)     public long readmillReadingId = -1;
  @DatabaseField(columnName = READMILL_USER_ID_FIELD_NAME)        public long readmillUserId = -1;
  @DatabaseField(columnName = READMILL_PERMALINK_URL_FIELD_NAME)  public String readmillPermalinkUrl;

  @DatabaseField(columnName = COMMENT_FIELD_NAME)                 public String comment;
  @DatabaseField(columnName = COMMENT_COUNT_FIELD_NAME)           public int commentCount = 0;
  @DatabaseField(columnName = LIKE_COUNT_FIELD_NAME)              public int likeCount = 0;

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

  /**
   * Checks the presence of a comment on the highlight.
   *
   * @return true if the highlight has a comment attached.
   */
  public boolean hasComment() {
    return comment != null && comment.length() > 0;
  }

  /**
   * Determine if the highlight is synced up and until the given timestamp.
   *
   * @param when Date to compare with
   * @return true if when is null or the highlight is synced at or after the date. False otherwise.
   */
  public boolean lastSyncedBefore(Date when) {
    return syncedAt != null && syncedAt.before(when);
  }

  /**
   * Determine if this highlight is edited after the given instant.
   *
   * @param when Instant to compare with
   * @return true if the highlight has been edited after the given instant
   */
  public boolean isEditedAfter(Date when) {
    return editedAt != null && editedAt.after(when);
  }

  /**
   * Checks whether the highlight is connected to Readmill or not.
   * @return true if the highlight is connected to Readmill.
   */
  public boolean isOfflineOnly() {
    return readmillHighlightId < 1;
  }

  @Override
  public void writeToParcel(Parcel parcel, int i) {
    parcel.writeInt(id);
    parcel.writeLong(readingId);
    parcel.writeString(content);
    parcel.writeLong(highlightedAt.getTime());
    parcel.writeDouble(position);
    parcel.writeLong(editedAt == null ? 0 : editedAt.getTime());
    parcel.writeLong(syncedAt == null ? 0 : syncedAt.getTime());
    parcel.writeInt(deletedByUser ? 1 : 0);
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

    long storedEditedAt = parcel.readLong();
    editedAt = storedEditedAt == 0 ? null : new Date(storedEditedAt);

    long storedSyncedAt = parcel.readLong();
    syncedAt = storedSyncedAt == 0 ? null : new Date(storedSyncedAt);

    deletedByUser = parcel.readInt() == 1;
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
