package com.readtracker.android.adapters;

/**
 * Simple display of a book with title, author and cover.
 */
public class BookItem {
  public String title;
  public String author;
  public String coverURL;

  public long pageCount = -1;

  public BookItem(String title, String author, String coverURL) {
    this.title = title;
    this.author = author;
    this.coverURL = coverURL;
  }
}
