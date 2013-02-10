package com.readtracker.db;

import android.graphics.Color;
import android.os.Parcel;
import android.os.Parcelable;
import com.j256.ormlite.field.DatabaseField;
import com.readtracker.support.ReadmillApiHelper;

import java.util.Date;
import java.util.List;

/**
 * A local reading stored on the device. May, or may not, be connected to a
 * remote (Readmill) reading.
 * <p/>
 * Stores additional information that is not necessarily available on the remote
 * reading (like page numbers).
 */
public class LocalReading implements Parcelable {
  private int mColor = -1; // Cache of the calculated color for this Reading

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

  public static final String READMILL_RECOMMENDED_FIELD_NAME = "rm_recommend";
  public static final String READMILL_IS_PRIVATE_FIELD_NAME = "rm_is_private";

  public static final String DELETED_BY_USER_FIELD_NAME = "user_deleted";
  public static final String STARTED_AT_FIELD_NAME = "started_at";

  // Database => Member bindings

  @DatabaseField(generatedId = true)
  public int id = 0;

  @DatabaseField(columnName = TITLE_FIELD_NAME)
  public String title;

  @DatabaseField(columnName = AUTHOR_FIELD_NAME)
  public String author;

  @DatabaseField(columnName = COVER_URL_FIELD_NAME)
  public String coverURL;

  @DatabaseField(columnName = TOTAL_PAGES_FIELD_NAME)
  public long totalPages = 0;

  @DatabaseField(columnName = CURRENT_PAGE_FIELD_NAME)
  public long currentPage = 0;

  @DatabaseField(columnName = MEASURE_IN_PERCENT)
  public boolean measureInPercent = false;

  @DatabaseField(columnName = PROGRESS_FIELD_NAME)
  public double progress = 0.0f;

  @DatabaseField(columnName = TIME_SPENT_FIELD_NAME)
  public long timeSpentMillis = 0;

  @DatabaseField(columnName = LAST_READ_AT_FIELD_NAME)
  public long lastReadAt = 0;

  @DatabaseField(columnName = LOCALLY_CLOSED_AT_FIELD_NAME)
  protected long locallyClosedAt;

  @DatabaseField(columnName = DELETED_BY_USER_FIELD_NAME)
  public boolean deletedByUser = false;

  @DatabaseField(columnName = STARTED_AT_FIELD_NAME)
  public long startedAt = 0;

  // TODO These should be in another object
  @DatabaseField(columnName = READMILL_RECOMMENDED_FIELD_NAME)
  public boolean readmillRecommended = false;

  @DatabaseField(columnName = READMILL_IS_PRIVATE_FIELD_NAME)
  public boolean readmillPrivate = false;

  @DatabaseField(columnName = READMILL_READING_ID_FIELD_NAME)
  public long readmillReadingId = -1;

  @DatabaseField(columnName = READMILL_BOOK_ID_FIELD_NAME)
  public long readmillBookId = -1;

  @DatabaseField(columnName = READMILL_USER_ID_FIELD_NAME)
  public long readmillUserId = -1;

  @DatabaseField(columnName = READMILL_TOUCHED_AT_FIELD_NAME)
  public long readmillTouchedAt = 0;

  @DatabaseField(columnName = READMILL_STATE_FIELD_NAME)
  public int readmillState = ReadmillApiHelper.ReadingState.READING;

  @DatabaseField(columnName = READMILL_CLOSING_REMARK)
  public String readmillClosingRemark = "";

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

  /**
   * Returns the estimated time left to finish the book.
   *
   * @return The estimated time left in seconds.
   */
  public int estimateTimeLeft() {
    if(!isActive() || progress == 0 || timeSpentMillis == 0 || progress > 1.0f) {
      return 0;
    }
    long estimatedMillisecondsLeft = (long) ((1.0 - progress) * timeSpentMillis);
    return (int) (estimatedMillisecondsLeft / 1000);
  }

