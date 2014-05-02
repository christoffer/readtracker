package com.readtracker.android.adapters;

/**
 * Simple display of a book with title, author and cover.
 */
public class BookItem {
  public String title;
  public String author;
  public String coverURL;
  public long pageCount;

  public BookItem(String title, String author, String coverURL, long pageCount) {
    this.title = title;
    this.author = author;
    this.coverURL = coverURL;
    this.pageCount = pageCount;
  }
}
