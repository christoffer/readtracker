package com.readtracker.android.events;

import com.readtracker.android.db.Book;

public class BookUpdatedEvent {
  private final Book mBook;

  public BookUpdatedEvent(Book book) {
    mBook = book;
  }

  public Book getBook() {
    return mBook;
  }
}
