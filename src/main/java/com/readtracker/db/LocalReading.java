package com.readtracker.db;

import android.os.Parcel;
import android.os.Parcelable;
import com.j256.ormlite.field.DatabaseField;
import com.readtracker.readmill.ReadmillApiHelper;

import java.util.List;

/**
 * A local reading stored on the device. May, or may not, be connected to a
 * remote (Readmill) reading.
 * <p/>
 * Stores additional information that is not necessarily available on the remote
 * reading (like page numbers).
 */
public class LocalReading implements Parcelable {

  // Database Column names

  public static final String TITLE_FIELD_NAME = "title";
  public static final String AUTHOR_FIELD_NAME = "author";
  public static final String COVER_URL_FIELD_NAME = "coverURL";

  public static final String TOTAL_PAGES_FIELD_NAME = "totalPages";
  public static final String CURRENT_PAGE_FIELD_NAME = "currentPage";
  public static final String MEASURE_IN_PERCENT = "measure_in_percent";

  public static final String PROGRESS_FIELD_NAME = "rt_progress";
  public static final String TIME_SPENT_FIELD_NAME = "timeSpent";
  public static final String LAST_READ_AT_FIELD_NAME = "lastReadAt";
  public static final String LOCALLY_CLOSED_AT_FIELD_NAME = "closed_at";

  public static final String READMILL_USER_ID_FIELD_NAME = "rm_user_id";
  public static final String READMILL_BOOK_ID_FIELD_NAME = "rm_book_id";
  public static final String READMILL_READING_ID_FIELD_NAME = "rm_reading_id";

  public static final String READMILL_TOUCHED_AT_FIELD_NAME = "rm_touched_at";
  public static final String READMILL_STATE_FIELD_NAME = "rm_state";
  public static final String READMILL_CLOSING_REMARK = "rm_closing_remark";


  // Database => Member bindings

  @DatabaseField(generatedId = true)
  public int id = 0;

  @DatabaseField(columnName = TITLE_FIELD_NAME) public String title;
  @DatabaseField(columnName = AUTHOR_FIELD_NAME) public String author;
  @DatabaseField(columnName = COVER_URL_FIELD_NAME) public String coverURL;

  @DatabaseField(columnName = TOTAL_PAGES_FIELD_NAME) public long totalPages = 0;
  @DatabaseField(columnName = CURRENT_PAGE_FIELD_NAME) public long currentPage = 0;
  @DatabaseField(columnName = MEASURE_IN_PERCENT) public boolean measureInPercent = false;
  @DatabaseField(columnName = PROGRESS_FIELD_NAME) public double progress = 0.0f;
  @DatabaseField(columnName = TIME_SPENT_FIELD_NAME) public long timeSpentMillis = 0;
  @DatabaseField(columnName = LAST_READ_AT_FIELD_NAME) public long lastReadAt = 0;

  @DatabaseField(columnName = READMILL_READING_ID_FIELD_NAME) public long readmillReadingId = -1;
  @DatabaseField(columnName = READMILL_BOOK_ID_FIELD_NAME) public long readmillBookId = -1;
  @DatabaseField(columnName = READMILL_USER_ID_FIELD_NAME) public long readmillUserId = -1;

  @DatabaseField(columnName = READMILL_TOUCHED_AT_FIELD_NAME) public long readmillTouchedAt = 0;
  @DatabaseField(columnName = READMILL_STATE_FIELD_NAME)
  public int readmillState = ReadmillApiHelper.ReadingState.READING;
  @DatabaseField(columnName = READMILL_CLOSING_REMARK) public String readmillClosingRemark = "";

  @DatabaseField(columnName = LOCALLY_CLOSED_AT_FIELD_NAME) public long locallyClosedAt;

  // Virtual attributes

  // Caching of the progresses of all LocalSessions for this reading
  private float[] progressStops;

  public LocalReading() {
  }

  public void refreshProgress() {
    if(!hasPageInfo())
      return;
    if(currentPage > totalPages) {
      progress = 1.0;
    } else {
      progress = ((double) currentPage / (double) totalPages);
    }
  }

  public int getProgressPercent() {
    return (int) (progress * 100);
  }

