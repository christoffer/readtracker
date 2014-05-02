package com.readtracker.android.googlebooks;

import java.util.List;

import com.readtracker.android.googlebooks.model.Volume;
import retrofit.http.GET;
import retrofit.http.Query;

/**
 * Refer to https://developers.google.com/books/docs/v1/using
 */
public interface GoogleBooksApi {
    @GET("/books/v1/volumes")
    public List<Volume> findBooks(@Query("q") String query);
}
