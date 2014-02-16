package com.readtracker.android.db;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a book in the users list of books that are being read.
 */
@DatabaseTable(tableName = "books")
public class Book extends Model {
  public static enum State {Unknown, Finished, Reading}

  @DatabaseField(columnName = Columns.TITLE) String mTitle;
  @DatabaseField(columnName = Columns.AUTHOR) String mAuthor;
  @DatabaseField(columnName = Columns.COVER_URL) String mCoverUrl;
  @DatabaseField(columnName = Columns.NUMBER_PAGES) Float mNumberPages;
  @DatabaseField(columnName = Columns.STATE, dataType = DataType.ENUM_STRING) State mState;
  @DatabaseField(columnName = Columns.LAST_POSITION) Float mCurrentPosition;
  @DatabaseField(columnName = Columns.LAST_OPENED_AT) Long mLastOpenedAt;
  @DatabaseField(columnName = Columns.FIRST_POSITION_AT) Long mFirstPositionAt;
  @DatabaseField(columnName = Columns.CLOSING_REMARK) String mClosingRemark;

  // Cached list of sessions. Avoid using a foreign collection since we want complete
  // control over when this is loaded from the database.
  private List<Session> mSessions = new ArrayList<Session>();

  public Book() { }

  /** Load all sessions for this book from the database. */
  public void loadSessions(DatabaseManager databaseManager) {
    mSessions = databaseManager.getSessionsForBook(this);
  }

  @Override public boolean equals(Object o) {
    if(this == o) return true;
    if(o instanceof Book) {
      final Book other = (Book) o;
      return this.getTitle().equals(other.getTitle()) && (this.getAuthor().equals(other.getAuthor()));
    }
    return false;
  }

  @Override public int hashCode() {
    return (getTitle() + getAuthor()).hashCode();
  }

  public String getTitle() { return mTitle == null ? "" : mTitle; }

  public void setTitle(String title) { mTitle = title; }

  public String getAuthor() { return mAuthor == null ? "" : mAuthor; }

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

  public List<Session> getSessions() {
    return mSessions;
  }

  public static abstract class Columns extends Model.Columns {
    public static final String TITLE = "title";
    public static final String AUTHOR = "author";
    public static final String COVER_URL = "cover_url";
    public static final String NUMBER_PAGES = "number_pages";
    public static final String STATE = "state";
    public static final String LAST_POSITION = "last_position";
    public static final String LAST_OPENED_AT = "last_opened_at";
    public static final String FIRST_POSITION_AT = "first_position_at";
    public static final String CLOSING_REMARK = "closing_remark";
  }
}
