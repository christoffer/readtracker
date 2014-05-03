package com.readtracker.android.googlebooks.rx;

import com.readtracker.android.googlebooks.model.ApiResponse;
import com.readtracker.android.googlebooks.model.Volume;

import com.readtracker.android.googlebooks.GoogleBooksApi;
import rx.Observable;
import rx.Subscriber;

public class SearchBooks implements Observable.OnSubscribe<ApiResponse<Volume>> {

  private final GoogleBooksApi mApi;
  private final String mQuery;

  public SearchBooks(GoogleBooksApi api, String query) {
    mApi = api;
    mQuery = query;
  }

  @Override
  public void call(Subscriber<? super ApiResponse<Volume>> subscriber) {
    try {
      subscriber.onNext(mApi.searchBooks(mQuery));
      subscriber.onCompleted();
    } catch(Exception e) {
      subscriber.onError(e);
    }
  }
}
