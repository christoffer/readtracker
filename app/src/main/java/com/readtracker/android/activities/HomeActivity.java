package com.readtracker.android.activities;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.view.PagerTabStrip;
import android.support.v4.view.ViewPager;
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

import com.readtracker.android.IntentKeys;
import com.readtracker.android.R;
import com.readtracker.android.ReadTrackerApp;
import com.readtracker.android.db.Book;
import com.readtracker.android.db.DatabaseManager;
import com.readtracker.android.fragments.BookListFragment;
import com.readtracker.android.fragments.HomeFragmentAdapter;
import com.squareup.otto.Bus;
import com.squareup.otto.Produce;
import com.squareup.otto.Subscribe;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class HomeActivity extends BaseActivity {
  private static final int REQUEST_READING_SESSION = 1;

  // List of books loaded from the database
  private List<Book> mBooks = new ArrayList<Book>();

  private ViewPager mViewPager;
  HomeFragmentAdapter mFragmentAdapter;

  private Bus mBus;
  private BookLoadTask mBookLoadTask;

  @Override public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    mBus = ReadTrackerApp.from(this).getBus();

    requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

    setContentView(R.layout.activity_home);

    // Show welcome screen for first time users
    if(getApp().getFirstTimeFlag()) {
      Log.d(TAG, "First time opening the app, showing introduction.");
      showIntroduction();
    }

    PagerTabStrip pagerTabStrip = (PagerTabStrip) findViewById(R.id.pager_tab_strip);
    pagerTabStrip.setTabIndicatorColor(getResources().getColor(R.color.base_color));

    mViewPager = (ViewPager) findViewById(R.id.book_list_pager);

    resetFragmentAdapter();
    mViewPager.setCurrentItem(mFragmentAdapter.getDefaultPage());

    loadBooks();
  }

  @Override protected void onResume() {
    super.onResume();
    mBus.register(this);
  }

  @Override protected void onPause() {
    super.onPause();
    mBus.unregister(this);
  }

  @Produce public BooksLoadedEvent produceBooksLoadedEvent() {
    return new BooksLoadedEvent(mBooks);
  }

  @Subscribe public void onBookClickedEvent(BookListFragment.BookClickedEvent event) {
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

    // TODO we should be smarter here and just reload the one book that was changed
    final boolean shouldReload = (
      requestCode == REQUEST_READING_SESSION && resultCode == RESULT_OK
    );
    final boolean needReloadDueToAddedBook = requestCode == ActivityCodes.REQUEST_ADD_BOOK;

    // Handle coming back from settings
    if(requestCode == ActivityCodes.SETTINGS) {
      // Reset the adapter to refresh views if the user toggled compact mode
      resetFragmentAdapter();
      loadBooks();
    } else if(shouldReload || needReloadDueToAddedBook) {
      // Push new changes and reload local lists
      loadBooks();
    }
  }

  @Override
  public boolean onSearchRequested() {
    exitToBookSearch();
    return true;
  }

  private void resetFragmentAdapter() {
    mFragmentAdapter = new HomeFragmentAdapter(this, getAppSettings().hasCompactFinishedList());
    mViewPager.setAdapter(mFragmentAdapter);
  }

  /** Show introduction for new users. */
  private void showIntroduction() {
    ViewStub stub = (ViewStub) findViewById(R.id.introduction_stub);
    final View root = stub.inflate();
    TextView introText = (TextView) root.findViewById(R.id.introduction_text);
    introText.setText(Html.fromHtml(getString(R.string.introduction_text)));
    introText.setMovementMethod(LinkMovementMethod.getInstance());

    Button dismissButton = (Button) root.findViewById(R.id.start_using_button);
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
    if(mBookLoadTask != null) {
      Log.d(TAG, "Task already running, ignoring");
      return;
    }

    getSupportActionBar().setSubtitle(R.string.home_loading_books);
    setProgressBarIndeterminateVisibility(Boolean.TRUE);

    mBookLoadTask = new BookLoadTask(this);
    mBookLoadTask.execute();
  }

  private void onBooksLoaded(List<Book> books) {
    mBookLoadTask = null;
    mBus.post(new BooksLoadedEvent(books));

    getSupportActionBar().setSubtitle(null);
    setProgressBarIndeterminateVisibility(Boolean.FALSE);
  }

  /** Load all Book models from the database. */
  private static class BookLoadTask extends AsyncTask<Void, Void, List<Book>> {
    private final WeakReference<HomeActivity> mActivity;
    private final DatabaseManager mDatabaseManager;

    public BookLoadTask(HomeActivity activity) {
      mActivity = new WeakReference<HomeActivity>(activity);
      mDatabaseManager = ReadTrackerApp.from(activity).getDatabaseManager();
    }

    @Override
    protected List<Book> doInBackground(Void... ignored) {
      List<Book> books = mDatabaseManager.getAll(Book.class);
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
        activity.onBooksLoaded(books);
      }
    }
  }

  /** Emitted when the HomeActivity has finished loading books from the database. */
  public static class BooksLoadedEvent {
    private final List<Book> mBooks;

    public BooksLoadedEvent(List<Book> books) {
      mBooks = books;
    }

    public List<Book> getBooks() {
      return mBooks;
    }
  }
}
