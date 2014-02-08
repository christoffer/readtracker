package com.readtracker.android.db;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

/**
 * Represents one reading session.
 */
@DatabaseTable(tableName = "sessions")
public class Session extends Model {
  @DatabaseField(columnName = "start_position") private Float mStartPosition;
  @DatabaseField(columnName = "end_position") private Float mEndPosition;
  @DatabaseField(columnName = "duration_seconds") private Long mDurationSeconds;
  @DatabaseField(columnName = "started_at") private Long mStartedAt;
  @DatabaseField(
    columnName = "book_id",
    foreign = true,
    columnDefinition = "integer references books (_id) on delete cascade"
  ) private Book mBook;

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
}
