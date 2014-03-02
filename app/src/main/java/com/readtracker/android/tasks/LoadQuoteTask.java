package com.readtracker.android.tasks;

import android.os.AsyncTask;

import com.readtracker.android.db.DatabaseManager;
import com.readtracker.android.db.Quote;
import com.squareup.otto.Bus;

public class LoadQuoteTask extends AsyncTask<Void, Void, Quote> {
  private final int mQuoteId;
  private final Bus mBus;
  private final DatabaseManager mDatabaseManager;

  public LoadQuoteTask(int quoteId, Bus bus, DatabaseManager db) {
    mQuoteId = quoteId;
    mBus = bus;
    mDatabaseManager = db;
  }

  @Override
  protected Quote doInBackground(Void... voids) {
    return mDatabaseManager.get(Quote.class, mQuoteId);
  }

  @Override
  protected void onPostExecute(Quote loadedQuote) {
    mBus.post(new QuoteLoadEvent(loadedQuote));
  }

  /** Emitted once the quote has been loaded from the database. */
  public static class QuoteLoadEvent {
    private final Quote mLoadedQuote;

    public QuoteLoadEvent(Quote loadedQuote) {
      mLoadedQuote = loadedQuote;
    }

    /** Returns the loaded Quote or null if no Quote was loaded. */
    public Quote getLoadedQuote() {
      return mLoadedQuote;
    }
  }
}
