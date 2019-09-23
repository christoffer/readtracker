package com.readtracker.android.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.view.PagerTabStrip;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewStub;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

import com.readtracker.R;
import com.readtracker.android.ReadTrackerApp;
import com.readtracker.android.db.Book;
import com.readtracker.android.db.DatabaseManager;
import com.readtracker.android.db.export.JSONImporter;
import com.readtracker.android.fragments.BookListFragment;
import com.readtracker.android.fragments.HomeFragmentAdapter;
import com.readtracker.android.support.ApplicationSettingsHelper;
import com.readtracker.android.support.ReadTrackerDataImportHandler;
import com.readtracker.android.tasks.ImportReadTrackerFileTask;
import com.squareup.otto.Subscribe;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;

public class HomeActivity extends BaseActivity implements ImportReadTrackerFileTask.ResultListener {
  private static final String TAG = HomeActivity.class.getSimpleName();

  private static final int REQUEST_READING_SESSION = 1;

  // List of books loaded from the database
  private List<Book> mBooks = new ArrayList<>();

  @InjectView(R.id.book_list_pager) ViewPager mViewPager;
  @InjectView(R.id.pager_tab_strip) PagerTabStrip mPagerTabStrip;

  private LoadCatalogueTask mLoadCatalogueTask;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    supportRequestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
    super.onCreate(savedInstanceState);
    Log.d(TAG, "onCreate");
    setContentView(R.layout.activity_home);
    ButterKnife.inject(this);

    // Show welcome screen for first time users
    if(getApp().getFirstTimeFlag()) {
      Log.d(TAG, "First time opening the app, showing introduction.");
      showIntroduction();
    }

