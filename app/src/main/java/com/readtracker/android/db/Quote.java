package com.readtracker.android.db;

import com.j256.ormlite.field.DatabaseField;

public class Quote {
  @DatabaseField(columnName = "book_id", foreign = true) private Book mBookId;
  @DatabaseField(columnName = "quote") private String mQuote;
  @DatabaseField(columnName = "added_at") private Long mAddedAt;
  @DatabaseField(columnName = "quote_position") private Float mQuotePosition;

  public Quote() {
  }

  public Book getBookId() { return mBookId; }

  public void setBookId(Book bookId) { mBookId = bookId; }

  public String getQuote() { return mQuote; }

  public void setQuote(String quote) { mQuote = quote; }

  public Long getAddedAt() { return mAddedAt; }

  public void setAddedAt(Long addedAt) { mAddedAt = addedAt; }

  public Float getQuotePosition() { return mQuotePosition; }

  public void setQuotePosition(Float quotePosition) { mQuotePosition = quotePosition; }
}
