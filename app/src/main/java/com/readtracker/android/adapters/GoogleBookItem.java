package com.readtracker.android.adapters;

import com.readtracker.android.support.GoogleBook;

/**
 * Shows a GoogleBook search result
 */
public class GoogleBookItem extends BookItem {
  public GoogleBookItem(String title, String author) {
    super(title, author, null);
  }

  public GoogleBookItem(GoogleBook googleBook) {
    this(googleBook.getTitle(), googleBook.getAuthor());
    coverURL = googleBook.getCoverURL();
    this.pageCount = googleBook.getPageCount();
  }
}
