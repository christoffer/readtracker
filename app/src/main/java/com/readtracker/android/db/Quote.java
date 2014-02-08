package com.readtracker.android.db;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

/**
 * Represents a quote from a book.
 */
@DatabaseTable(tableName = "quotes")
public class Quote extends Model {
  @DatabaseField(columnName = "quote") private String mQuote;
  @DatabaseField(columnName = "added_at") private Long mAddedAt;
  @DatabaseField(columnName = "quote_position") private Float mQuotePosition;
  @DatabaseField(
    columnName = "book_id",
    foreign = true,
    columnDefinition = "integer references books (_id) on delete cascade"
  ) private Book mBook;

  public Quote() {
  }

  public Book getBook() { return mBook; }

  public void setBook(Book book) { mBook = book; }

  public String getQuote() { return mQuote; }

  public void setQuote(String quote) { mQuote = quote; }

  public Long getAddedAt() { return mAddedAt; }

  public void setAddedAt(Long addedAt) { mAddedAt = addedAt; }

  public Float getQuotePosition() { return mQuotePosition; }

  public void setQuotePosition(Float quotePosition) { mQuotePosition = quotePosition; }
}
