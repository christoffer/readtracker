package com.readtracker.android.googlebooks;

import android.net.Uri;

import retrofit.RestAdapter;

public class ApiProvider {

  public static GoogleBooksApi provideGoogleBooksApi() {
    String apiUrl = new Uri.Builder().scheme("https").authority("www.googleapis.com").toString();
    RestAdapter restAdapter = new RestAdapter.Builder()
        .setEndpoint(apiUrl)
        .setLogLevel(RestAdapter.LogLevel.FULL)
        .build();

    return restAdapter.create(GoogleBooksApi.class);
  }
}
