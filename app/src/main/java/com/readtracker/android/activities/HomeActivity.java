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

import com.readtracker.R;
import com.readtracker.android.ReadTrackerApp;
import com.readtracker.android.db.Book;
import com.readtracker.android.db.DatabaseManager;
import com.readtracker.android.fragments.BookListFragment;
import com.readtracker.android.fragments.HomeFragmentAdapter;
import com.squareup.otto.Subscribe;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;

public class HomeActivity extends BaseActivity {
  private static final String TAG = HomeActivity.class.getSimpleName();

  private static final int REQUEST_READING_SESSION = 1;

  // List of books loaded from the database
  private List<Book> mBooks = new ArrayList<Book>();

  @InjectView(R.id.book_list_pager) ViewPager mViewPager;

  private HomeFragmentAdapter mFragmentAdapter;
  private LoadCatalogueTask mLoadCatalogueTask;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Log.d(TAG, "onCreate");
    requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
    setContentView(R.layout.activity_home);
    ButterKnife.inject(this);

    // Show welcome screen for first time users
    if(getApp().getFirstTimeFlag()) {
      Log.d(TAG, "First time opening the app, showing introduction.");
      showIntroduction();
    }

    PagerTabStrip pagerTabStrip = (PagerTabStrip) findViewById(R.id.pager_tab_strip);
    pagerTabStrip.setTabIndicatorColor(getResources().getColor(R.color.base_color));

    resetFragmentAdapter();
    loadBooks();
    mViewPager.setCurrentItem(mFragmentAdapter.getDefaultPage());
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
    final boolean shouldReload = (
      requestCode == REQUEST_READING_SESSION && resultCode == RESULT_OK
    );
    final boolean needReloadDueToAddedBook = requestCode == ActivityCodes.REQUEST_ADD_BOOK;

    // Handle coming back from settings
    if(requestCode == ActivityCodes.SETTINGS) {
      // Reset the adapter to refresh views if the user toggled compact mode
      resetFragmentAdapter();
    } else if(shouldReload || needReloadDueToAddedBook) {
      if(data != null && data.getBooleanExtra(BookActivity.KEY_FINISHED, false)) {
        // Came back with a success result for a finished book, let the view pager show it
        if(mViewPager != null) {
          mViewPager.setCurrentItem(0);
        }
      }
    }
  }

  @Override
  public boolean onSearchRequested() {
    exitToBookSearch();
    return true;
  }

  /** Returns a list of all books currently loaded. */
  public List<Book> getBooks() {
    return mBooks;
  }

  private void resetFragmentAdapter() {
    mFragmentAdapter = new HomeFragmentAdapter(this, getAppSettings().hasCompactFinishedList());
    mViewPager.setAdapter(mFragmentAdapter);
  }

  /**
   * Show introduction for new users.
   */
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
    if(mLoadCatalogueTask != null) {
      Log.d(TAG, "Task already running, ignoring");
      return;
    }

    getSupportActionBar().setSubtitle(R.string.home_loading_books);
    setProgressBarIndeterminateVisibility(Boolean.TRUE);

    mLoadCatalogueTask = new LoadCatalogueTask(this);
    mLoadCatalogueTask.execute();
  }

  private void onCatalogueLoaded(List<Book> books) {
    mLoadCatalogueTask = null;
    mBooks = books;
    postEvent(new CatalogueLoadedEvent(books));

    getSupportActionBar().setSubtitle(null);
    setProgressBarIndeterminateVisibility(Boolean.FALSE);
  }

  /**
   * Load all Book models from the database.
   */
  private static class LoadCatalogueTask extends AsyncTask<Void, Void, List<Book>> {
    private final WeakReference<HomeActivity> mActivity;
    private final DatabaseManager mDatabaseManager;

    public LoadCatalogueTask(HomeActivity activity) {
      mActivity = new WeakReference<HomeActivity>(activity);
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

    public CatalogueLoadedEvent(List<Book> books) {
      mBooks = books;
    }

    public List<Book> getBooks() {
      return mBooks;
    }
  }
}