  public void setCurrentPage(long currentPage) {
    this.currentPage = currentPage;
    refreshProgress();
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

  public int getColor() {
    if(mColor == -1) { // need recalculation
      final String colorKey = title + author + readmillReadingId;
      float color = 360 * (Math.abs(colorKey.hashCode()) / (float) Integer.MAX_VALUE);
      mColor = Color.HSVToColor(new float[]{
        color,
        0.4f,
        0.5f
      });
    }
    return mColor;
  }

  @Override
  public String toString() {
    final String info = String.format("LocalReading #%d: \"%s\" by \"%s\"", id, title, author);

    if(deletedByUser) {
      return "[Deleted] " + info;
    }

    return info;
  }

  public String getInfo() {
    return String.format("LocalReading #%d \"%s\" by \"%s\" - Page (%d/%d) over %d seconds - Readmill Reading #%s, Book #%s, User #%s State %d Closing Remark '%s' - Cover '%s'", id, title, author, currentPage, totalPages, getTimeSpentSeconds(), readmillReadingId, readmillBookId, readmillUserId, readmillState, readmillClosingRemark, coverURL);
  }

  public boolean isActive() {
    return readmillState == ReadmillApiHelper.ReadingState.READING;
  }

  public boolean isClosed() {
    return locallyClosedAt > 0 ||
      readmillState == ReadmillApiHelper.ReadingState.FINISHED ||
      readmillState == ReadmillApiHelper.ReadingState.ABANDONED;
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
    return isClosed() && (readmillClosingRemark != null && readmillClosingRemark.length() > 0);
  }

  public String getClosingRemark() {
    return hasClosingRemark() ? readmillClosingRemark : null;
  }

  /**
   * Sets the closed at timestamp by converting a Date object
   *
   * @param closedAt Date object of when the reading was closed
   */
  public void setClosedAt(Date closedAt) {
    locallyClosedAt = closedAt.getTime() / 1000;
  }

  /**
   * Returns the closed at timestamp as a Date object
   *
   * @return the closed at timestamp
   */
  public Date getClosedAtDate() {
    return new Date(locallyClosedAt * 1000);
  }

  /**
   * Checks if a local reading has a locally closed timestamp
   *
   * @return true if the LocalReading has a locally closed timestamp
   */
  public boolean hasClosedAt() {
    return locallyClosedAt > 0;
  }

  public void setProgressStops(final List<LocalSession> sessions) {
    if(sessions == null) {
      progressStops = new float[0];
      return;
    }

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
    parcel.writeInt(progressStops == null ? -1 : progressStops.length);
    if(progressStops != null) {
      parcel.writeFloatArray(progressStops);
    }
    parcel.writeString(title);
    parcel.writeString(author);
    parcel.writeString(coverURL);

    parcel.writeLong(totalPages);
    parcel.writeLong(currentPage);
    parcel.writeInt(measureInPercent ? 1 : 0);

    parcel.writeDouble(progress);
    parcel.writeLong(timeSpentMillis);

    parcel.writeLong(lastReadAt);
    parcel.writeLong(locallyClosedAt);
    parcel.writeInt(deletedByUser ? 1 : 0);
    parcel.writeLong(startedAt);

    parcel.writeInt(readmillRecommended ? 1 : 0);
    parcel.writeInt(readmillPrivate ? 1 : 0);
    parcel.writeLong(readmillReadingId);
    parcel.writeLong(readmillBookId);
    parcel.writeLong(readmillUserId);
    parcel.writeLong(readmillTouchedAt);
    parcel.writeInt(readmillState);
    parcel.writeString(readmillClosingRemark);
  }

  public LocalReading(Parcel parcel) {
    id = parcel.readInt();
    int numStops = parcel.readInt();
    if(numStops != -1) {
      progressStops = new float[numStops];
      parcel.readFloatArray(progressStops);
    }

    title = parcel.readString();
    author = parcel.readString();
    coverURL = parcel.readString();

    totalPages = parcel.readLong();
    currentPage = parcel.readLong();
    measureInPercent = parcel.readInt() == 1;

    progress = parcel.readDouble();
    timeSpentMillis = parcel.readLong();

    lastReadAt = parcel.readLong();
    locallyClosedAt = parcel.readLong();
    deletedByUser = parcel.readInt() == 1;
    startedAt = parcel.readLong();

    readmillRecommended = parcel.readInt() == 1;
    readmillPrivate = parcel.readInt() == 1;
    readmillReadingId = parcel.readLong();
    readmillBookId = parcel.readLong();
    readmillUserId = parcel.readLong();
    readmillTouchedAt = parcel.readLong();
    readmillState = parcel.readInt();
    readmillClosingRemark = parcel.readString();


  }

  @Override
  public int describeContents() {
    return 0;
  }
}
