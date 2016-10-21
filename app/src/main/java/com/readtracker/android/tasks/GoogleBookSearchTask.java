package com.readtracker.android.tasks;

import android.os.AsyncTask;

import com.readtracker.android.support.GoogleBook;
import com.readtracker.android.support.GoogleBookSearch;
import com.readtracker.android.support.GoogleBookSearchException;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

/**
 * Background task for performing a Google Book search.
 */
public class GoogleBookSearchTask extends AsyncTask<String, Void, ArrayList<GoogleBook>> {
  private final WeakReference<BookSearchResultListener> mListener;

  private GoogleBookSearchTask(BookSearchResultListener listener) {
    mListener = new WeakReference<>(listener);
  }

  public static void search(String query, BookSearchResultListener resultListener) {
    (new GoogleBookSearchTask(resultListener)).execute(query);
  }

  protected void onPostExecute(ArrayList<GoogleBook> foundBooks) {
    final BookSearchResultListener listener = mListener.get();
    if(listener != null) {
      listener.onSearchResultsRetrieved(foundBooks);
    }
  }

  @Override
  protected ArrayList<GoogleBook> doInBackground(String... searchWords) {
    try {
      return GoogleBookSearch.search(searchWords[0]);
    } catch(GoogleBookSearchException e) {
      return null;
    }
  }

  /**
   * Result listener for doing book searches using the Google Books API.
   */
  public interface BookSearchResultListener {
    /**
     * Called when the results from a search returns.
     *
     * @param result A list of all the GoogleBooks found, or null if the search failed.
     */
    void onSearchResultsRetrieved(ArrayList<GoogleBook> result);
  }
}
