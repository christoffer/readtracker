package com.readtracker.android.activities;

import android.os.AsyncTask;
import android.os.Build;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBar;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.MenuItem;
import android.widget.ImageView;

import com.readtracker.BuildConfig;
import com.readtracker.R;
import com.readtracker.android.db.Book;
import com.readtracker.android.db.DatabaseManager;
import com.squareup.picasso.Picasso;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;

public abstract class BookBaseActivity extends BaseActivity {
  public static final String KEY_BOOK_ID = "BOOK_ID";

  // Task for loading data in the background
  private LoadDataTask mLoadDataTask;

  // Currently loaded book
  private Book mBook;

  private final ArrayList<Runnable> mBookReadyRunnables = new ArrayList<Runnable>();

  public Book getBook() {
    return mBook;
  }

  protected void loadBook(int bookId) {
    if(mLoadDataTask != null) {
      return;
    }

    mLoadDataTask = new LoadDataTask(this, bookId);
    mLoadDataTask.execute();
  }

  /**
   * Convenience method for loading the book with id provided as
   * extra with key: {@code BaseBookActivity.KEY_BOOK_ID}
   */
  protected void loadBookFromIntent() {
    if(!getIntent().hasExtra(KEY_BOOK_ID)) {
      throw new IllegalArgumentException("Must provide key: " + KEY_BOOK_ID + " with the book id");
    }

    loadBook(getBookIdFromIntent());
  }

  /** Calls a callback when the book has been loaded. */
  public void runWhenBookIsReady(Runnable runnable) {
    mBookReadyRunnables.add(runnable);
    if(BuildConfig.DEBUG) {
      Log.v(getClass().getSimpleName(), "Adding book ready runnable");
    }

    flushBookReadyRunnables();
  }

  /**
   * Returns the book id that was passed with the {@code BookBaseActivity.KEY_BOOK_ID} key.
   * Throws an exception if it wasn't found.
   */
  protected int getBookIdFromIntent() {
    return getIntent().getExtras().getInt(KEY_BOOK_ID);
  }

  private void flushBookReadyRunnables() {
    if(mBookReadyRunnables.isEmpty() || mBook == null) {
      return;
    }

    if(BuildConfig.DEBUG) {
      Log.v(getClass().getSimpleName(), "Flushing " + mBookReadyRunnables.size() + " runnables");
    }

    for(Iterator<Runnable> iterator = mBookReadyRunnables.iterator(); iterator.hasNext(); ) {
      Runnable runnable = iterator.next();
      iterator.remove();
      runnable.run();
    }
  }

  private void setupActionBar(Book book) {
    ActionBar actionBar = getSupportActionBar();

    // Set the cover as the home icon. Unfortunately it seems like there's no easy way of getting
    // the ImageView from the actionbar pre-11. So Gingerbread will be stuck with the default image...
    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
      ImageView homeIcon = (ImageView) findViewById(android.R.id.home);
      if(homeIcon != null && !TextUtils.isEmpty(book.getCoverImageUrl())) {
        int size = getActionBarHeight();
        if(size == 0) size = 48; // Arbitrary default value
        Picasso.with(this)
            .load(book.getCoverImageUrl())
            .placeholder(R.drawable.bookmark)
            .resize(size, size)
            .centerCrop()
            .into(homeIcon);
        actionBar.setDisplayShowHomeEnabled(true);
      }
    }

    actionBar.setTitle(book.getTitle());
    actionBar.setSubtitle(book.getAuthor());
    actionBar.setDisplayHomeAsUpEnabled(true);
  }

  /** Callback from the async task loading the book (with associated data) from the database. */
  protected abstract void onBookLoaded(Book book);

  /** Return true for activities that need related book data, such as Sessions and Quotes. */
  protected boolean shouldLoadRelatedBookData() {
    return false;
  }

  private void onLoadTaskCompleted(Book book) {
    mBook = book;
    mLoadDataTask = null;
    flushBookReadyRunnables();
    setupActionBar(book);
    onBookLoaded(book);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    if(item.getItemId() == android.R.id.home) {
      NavUtils.navigateUpFromSameTask(this);
      return true;
    }
    return super.onOptionsItemSelected(item);
  }

  public int getActionBarHeight() {
    // Calculate ActionBar height
    TypedValue tv = new TypedValue();
    if(getTheme() != null && getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true)) {
      return TypedValue.complexToDimensionPixelSize(tv.data, getResources().getDisplayMetrics());
    }
    return 0;
  }

  /** Load book data from the database. */
  private static class LoadDataTask extends AsyncTask<Void, Void, Book> {
    private final WeakReference<BookBaseActivity> mActivityRef;
    private final int mBookId;

    private final DatabaseManager mDatabaseManager;
    private final boolean mLoadRelated;

    LoadDataTask(BookBaseActivity activity, int bookId) {
      mActivityRef = new WeakReference<BookBaseActivity>(activity);
      mBookId = bookId;
      mDatabaseManager = activity.getApp().getDatabaseManager();
      mLoadRelated = activity.shouldLoadRelatedBookData();
    }

    @Override
    protected Book doInBackground(Void... params) {
      // Fire off events for each part as they become available to increase feedback to the user
      // TODO Error handling

      Book book = mDatabaseManager.get(Book.class, mBookId);
      if(book == null) {
        Log.w("LoadDataTask", "Failed to load book");
        return null;
      } else if(mLoadRelated) {
        book.loadSessions(mDatabaseManager);
        book.loadQuotes(mDatabaseManager);
      }

      return book;
    }

    @Override protected void onPostExecute(Book book) {
      BookBaseActivity activity = mActivityRef.get();
      if(book != null && activity != null && !activity.isFinishing()) {
        activity.onLoadTaskCompleted(book);
      }
    }
  }

  /** Emitted when the BookActivity has loaded the book, with all it's sessions and quotes. */
  public static class BookLoadedEvent {
    private final Book mBook;

    BookLoadedEvent(Book book) { mBook = book; }

    public Book getBook() { return mBook; }
  }
}
