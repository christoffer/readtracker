package com.readtracker.android.db;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import com.readtracker.android.support.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Represents a book in the users list of books that are being read.
 */
@DatabaseTable(tableName = "books")
public class Book extends Model {

  public static enum State {Unknown, Finished, Reading}

  /* Database fields */

  @DatabaseField(columnName = Columns.TITLE)
  private String mTitle;

  @DatabaseField(columnName = Columns.AUTHOR)
  private String mAuthor;

  @DatabaseField(columnName = Columns.COVER_IMAGE_URL)
  private String mCoverImageUrl;

  @DatabaseField(columnName = Columns.PAGE_COUNT)
  private Float mPageCount;

  @DatabaseField(columnName = Columns.STATE, dataType = DataType.ENUM_STRING)
  private State mState = State.Reading;

  @DatabaseField(columnName = Columns.CURRENT_POSITION)
  private Float mCurrentPosition;

  @DatabaseField(columnName = Columns.CURRENT_POSITION_TIMESTAMP)
  private Long mCurrentPositionTimestampMs;

  @DatabaseField(columnName = Columns.FIRST_POSITION_TIMESTAMP)
  private Long mFirstPositionTimestampMs;

  @DatabaseField(columnName = Columns.CLOSING_REMARK)
  private String mClosingRemark;

  /* End database fields */

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
      return Utils.equal(getTitle(), other.getTitle())
          && Utils.equal(getAuthor(), other.getAuthor());
    }
    return false;
  }

  @Override public int hashCode() {
    return Arrays.hashCode(new Object[]{getTitle(), getAuthor()});
  }

  public String getTitle() { return mTitle == null ? "" : mTitle; }

  public void setTitle(String title) { mTitle = title; }

  public String getAuthor() { return mAuthor == null ? "" : mAuthor; }

  public void setAuthor(String author) { mAuthor = author; }

  public String getCoverImageUrl() { return mCoverImageUrl; }

  public void setCoverImageUrl(String coverImageUrl) { mCoverImageUrl = coverImageUrl; }

  public Float getPageCount() { return mPageCount; }

  public void setPageCount(Float pageCount) { mPageCount = pageCount; }

  public State getState() { return mState; }

  public void setState(State state) { mState = state; }

  public boolean isInState(State state) {
    return mState != null && mState.equals(state);
  }

  public float getCurrentPosition() { return mCurrentPosition == null ? 0f : mCurrentPosition; }

  public void setCurrentPosition(float currentPosition) { mCurrentPosition = currentPosition; }

  public Long getCurrentPositionTimestampMs() { return mCurrentPositionTimestampMs; }

  public void setCurrentPositionTimestampMs(Long currentPositionTimestampMs) { mCurrentPositionTimestampMs = currentPositionTimestampMs; }

  public Long getFirstPositionTimestampMs() { return mFirstPositionTimestampMs; }

  public void setFirstPositionTimestampMs(Long firstPositionTimestampMs) { mFirstPositionTimestampMs = firstPositionTimestampMs; }

  public String getClosingRemark() { return mClosingRemark; }

  public void setClosingRemark(String closingRemark) { mClosingRemark = closingRemark; }

  public static abstract class Columns extends Model.Columns {
    public static final String TITLE = "title";
    public static final String AUTHOR = "author";
    public static final String COVER_IMAGE_URL = "cover_image_url";
    public static final String PAGE_COUNT = "page_count";
    public static final String STATE = "state";
    public static final String CURRENT_POSITION = "current_position";
    public static final String CURRENT_POSITION_TIMESTAMP = "current_position_timestamp";
    public static final String FIRST_POSITION_TIMESTAMP = "first_position_timestamp";
    public static final String CLOSING_REMARK = "closing_remark";
  }

  /** Returns true if this book has page numbers set. */
  public boolean hasPageNumbers() {
    return mPageCount != null && mPageCount > 0;
  }

  /** Returns true if this book has a current position set. */
  public boolean hasCurrentPosition() {
    return mCurrentPosition != null && mCurrentPosition > 0.0;
  }

  /** Returns the page number if the user is reading a book with total pages set. Otherwise returns current position in %. */
  public String getCurrentPageName() {
    if(hasPageNumbers() && hasCurrentPosition()) {
      return String.format("%d", Math.round(mPageCount * mCurrentPosition));
    } else if(hasCurrentPosition()) {
      return String.format("%.2f%%", mCurrentPosition * 100);
    } else {
      return "0%";
    }
  }

  /** Returns the numerical value of the current page if the book has a position and total page count. */
  public int getCurrentPage() {
    if(mCurrentPosition == null || mPageCount == null) {
      return 0;
    }

    return (int) (mCurrentPosition * mPageCount);
  }

  /** Returns the sum of all loaded sessions. */
  public long calculateSecondsSpent() {
    long totalDuration = 0;

    for(Session session : mSessions) {
      totalDuration += session.getDurationSeconds();
    }

    return totalDuration;
  }

  /** Returns an estimated time left, based on the progress and time of all loaded sessions. */
  public int calculateEstimatedSecondsLeft() {
    if(mState != State.Reading || getCurrentPosition() <= 0.0 || getCurrentPosition() >= 1.0) {
      return 0;
    }

    final long secondsSpent = calculateSecondsSpent();
    final float secondsPerPosition = secondsSpent / getCurrentPosition(); // TODO use start-end for sessions for accuracy
    final float positionsToRead = 1.0f - getCurrentPosition();

    return (int) (positionsToRead * secondsPerPosition);
  }

}
