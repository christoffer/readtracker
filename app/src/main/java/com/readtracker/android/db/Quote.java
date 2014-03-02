package com.readtracker.android.db;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

/**
 * Represents a quote from a book.
 */
@DatabaseTable(tableName = "quotes")
public class Quote extends Model {
  @DatabaseField(columnName = Columns.CONTENT) private String mContent;
  @DatabaseField(columnName = Columns.ADD_TIMESTAMP) private Long mAddTimestamp;
  @DatabaseField(columnName = Columns.POSITION) private Float mPosition;
  @DatabaseField(
    columnName = Columns.BOOK_ID,
    foreign = true,
    canBeNull = false,
    columnDefinition = "integer references books (_id) on delete cascade")
  private Book mBook;

  public Quote() { }

  public Book getBook() { return mBook; }

  public void setBook(Book book) { mBook = book; }

  public String getContent() { return mContent; }

  public void setContent(String content) { mContent = content; }

  public Long getAddTimestamp() { return mAddTimestamp; }

  public void setAddTimestamp(Long addTimestamp) { mAddTimestamp = addTimestamp; }

  public Float getPosition() { return mPosition; }

  public void setPosition(Float position) { mPosition = position; }

  public static abstract class Columns extends Model.Columns {
    public static final String CONTENT = "content";
    public static final String ADD_TIMESTAMP = "add_timestamp";
    public static final String POSITION = "position";
    public static final String BOOK_ID = "book_id";
  }
}
