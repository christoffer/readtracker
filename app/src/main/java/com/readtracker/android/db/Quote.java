package com.readtracker.android.db;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import com.readtracker.android.support.Utils;

import java.util.Arrays;

/**
 * Represents a quote from a book.
 */
@DatabaseTable(tableName = "quotes")
public class Quote extends Model {

  /* Database fields */

  @DatabaseField(columnName = Columns.CONTENT)
  private String mContent;

  @DatabaseField(columnName = Columns.ADD_TIMESTAMP)
  private Long mAddTimestampMs;

  @DatabaseField(columnName = Columns.POSITION)
  private Float mPosition;

  @DatabaseField(
      columnName = Columns.BOOK_ID,
      foreign = true,
      canBeNull = false,
      columnDefinition = "integer references books (_id) on delete cascade")
  private Book mBook;

  /* End database fields */

  public Quote() { }

  public Book getBook() { return mBook; }

  public void setBook(Book book) { mBook = book; }

  public String getContent() { return mContent; }

  public void setContent(String content) { mContent = content; }

  public Long getAddTimestampMs() { return mAddTimestampMs; }

  public void setAddTimestampMs(Long addTimestampMs) { mAddTimestampMs = addTimestampMs; }

  public Float getPosition() { return mPosition; }

  public void setPosition(Float position) { mPosition = position; }

  public static abstract class Columns extends Model.Columns {
    public static final String CONTENT = "content";
    public static final String ADD_TIMESTAMP = "add_timestamp";
    public static final String POSITION = "position";
    public static final String BOOK_ID = "book_id";
  }

  @Override public boolean equals(Object o) {
    if(this == o) return true;
    if(o instanceof Quote) {
      final Quote other = (Quote) o;

      return Utils.equal(getBook(), other.getBook())
          && Utils.equal(getContent(), other.getContent())
          && Utils.equal(getAddTimestampMs(), other.getAddTimestampMs());
    }
    return false;
  }

  @Override public int hashCode() {
    return Arrays.hashCode(new Object[]{getContent(), getBook(), getAddTimestampMs()});
  }
}
