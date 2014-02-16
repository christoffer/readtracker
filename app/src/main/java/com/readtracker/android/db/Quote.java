package com.readtracker.android.db;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

/**
 * Represents a quote from a book.
 */
@DatabaseTable(tableName = "quotes")
public class Quote extends Model {
  @DatabaseField(columnName = Columns.CONTENT) String mContent;
  @DatabaseField(columnName = Columns.ADDED_AT) Long mAddedAt;
  @DatabaseField(columnName = Columns.POSITION) Float mPosition;
  @DatabaseField(columnName = Columns.BOOK_ID, foreign = true,
    columnDefinition = "integer references books (_id) on delete cascade") Book mBook;

  public Quote() { }

  public Book getBook() { return mBook; }

  public void setBook(Book book) { mBook = book; }

  public String getContent() { return mContent; }

  public void setContent(String content) { mContent = content; }

  public Long getAddedAt() { return mAddedAt; }

  public void setAddedAt(Long addedAt) { mAddedAt = addedAt; }

  public Float getPosition() { return mPosition; }

  public void setPosition(Float position) { mPosition = position; }

  public static abstract class Columns extends Model.Columns {
    public static final String CONTENT = "content";
    public static final String ADDED_AT = "added_at";
    public static final String POSITION = "position";
    public static final String BOOK_ID = "book_id";
  }
}
