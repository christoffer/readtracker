package com.readtracker.android.db;

import android.graphics.Color;
import android.os.Parcel;
import android.os.Parcelable;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;

/**
 * A local reading stored on the device. May, or may not, be connected to a
 * remote (Readmill) reading.
 * <p/>
 * Stores additional information that is not necessarily available on the remote
 * reading (like page numbers).
 */
public class LocalReading implements Parcelable {

  public static enum ReadingState {
    UNKNOWN, INTERESTING, READING, FINISHED, ABANDONED
  }

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

  public static final String UPDATED_AT_FIELD_NAME = "updated_at";

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
  protected long lastReadAt = 0;

  @DatabaseField(columnName = LOCALLY_CLOSED_AT_FIELD_NAME)
  protected long locallyClosedAt;

  @DatabaseField(columnName = UPDATED_AT_FIELD_NAME)
  protected long updatedAt;

  @DatabaseField(columnName = DELETED_BY_USER_FIELD_NAME)
  public boolean deletedByUser = false;

  @DatabaseField(columnName = STARTED_AT_FIELD_NAME)
  protected long startedAt = 0;

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
  protected long readmillTouchedAt = 0;

  @DatabaseField(columnName = READMILL_STATE_FIELD_NAME, dataType = DataType.ENUM_INTEGER)
  public ReadingState readmillState = ReadingState.READING;

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

  private long getTimeSpentSeconds() {
    return timeSpentMillis / 1000;
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
    return String.format("LocalReading #%d \"%s\" by \"%s\" - Page (%d/%d) over %d seconds - Readmill Reading #%s, Book #%s, User #%s State %s Closing Remark '%s' - Cover '%s'", id, title, author, currentPage, totalPages, getTimeSpentSeconds(), readmillReadingId, readmillBookId, readmillUserId, readmillState, readmillClosingRemark, coverURL);
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
    parcel.writeString(readmillState.toString());
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
    readmillState = ReadingState.valueOf(parcel.readString());
    readmillClosingRemark = parcel.readString();
  }

  @Override
  public int describeContents() {
    return 0;
  }
}
