package com.readtracker.android.googlebooks;

import java.util.List;

import com.readtracker.android.googlebooks.model.Volume;
import com.readtracker.android.googlebooks.rx.SearchBooks;

import rx.Observable;

public class GoogleBooksClient {

  public static Observable<List<Volume>> findBooks(GoogleBooksApi api, String query) {
    return Observable.create(new SearchBooks(api, query));
  }
}
