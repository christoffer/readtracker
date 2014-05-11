package com.readtracker.android.adapters;

/**
 * Simple display of a book with title, author and cover.
 */
public class BookItem {
  public final String title;
  public final String author;
  public final String coverURL;
  public final long pageCount;

  public BookItem(String title, String author, String coverURL, long pageCount) {
    this.title = title;
    this.author = author;
    this.coverURL = coverURL;
    this.pageCount = pageCount;
  }
}
