package com.readtracker.android.db;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

/**
 * Represents a book in the users list of books that are being read.
 */
@DatabaseTable(tableName = "books")
public class Book extends Model {
  public static enum State { Uknown, Finished, Reading}

  @DatabaseField(columnName = "title") private String mTitle;
  @DatabaseField(columnName = "author") private String mAuthor;
  @DatabaseField(columnName = "cover_url") private String mCoverUrl;
  @DatabaseField(columnName = "number_pages") private Float mNumberPages;
  @DatabaseField(columnName = "state", dataType = DataType.ENUM_STRING) private State mState;
  @DatabaseField(columnName = "last_position") private Float mCurrentPosition;
  @DatabaseField(columnName = "last_opened_at") private Long mLastOpenedAt;
  @DatabaseField(columnName = "first_position_at") private Long mFirstPositionAt;
  @DatabaseField(columnName = "closing_remark") private String mClosingRemark;

  public Book() { }

  /** Load all sessions for this book from the database. */
  public void loadSessions(DatabaseManager databaseManager) {
    // TODO
  }

  public String getTitle() { return mTitle; }

  public void setTitle(String title) { mTitle = title; }

  public String getAuthor() { return mAuthor; }

  public void setAuthor(String author) { mAuthor = author; }

  public String getCoverUrl() { return mCoverUrl; }

  public void setCoverUrl(String coverUrl) { mCoverUrl = coverUrl; }

  public Float getNumberPages() { return mNumberPages; }

  public void setNumberPages(Float numberPages) { mNumberPages = numberPages; }

  public State getState() { return mState; }

  public void setState(State state) { mState = state; }

  public Float getCurrentPosition() { return mCurrentPosition; }

  public void setCurrentPosition(Float currentPosition) { mCurrentPosition = currentPosition; }

  public Long getLastOpenedAt() { return mLastOpenedAt; }

  public void setLastOpenedAt(Long lastOpenedAt) { mLastOpenedAt = lastOpenedAt; }

  public Long getFirstPositionAt() { return mFirstPositionAt; }

  public void setFirstPositionAt(Long firstPositionAt) { mFirstPositionAt = firstPositionAt; }

  public String getClosingRemark() { return mClosingRemark; }

  public void setClosingRemark(String closingRemark) { mClosingRemark = closingRemark; }
}
