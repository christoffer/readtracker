package com.readtracker.android.db;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

/**
 * Represents a quote from a book.
 */
@DatabaseTable(tableName = "quotes")
public class Quote extends Model {
  @DatabaseField(columnName = "content") String mContent;
  @DatabaseField(columnName = "added_at") Long mAddedAt;
  @DatabaseField(columnName = "position") Float mPosition;
  @DatabaseField(
    columnName = "book_id",
    foreign = true,
    columnDefinition = "integer references books (_id) on delete cascade"
  ) Book mBook;

  public Quote() {
  }

  public Book getBook() { return mBook; }

  public void setBook(Book book) { mBook = book; }

  public String getContent() { return mContent; }

  public void setContent(String content) { mContent = content; }

  public Long getAddedAt() { return mAddedAt; }

  public void setAddedAt(Long addedAt) { mAddedAt = addedAt; }

  public Float getPosition() { return mPosition; }

  public void setPosition(Float position) { mPosition = position; }
}