    resetFragmentAdapter();
    loadBooks();
    mPagerTabStrip.setDrawFullUnderline(false);
    mViewPager.setCurrentItem(HomeFragmentAdapter.DEFAULT_POSITION);
  }

  @Override protected void onStart() {
    super.onStart();
    loadBooks();
  }

  @Override protected void onDestroy() {
    super.onDestroy();
    Log.d(TAG, "Destroying");
  }

  @Subscribe
  public void onBookClickedEvent(BookListFragment.BookClickedEvent event) {
    exitToBookActivity(event.getBook().getId());
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.home, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    int clickedId = item.getItemId();

    if(clickedId == R.id.settings_menu) {
      exitToSettings();
    } else if(clickedId == R.id.add_book_menu) {
      exitToBookSearch();
    } else {
      return false;
    }

    return true;
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    final boolean returnFromReadingSession = requestCode == REQUEST_READING_SESSION && resultCode == RESULT_OK;
    final boolean readingSessionFinishedBook = data != null && data.getBooleanExtra(BookActivity.KEY_FINISHED, false);
    final boolean returnFromAddBook = requestCode == ActivityCodes.REQUEST_ADD_BOOK;

    if (ReadTrackerDataImportHandler.handleActivityResult(this, requestCode, resultCode, data)) {
      // Result was for an import request which was handled, so bail out
      return;
    }

    // Handle coming back from settings
    if(requestCode == ActivityCodes.SETTINGS) {
      Log.d(TAG, "Returned to HomeActivity from Settings -- reapplying settings");
      // Reset the adapter to refresh views if the user toggled compact mode
      resetFragmentAdapter();
    } else if(returnFromReadingSession || returnFromAddBook) {
      Log.d(TAG, "Returned with a trigger for reloading books");
      if(returnFromAddBook) {
        Log.v(TAG, "Returned from adding book, swiping to read list");
        // Came back from adding a book, swipe to reading list to show it
        if(mViewPager != null) {
          mViewPager.setCurrentItem(1);
        }
      } else if(readingSessionFinishedBook) {
        Log.v(TAG, "Returning from finishing book, swiping to finish list");
        // Came back with a success result for a finished book, let the view pager show it
        if(mViewPager != null) {
          mViewPager.setCurrentItem(0);
        }
      }
    }
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    Log.v(TAG, String.format("onRequestPermissionsResult: permissions %s, grantedResults %s", Arrays.toString(permissions), Arrays.toString(grantResults)));
    ReadTrackerDataImportHandler.handleRequestPermissionResult(this, requestCode, permissions, grantResults);
  }

  @Override
  public boolean onSearchRequested() {
    exitToBookSearch();
    return true;
  }

  @Override public void onImportStart() {
    ReadTrackerDataImportHandler.openProgressDialog(this);
  }

  @Override public void onImportComplete(JSONImporter.ImportResultReport result) {
    ReadTrackerDataImportHandler.closeProgressDialog(this, result);
  }

  @Override public void onImportUpdate(int currentBook, int totalBooks) {
    ReadTrackerDataImportHandler.showProgressUpdate(currentBook, totalBooks);
  }

  @Override public Activity getResultActivity() {
    return this;
  }


  /** Returns a list of all books currently loaded. */
  public List<Book> getBooks() {
    return mBooks;
  }

  private void resetFragmentAdapter() {
    Log.d(TAG, "Resetting HomeFragmentAdapter");
    final ApplicationSettingsHelper settings = getAppSettings();
    final boolean useCompactFinishedList = settings.getUseCompactFinishedList();
    final boolean useFullDates = settings.getUseFullDates();
    HomeFragmentAdapter adapter = new HomeFragmentAdapter(this, useCompactFinishedList, useFullDates);
    mViewPager.setAdapter(adapter);
  }

  /**
   * Show introduction for new users.
   */
  private void showIntroduction() {
    ViewStub stub = findViewById(R.id.introduction_stub);
    final View root = stub.inflate();
    TextView introText = root.findViewById(R.id.introduction_text);
    introText.setText(Html.fromHtml(getString(R.string.introduction_text)));
    introText.setMovementMethod(LinkMovementMethod.getInstance());

    Button dismissButton = root.findViewById(R.id.start_using_button);
    dismissButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        root.setVisibility(View.GONE);
        getApp().removeFirstTimeFlag();
      }
    });
  }

  private void exitToBookSearch() {
    startActivityForResult(new Intent(this, BookSearchActivity.class), ActivityCodes.REQUEST_ADD_BOOK);
  }

  private void exitToSettings() {
    Intent intentSettings = new Intent(this, SettingsActivity.class);
    startActivityForResult(intentSettings, ActivityCodes.SETTINGS);
  }

  private void exitToBookActivity(int bookId) {
    Intent bookActivity = new Intent(this, BookActivity.class);
    bookActivity.putExtra(BookActivity.KEY_BOOK_ID, bookId);

    startActivityForResult(bookActivity, ActivityCodes.REQUEST_READING_SESSION);
  }

  private void loadBooks() {
    if(mLoadCatalogueTask != null) {
      Log.d(TAG, "Task already running, ignoring");
      return;
    }
    final ActionBar actionBar = getSupportActionBar();
    if (actionBar != null) {
      getSupportActionBar().setSubtitle(R.string.home_loading_books);
    }
    setProgressBarIndeterminateVisibility(Boolean.TRUE);

    mLoadCatalogueTask = new LoadCatalogueTask(this);
    mLoadCatalogueTask.execute();
  }

  private void onCatalogueLoaded(List<Book> books) {
    mLoadCatalogueTask = null;
    mBooks = books;
    postEvent(new CatalogueLoadedEvent(books));

    final ActionBar actionBar = getSupportActionBar();
    if (actionBar != null) {
      actionBar.setSubtitle(null);
    }
    setProgressBarIndeterminateVisibility(Boolean.FALSE);
  }

  /**
   * Load all Book models from the database.
   */
  private static class LoadCatalogueTask extends AsyncTask<Void, Void, List<Book>> {
    private final WeakReference<HomeActivity> mActivity;
    private final DatabaseManager mDatabaseManager;

    LoadCatalogueTask(HomeActivity activity) {
      mActivity = new WeakReference<>(activity);
      mDatabaseManager = ReadTrackerApp.from(activity).getDatabaseManager();
    }

    @Override
    protected List<Book> doInBackground(Void... ignored) {
      List<Book> books = mDatabaseManager.getAll(Book.class);
      Log.d(TAG, String.format("Loaded %d books", books.size()));
      for(Book book : books) {
        // Need the sessions to display segmented progress bars
        book.loadSessions(mDatabaseManager);
      }
      return books;
    }

    @Override
    protected void onPostExecute(List<Book> books) {
      HomeActivity activity = mActivity.get();
      if(activity != null && !activity.isFinishing()) {
        activity.onCatalogueLoaded(books);
      }
    }
  }

  /**
   * Emitted when the HomeActivity has finished loading books from the database.
   */
  public static class CatalogueLoadedEvent {
    private final List<Book> mBooks;

    @SuppressWarnings("WeakerAccess")
    public CatalogueLoadedEvent(List<Book> books) {
      mBooks = books;
    }

    public List<Book> getBooks() {
      return mBooks;
    }
  }
}
