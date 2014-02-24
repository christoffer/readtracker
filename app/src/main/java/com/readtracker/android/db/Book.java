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
  @DatabaseField(columnName = Columns.STATE, dataType = DataType.ENUM_STRING) State mState = State.Reading;
  @DatabaseField(columnName = Columns.LAST_POSITION) Float mCurrentPosition;
  @DatabaseField(columnName = Columns.LAST_OPENED_AT) Long mLastOpenedAt;
  @DatabaseField(columnName = Columns.FIRST_POSITION_AT) Long mFirstPositionAt;
  @DatabaseField(columnName = Columns.CLOSING_REMARK) String mClosingRemark;

  // Use manual handling of foreign keys here as we want to have complete
  // control over when and where these are loaded.
  private List<Session> mSessions = new ArrayList<Session>();
  private List<Quote> mQuotes = new ArrayList<Quote>();

  public Book() { }

  /** Load all sessions for this book from the database. */
  public void loadSessions(DatabaseManager databaseManager) {
    mSessions = databaseManager.getSessionsForBook(this);
  }

  public List<Session> getSessions() {
    return mSessions;
  }

  public void loadQuotes(DatabaseManager databaseManager) {
    mQuotes = databaseManager.getQuotesForBook(this);
  }

  public List<Quote> getQuotes() {
    return mQuotes;
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

  public boolean hasState(State state) {
    return mState != null && mState.equals(state);
  }

  public Float getCurrentPosition() { return mCurrentPosition; }

  public void setCurrentPosition(Float currentPosition) { mCurrentPosition = currentPosition; }

  public Long getLastOpenedAt() { return mLastOpenedAt; }

  public void setLastOpenedAt(Long lastOpenedAt) { mLastOpenedAt = lastOpenedAt; }

  public Long getFirstPositionAt() { return mFirstPositionAt; }

  public void setFirstPositionAt(Long firstPositionAt) { mFirstPositionAt = firstPositionAt; }

  public String getClosingRemark() { return mClosingRemark; }

  public void setClosingRemark(String closingRemark) { mClosingRemark = closingRemark; }

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

  /** Returns true if this book has page numbers set. */
  public boolean hasPageNumbers() {
    return mNumberPages != null && mNumberPages > 0;
  }

  /** Returns true if this book has a current position set. */
  public boolean hasCurrentPosition() {
    return mCurrentPosition != null && mCurrentPosition > 0.0;
  }

  /** Returns the page number if the user is reading a book with total pages set. Otherwise returns current position in %. */
  public String getCurrentPageName() {
    if(hasPageNumbers() && hasCurrentPosition()) {
      return String.format("%d", Math.round(mNumberPages * mCurrentPosition));
    } else if(hasCurrentPosition()) {
      return String.format("%.2f%%", mCurrentPosition * 100);
    } else {
      return "0%";
    }
  }

  /** Returns the numerical value of the current page if the book has a position and total page count. */
  public int getCurrentPage() {
    if(mCurrentPosition == null || mNumberPages == null) {
      return 0;
    }

    return (int) (mCurrentPosition * mNumberPages);
  }

  /** Returns the sum of all loaded sessions. */
  public long calculateSecondsSpent() {
    long totalDuration = 0;

    for(Session session: mSessions) {
      totalDuration += session.getDurationSeconds();
    }

    return totalDuration;
  }

  /** Returns an estimated time left, based on the progress and time of all loaded sessions. */
  public int calculateEstimatedSecondsLeft() {
    if(mState != State.Reading || mCurrentPosition <= 0.0 || mCurrentPosition >= 1.0) {
      return 0;
    }

    final long secondsSpent = calculateSecondsSpent();
    final float secondsPerPosition = secondsSpent / mCurrentPosition; // TODO use start-end for sessions for accuracy
    final float positionsToRead = 1.0f - mCurrentPosition;

    return (int) (positionsToRead * secondsPerPosition);
  }

}
