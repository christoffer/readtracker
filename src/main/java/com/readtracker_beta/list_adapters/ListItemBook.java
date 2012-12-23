package com.readtracker_beta.list_adapters;

/**
 * Simple display of a book with title, author and cover.
 */
public class ListItemBook {
  public String title = "";
  public String author = "";
  public String coverURL = null;

  public long pageCount = -1;

  public ListItemBook(String title, String author, String coverURL) {
    this.title = title;
    this.author = author;
    this.coverURL = coverURL;
  }

  public ListItemBook(String title, String author, String coverURL, long pageCount) {
    this(title, author, coverURL);
    this.pageCount = pageCount;
  }
}
