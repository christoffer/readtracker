package com.readtracker_beta.adapters;

import com.readtracker_beta.support.GoogleBook;

/**
 * Shows a GoogleBook search result
 */
public class GoogleBookItem extends BookItem {
  public GoogleBook googleBook = null;

  public GoogleBookItem(String title, String author) {
    super(title, author, null);
  }

  public GoogleBookItem(GoogleBook googleBook) {
    this(googleBook.getTitle(), googleBook.getAuthor());
    this.googleBook = googleBook;
    coverURL = googleBook.getCoverURL();
    this.pageCount = googleBook.getPageCount();
  }
}
