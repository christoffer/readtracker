package com.readtracker_beta.list_adapters;

import com.readtracker_beta.support.GoogleBook;

/**
 * Shows a GoogleBook search result
 */
public class ListItemGoogleBook extends ListItemBook {
  public GoogleBook googleBook = null;

  public ListItemGoogleBook(String title, String author) {
    super(title, author, null);
  }

  public ListItemGoogleBook(GoogleBook googleBook) {
    this(googleBook.getTitle(), googleBook.getAuthor());
    this.googleBook = googleBook;
    coverURL = googleBook.getCoverURL();
    this.pageCount = googleBook.getPageCount();
  }
}
