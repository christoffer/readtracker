package com.readtracker.android.db;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

/**
 * Represents a book in the users list of books that are being read.
 */
@DatabaseTable(tableName = "books")
public class Book {
  @DatabaseField(columnName = "title") private String mTitle;
  @DatabaseField(columnName = "author") private String mAuthor;
  @DatabaseField(columnName = "cover_url") private String mCoverUrl;
  @DatabaseField(columnName = "number_pages") private Float mNumberPages;
  @DatabaseField(columnName = "current_position") private Float mCurrentPosition;
  @DatabaseField(columnName = "highest_position") private Float mHighestPosition;
  @DatabaseField(columnName = "last_opened_at") private Long mLastOpenedAt;
  @DatabaseField(columnName = "first_position_at") private Long mFirstPositionAt;
  @DatabaseField(columnName = "closing_remark") private String mClosingRemark;

  public Book() {
  }

  public String getTitle() { return mTitle; }

  public void setTitle(String title) { mTitle = title; }

  public String getAuthor() { return mAuthor; }

  public void setAuthor(String author) { mAuthor = author; }

  public String getCoverUrl() { return mCoverUrl; }

  public void setCoverUrl(String coverUrl) { mCoverUrl = coverUrl; }

  public Float getNumberPages() { return mNumberPages; }

  public void setNumberPages(Float numberPages) { mNumberPages = numberPages; }

  public Float getCurrentPosition() { return mCurrentPosition; }

  public void setCurrentPosition(Float currentPosition) { mCurrentPosition = currentPosition; }

  public Float getHighestPosition() { return mHighestPosition; }

  public void setHighestPosition(Float highestPosition) { mHighestPosition = highestPosition; }

  public Long getLastOpenedAt() { return mLastOpenedAt; }

  public void setLastOpenedAt(Long lastOpenedAt) { mLastOpenedAt = lastOpenedAt; }

  public Long getFirstPositionAt() { return mFirstPositionAt; }

  public void setFirstPositionAt(Long firstPositionAt) { mFirstPositionAt = firstPositionAt; }

  public String getClosingRemark() { return mClosingRemark; }

  public void setClosingRemark(String closingRemark) { mClosingRemark = closingRemark; }
}
