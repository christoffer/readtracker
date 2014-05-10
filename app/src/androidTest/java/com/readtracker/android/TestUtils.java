package com.readtracker.android;

import com.readtracker.android.db.Book;

public class TestUtils {
  private static String uniqueString(String string) {
    final long number = (long) Math.random() * 100000;
    return String.format("%s-%6d", string, number);
  }

  /** Returns a Book with a random title and author. */
  public static Book buildBook() {
    Book book = new Book();
    book.setTitle(uniqueString("Title"));
    book.setAuthor(uniqueString("Author"));
    return book;
  }
}
