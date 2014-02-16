package com.readtracker.android.db;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

/**
 * Represents one reading session.
 */
@DatabaseTable(tableName = "sessions")
public class Session extends Model {

  @DatabaseField(columnName = Columns.START_POSITION) private Float mStartPosition;
  @DatabaseField(columnName = Columns.END_POSITION) private Float mEndPosition;
  @DatabaseField(columnName = Columns.DURATION_SECONDS) private Long mDurationSeconds;
  @DatabaseField(columnName = Columns.STARTED_AT) private Long mStartedAt;
  @DatabaseField(columnName = Columns.BOOK_ID, foreign = true,
    columnDefinition = "integer references books (_id) on delete cascade") private Book mBook;

  public Session() {
  }

  public Float getStartPosition() { return mStartPosition; }

  public void setStartPosition(Float startPosition) { mStartPosition = startPosition; }

  public Float getEndPosition() { return mEndPosition; }

  public void setEndPosition(Float endPosition) { mEndPosition = endPosition; }

  public Long getDurationSeconds() { return mDurationSeconds; }

  public void setDurationSeconds(Long durationSeconds) { mDurationSeconds = durationSeconds; }

  public Long getStartedAt() { return mStartedAt; }

  public void setStartedAt(Long startedAt) { mStartedAt = startedAt; }

  public Book getBook() { return mBook; }

  public void setBook(Book book) { mBook = book; }

  public static abstract class Columns extends Model.Columns {
    public static final String START_POSITION = "start_position";
    public static final String END_POSITION = "end_position";
    public static final String DURATION_SECONDS = "duration_seconds";
    public static final String STARTED_AT = "started_at";
    public static final String BOOK_ID = "book_id";
  }
}
