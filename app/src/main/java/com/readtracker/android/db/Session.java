package com.readtracker.android.db;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import com.readtracker.android.support.Utils;

import java.util.Arrays;

/**
 * Represents one reading session.
 */
@DatabaseTable(tableName = "sessions")
public class Session extends Model {

  /* Database fields */

  @DatabaseField(columnName = Columns.START_POSITION)
  private float mStartPosition;

  @DatabaseField(columnName = Columns.END_POSITION)
  private float mEndPosition;

  @DatabaseField(columnName = Columns.DURATION_SECONDS)
  private long mDurationSeconds;

  @DatabaseField(columnName = Columns.TIMESTAMP)
  private long mTimestampMs;

  @DatabaseField(
      columnName = Columns.BOOK_ID,
      foreign = true,
      canBeNull = false,
      columnDefinition = "integer references books (_id) on delete cascade")
  private Book mBook;

  /* End database fields. */

  public Session() {
  }

  public Float getStartPosition() { return mStartPosition; }

  public void setStartPosition(float startPosition) { mStartPosition = startPosition; }

  public float getEndPosition() { return mEndPosition; }

  public void setEndPosition(float endPosition) { mEndPosition = endPosition; }

  public Long getDurationSeconds() { return mDurationSeconds; }

  public void setDurationSeconds(long durationSeconds) { mDurationSeconds = durationSeconds; }

  public Long getTimestampMs() { return mTimestampMs; }

  public void setTimestampMs(long timestampMs) { mTimestampMs = timestampMs; }

  public Book getBook() { return mBook; }

  public void setBook(Book book) { mBook = book; }

  public static abstract class Columns extends Model.Columns {
    public static final String START_POSITION = "start_position";
    public static final String END_POSITION = "end_position";
    public static final String DURATION_SECONDS = "duration_seconds";
    public static final String TIMESTAMP = "timestamp";
    public static final String BOOK_ID = "book_id";
  }

  @Override public boolean equals(Object o) {
    if(this == o) return true;
    if(o instanceof Session) {
      final Session other = (Session) o;
      return Utils.equal(getBook(), other.getBook())
          && Utils.equal(getStartPosition(), other.getStartPosition())
          && Utils.equal(getEndPosition(), other.getEndPosition())
          && Utils.equal(getDurationSeconds(), other.getDurationSeconds())
          && Utils.equal(getTimestampMs(), other.getTimestampMs());
    }
    return false;
  }

  @Override public int hashCode() {
    return Arrays.hashCode(new Object[]{getBook(), getStartPosition(), getEndPosition(), getDurationSeconds(), getTimestampMs()});
  }
}
