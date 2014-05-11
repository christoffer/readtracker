package com.readtracker.android.googlebooks;

import com.readtracker.android.googlebooks.model.ApiResponse;
import com.readtracker.android.googlebooks.model.Volume;
import com.readtracker.android.googlebooks.rx.SearchBooks;

import rx.Observable;

public class GoogleBooksClient {

  public static Observable<ApiResponse<Volume>> searchBooks(GoogleBooksApi api, String query) {
    return Observable.create(new SearchBooks(api, query));
  }
}
