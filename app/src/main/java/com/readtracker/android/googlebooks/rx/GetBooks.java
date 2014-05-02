package com.readtracker.android.googlebooks.rx;

import com.readtracker.android.googlebooks.model.Volume;

import java.util.List;

import com.readtracker.android.googlebooks.GoogleBooksApi;
import rx.Observable;
import rx.Subscriber;

public class GetBooks implements Observable.OnSubscribe<List<Volume>> {

  private static final String TAG = GetBooks.class.getSimpleName();

  private final GoogleBooksApi mApi;
  private final String mQuery;

  public GetBooks(GoogleBooksApi api, String query) {
    mApi = api;
    mQuery = query;
  }

  @Override
  public void call(Subscriber<? super List<Volume>> subscriber) {
    try {
      subscriber.onNext(mApi.findBooks(mQuery));
      subscriber.onCompleted();
    } catch(Exception e) {
      subscriber.onError(e);
    }
  }
}
