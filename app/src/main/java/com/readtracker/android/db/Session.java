package com.readtracker.android.db;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

/**
 * Represents one reading session.
 */
@DatabaseTable(tableName = "sessions")
public class Session extends Model {

  @DatabaseField(columnName = Columns.START_POSITION) private float mStartPosition;
  @DatabaseField(columnName = Columns.END_POSITION) private float mEndPosition;
  @DatabaseField(columnName = Columns.DURATION_SECONDS) private long mDurationSeconds;
  @DatabaseField(columnName = Columns.TIMESTAMP) private long mTimestamp;
  @DatabaseField(
    columnName = Columns.BOOK_ID,
    foreign = true,
    canBeNull = false,
    columnDefinition = "integer references books (_id) on delete cascade")
  private Book mBook;

  public Session() {
  }

  public Float getStartPosition() { return mStartPosition; }

  public void setStartPosition(float startPosition) { mStartPosition = startPosition; }

  public float getEndPosition() { return mEndPosition; }

  public void setEndPosition(float endPosition) { mEndPosition = endPosition; }

  public Long getDurationSeconds() { return mDurationSeconds; }

  public void setDurationSeconds(long durationSeconds) { mDurationSeconds = durationSeconds; }

  public Long getTimestamp() { return mTimestamp; }

  public void setTimestamp(long timestamp) { mTimestamp = timestamp; }

  public Book getBook() { return mBook; }

  public void setBook(Book book) { mBook = book; }

  public static abstract class Columns extends Model.Columns {
    public static final String START_POSITION = "start_position";
    public static final String END_POSITION = "end_position";
    public static final String DURATION_SECONDS = "duration_seconds";
    public static final String TIMESTAMP = "timestamp";
    public static final String BOOK_ID = "book_id";
  }
}