  private long getTimeSpentSeconds() {
    return timeSpentMillis / 1000;
  }

  public long estimateTimeLeft() {
    if(!isActive() || progress == 0 || timeSpentMillis == 0 || progress > 1.0f) {
      return 0;
    }
    return (long) ((timeSpentMillis / progress) - timeSpentMillis);
  }

  /**
   * Gets the name of the user that the reading was marked via
   *
   * @return The user name or null if the via attribute is missing
   *         TODO Implement
   */
  public String getFoundVia() {
    return "NOT YET IMPLEMENTED";
  }

  public boolean hasPageInfo() {
    return totalPages > 0;
  }

  @Override
  public String toString() {
    return String.format("LocalReading #%d: \"%s\" by \"%s\"", id, title, author);
  }

  public String getInfo() {
    return String.format("LocalReading #%d \"%s\" by \"%s\" - Page (%d/%d) over %d seconds - Readmill Reading #%s, Book #%s, User #%s State %d Closing Remark '%s' - Cover '%s'", id, title, author, currentPage, totalPages, getTimeSpentSeconds(), readmillReadingId, readmillBookId, readmillUserId, readmillState, readmillClosingRemark, coverURL);
  }

  public boolean isActive() {
    return readmillState == ReadmillApiHelper.ReadingState.READING;
  }

  public boolean isMeasuredInPercent() {
    return measureInPercent;
  }

  public boolean isInteresting() {
    return readmillState == ReadmillApiHelper.ReadingState.INTERESTING;
  }

  public boolean isConnected() {
    return readmillReadingId > 0;
  }

  public boolean hasClosingRemark() {
    return readmillClosingRemark != null && readmillClosingRemark.length() > 0;
  }

  public void setProgressStops(final List<LocalSession> sessions) {
    progressStops = new float[sessions.size()];
    for(int i = 0, sessionsSize = sessions.size(); i < sessionsSize; i++) {
      LocalSession session = sessions.get(i);
      progressStops[i] = (float) session.progress;
    }
  }

  public float[] getProgressStops() {
    return this.progressStops;
  }

  public static Creator<LocalReading> CREATOR = new Creator<LocalReading>() {
    @Override
    public LocalReading createFromParcel(Parcel parcel) {
      return new LocalReading(parcel);
    }

    @Override
    public LocalReading[] newArray(int size) {
      return new LocalReading[size];
    }
  };

  @Override
  public void writeToParcel(Parcel parcel, int i) {
    parcel.writeInt(id);
    parcel.writeString(title);
    parcel.writeString(author);
    parcel.writeString(coverURL);
    parcel.writeLong(totalPages);
    parcel.writeLong(currentPage);
    parcel.writeDouble(progress);
    parcel.writeLong(timeSpentMillis);
    parcel.writeLong(lastReadAt);
    parcel.writeLong(readmillReadingId);
    parcel.writeLong(readmillBookId);
    parcel.writeLong(readmillUserId);
    parcel.writeLong(readmillTouchedAt);
    parcel.writeInt(readmillState);
    parcel.writeString(readmillClosingRemark);
    parcel.writeInt(measureInPercent ? 1 : 0);
    parcel.writeInt(progressStops == null ? 0 : progressStops.length);
    parcel.writeFloatArray(progressStops);
  }

  public LocalReading(Parcel parcel) {
    id = parcel.readInt();
    title = parcel.readString();
    author = parcel.readString();
    coverURL = parcel.readString();
    totalPages = parcel.readLong();
    currentPage = parcel.readLong();
    progress = parcel.readDouble();
    timeSpentMillis = parcel.readLong();
    lastReadAt = parcel.readLong();
    readmillReadingId = parcel.readLong();
    readmillBookId = parcel.readLong();
    readmillUserId = parcel.readLong();
    readmillTouchedAt = parcel.readLong();
    readmillState = parcel.readInt();
    readmillClosingRemark = parcel.readString();
    measureInPercent = parcel.readInt() == 1;
    if(parcel.readInt() > 0) {
      parcel.readFloatArray(progressStops);
    }
  }

  @Override
  public int describeContents() {
    return 0;
  }
}
