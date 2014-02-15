package com.readtracker.android.activities;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.view.PagerTabStrip;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.j256.ormlite.dao.Dao;
import com.readtracker.android.ApplicationReadTracker;
import com.readtracker.android.IntentKeys;
import com.readtracker.android.R;
import com.readtracker.android.db.LocalHighlight;
import com.readtracker.android.db.LocalReading;
import com.readtracker.android.db.LocalSession;
import com.readtracker.android.fragments.BookFragmentAdapter;
import com.readtracker.android.interfaces.EndSessionDialogListener;
import com.readtracker.android.support.SessionTimer;
import com.readtracker.android.support.SessionTimerStore;

import java.sql.SQLException;
import java.util.ArrayList;

/**
 * Fragmented screen for browsing and reading a book
 */
public class BookActivity extends BookBaseActivity implements EndSessionDialogListener {
  // Fragment pages
  public static final int PAGE_UNSPECIFIED = -1;
  public static final int PAGE_SESSIONS = 0;
  public static final int PAGE_READING = 1;
  public static final int PAGE_QUOTES = 2;

  private static final int NO_GROUP = 0;
  private static final int MENU_EDIT_BOOK_SETTINGS = 1;

  private boolean mForceResultOK = false;

  private LocalReading mLocalReading;

  private BookFragmentAdapter mBookFragmentAdapter;
  private ViewPager mViewPagerReading;

  private boolean mManualShutdown;

  // Flag that controls the return value of the activity to indicate
  // a need for syncing with Readmill.
  private boolean mShouldSync = false;

  private int mInitialPageForFragmentAdapter = PAGE_UNSPECIFIED;

  public void onCreate(Bundle in) {
    super.onCreate(in);
    Log.d(TAG, "onCreate()");
    setContentView(R.layout.activity_book);

    if(in != null) {
      Log.d(TAG, "unfreezing state");
      mInitialPageForFragmentAdapter = in.getInt(IntentKeys.INITIAL_FRAGMENT_PAGE, PAGE_SESSIONS);
    }

    mViewPagerReading = (ViewPager) findViewById(R.id.pagerBookActivity);

    // When a book is thrown off to the reading session immediately after being
    // created we want to finish the activity with result OK to induce the home screen
    // to reload the list of readings
    mForceResultOK = getIntent().getBooleanExtra(IntentKeys.FORCE_RESULT_OK, false);

    // Load information from database
    int readingId;
    if(in == null) {
      readingId = getIntent().getExtras().getInt(IntentKeys.READING_ID);
    } else {
      readingId = in.getInt(IntentKeys.READING_ID);
    }

    reloadLocalData(readingId);
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    Log.d(TAG, "freezing state");
    if(mViewPagerReading != null) {
      outState.putInt(IntentKeys.INITIAL_FRAGMENT_PAGE, mViewPagerReading.getCurrentItem());
    }
    outState.putInt(IntentKeys.READING_ID, mLocalReading.id);
  }

  @Override
  public void onBackPressed() {
    exitToHomeScreen();
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    Log.v(TAG, "onActivityResult()");
    if(resultCode == ActivityCodes.RESULT_CANCELED) {
      return;
    }

    Log.v(TAG, "onActivityResult: requestCode: " + requestCode + ", resultCode: " + resultCode);
    switch(requestCode) {
      case ActivityCodes.REQUEST_EDIT_PAGE_NUMBERS:
        if(resultCode == RESULT_OK) {
          Log.d(TAG, "Came back from editing page number");
          mInitialPageForFragmentAdapter = PAGE_READING;
          int updateReadingId = data.getIntExtra(IntentKeys.READING_ID, -1);
          reloadLocalData(updateReadingId);
        }
        break;
      case ActivityCodes.ADD_QUOTE:
        if(resultCode == RESULT_OK) {
          Log.d(TAG, "Came back from adding a quote");
          mInitialPageForFragmentAdapter = PAGE_QUOTES;
          int updateReadingId = data.getIntExtra(IntentKeys.READING_ID, -1);
          reloadLocalData(updateReadingId); // TODO optimally we should only reload the highlights here
          mShouldSync = true; // Ask for sync when returning to HomeActivity
        }
        break;
      case ActivityCodes.REQUEST_BOOK_SETTINGS:
        if(resultCode == ActivityCodes.RESULT_REQUESTED_BOOK_SETTINGS) {
          exitToBookEditScreen((LocalReading) data.getParcelableExtra(IntentKeys.LOCAL_READING));
        } else if(resultCode == ActivityCodes.RESULT_DELETED_BOOK) {
          shutdownWithResult(ActivityCodes.RESULT_LOCAL_READING_UPDATED);
        } else if(resultCode == ActivityCodes.RESULT_OK) {
          Log.d(TAG, "Came back from changing the book settings");
          // Something changed
          reloadLocalData(data.getIntExtra(IntentKeys.READING_ID, -1));
        }
    }
    super.onActivityResult(requestCode, resultCode, data);
  }

  @Override public boolean onCreateOptionsMenu(Menu menu) {
    menu.add(NO_GROUP, MENU_EDIT_BOOK_SETTINGS, 0, "Edit book settings");
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch(item.getItemId()) {
      case MENU_EDIT_BOOK_SETTINGS:
        exitToBookSettings();
        break;
      default:
        return false;
    }
    return true;
  }

  /**
    The session has been successfully created.
   */
  @Override
  public void onSessionCreated(LocalSession localSession) {
    Log.d(TAG, "Created a local session");
    shutdownWithResult(ActivityCodes.RESULT_LOCAL_READING_UPDATED);
  }

  /**
   * The session could not be created.
   */
  @Override
  public void onSessionFailed() {
    toast(getString(R.string.book_error_session_failed));
  }

  /**
   * Loads local reading data for a given reading.
   * Loads the LocalReading, LocalSessions and LocalHighlights.
   *
   * @param readingId id of LocalReading to load data for.
   */
  private void reloadLocalData(int readingId) {
    try {
      (new LoadLocalReadingAndSessionsTask()).execute(readingId);
    } catch(SQLException e) {
      Log.e(TAG, "Failed to reload local reading data", e);
      finishWithGenericError();
    }
  }

  /**
   * Called when the local information about a reading has finished being loaded
   * from the database.
   *
   * @param bundle The result bundle with all local data
   */
  public void onLocalReadingLoaded(LocalReadingBundle bundle) {
    Log.i(TAG, "Loaded LocalReading");

    if(mManualShutdown) {
      // Avoid the rather costly setup if we are shutting down
      Log.w(TAG, "Activity is shutting down - not initializing");
      return;
    }

    if(!bundle.isValid()) {
      toastLong(getString(R.string.book_error_loading));
      shutdownWithResult(ActivityCodes.RESULT_CANCELED);
      return;
    }

    mLocalReading = bundle.localReading;
    ArrayList<LocalSession> localSessions = bundle.localSessions;
    ArrayList<LocalHighlight> localHighlights = bundle.localHighlights;

    Log.d(TAG, "Got " + localSessions.size() + " reading sessions and " + localHighlights.size() + " highlights");

    setReading(mLocalReading);

    if(mInitialPageForFragmentAdapter == PAGE_UNSPECIFIED) {
      if(mLocalReading.isActive()) {
        mInitialPageForFragmentAdapter = PAGE_READING;
      } else {
        mInitialPageForFragmentAdapter = PAGE_SESSIONS;
      }
    }

    PagerTabStrip pagerTabStrip = (PagerTabStrip) findViewById(R.id.pagerTabStrip);
    pagerTabStrip.setTabIndicatorColor(mLocalReading.getColor());
    setupFragments(bundle);

    Log.i(TAG, "Initialized for reading with id:" + mLocalReading.id);
  }

  private void setupFragments(LocalReadingBundle bundle) {
    boolean browserMode = !mLocalReading.isActive();

    // (re-)create the book adapter
    mBookFragmentAdapter = new BookFragmentAdapter(getApplicationContext(), getSupportFragmentManager(), bundle);
    mBookFragmentAdapter.setBrowserMode(browserMode);

    mViewPagerReading.setAdapter(mBookFragmentAdapter);
    // The default for off-screen page limit is 1, which means that the session/highlight view
    // is unloaded when going away from the center (reading) page.
    mViewPagerReading.setOffscreenPageLimit(2);

    int page = 0;
    switch(mInitialPageForFragmentAdapter) {
      case PAGE_UNSPECIFIED:
      case PAGE_SESSIONS:
        page = mBookFragmentAdapter.getSessionsPageIndex();
        break;
      case PAGE_READING:
        page = mBookFragmentAdapter.getReadingPageIndex();
        break;
      case PAGE_QUOTES:
        page = mBookFragmentAdapter.getQuotesPageIndex();
        break;
    }
    mViewPagerReading.setCurrentItem(page, false);
  }

  // Private

  public void exitToBookEditScreen(LocalReading localReading) {
    Log.d(TAG, String.format("Exiting to edit book: %s", localReading.toString()));
    Intent intentEditBook = new Intent(this, AddBookActivity.class);
    intentEditBook.putExtra(IntentKeys.LOCAL_READING, localReading);
    intentEditBook.putExtra(IntentKeys.EDIT_MODE, true);
    startActivityForResult(intentEditBook, ActivityCodes.REQUEST_EDIT_PAGE_NUMBERS);
  }

  public void exitToAddQuoteScreen(LocalHighlight highlight) {
    Intent activityAddHighlight = new Intent(this, AddQuoteActivity.class);
    activityAddHighlight.putExtra(IntentKeys.LOCAL_READING, mLocalReading);
    activityAddHighlight.putExtra(IntentKeys.LOCAL_HIGHLIGHT, highlight);
    startActivityForResult(activityAddHighlight, ActivityCodes.ADD_QUOTE);
  }

  /**
   * Exits to the home activity with correct result information.
   */
  public void exitToHomeScreen() {
    SessionTimer currentSession = mBookFragmentAdapter.getSessionTimer();
    if(currentSession.getTotalElapsed() > 0) {
      currentSession.stop();
      setResult(RESULT_CANCELED);
      mManualShutdown = false; // prevent session clearing

      toast(getString(R.string.book_pausing_book, mLocalReading.title));
      finish();
    } else {
      shutdownWithResult(mShouldSync ? ActivityCodes.RESULT_OK : ActivityCodes.RESULT_CANCELED);
    }
  }

  public void exitToBookSettings() {
    Intent bookSettings = new Intent(this, BookSettingsActivity.class);
    bookSettings.putExtra(IntentKeys.LOCAL_READING, mLocalReading);
    startActivityForResult(bookSettings, ActivityCodes.REQUEST_BOOK_SETTINGS);
  }

  private void shutdownWithResult(int resultCode) {
    shutdownWithResult(resultCode, null);
  }

  private void shutdownWithResult(int resultCode, Intent resultBundle) {
    Log.v(TAG, "Shutting down");

    if(resultBundle == null) {
      resultBundle = new Intent();
    }

    resultBundle.putExtra(IntentKeys.LOCAL_READING, mLocalReading);

    resultCode = mForceResultOK ? RESULT_OK : resultCode;

    setResult(resultCode, resultBundle);

    mManualShutdown = true;
    SessionTimerStore.clear();
    finish();
  }

  private void finishWithGenericError() {
    toast(getString(R.string.book_error_reading_book));
    shutdownWithResult(ActivityCodes.RESULT_CANCELED);
  }

  /**
   * Informs fragments if we are shutting down explicitly (as opposed to being
   * shutdown because activity is being sent to background or memory collected)
   *
   * @return true if we are explicitly shutting down, false otherwise
   */
  public boolean isManualShutdown() {
    return mManualShutdown;
  }

  /**
   * Grabs a chunk of local reading information from the database
   */
  private class LoadLocalReadingAndSessionsTask extends AsyncTask<Integer, Void, LocalReadingBundle> {
    private final Dao<LocalReading, Integer> readingDao;
    private final Dao<LocalSession, Integer> sessionDao;
    private final Dao<LocalHighlight, Integer> highlightDao;

    LoadLocalReadingAndSessionsTask() throws SQLException {
      readingDao = ApplicationReadTracker.getReadingDao();
      sessionDao = ApplicationReadTracker.getLocalSessionDao();
      highlightDao = ApplicationReadTracker.getHighlightDao();
    }

    @Override
    protected LocalReadingBundle doInBackground(Integer... LocalReadingIds) {
      if(LocalReadingIds.length != 1) {
        throw new RuntimeException("Should receive exactly one id for LocalReading");
      }

      int readingId = LocalReadingIds[0];

      LocalReadingBundle bundle = new LocalReadingBundle();

      Log.d(TAG, "Fetching LocalReading with id: " + readingId);

      try {
        bundle.localReading = readingDao.queryForId(readingId);
        bundle.localSessions = getLocalSessions(readingId);
        bundle.localHighlights = getHighlights(readingId);
        bundle.localReading.setProgressStops(bundle.localSessions);
      } catch(SQLException e) {
        Log.d(TAG, "Failed to fetch data for reading with id:" + readingId, e);
      }

      return bundle;
    }

    @Override
    protected void onPostExecute(LocalReadingBundle bundle) {
      onLocalReadingLoaded(bundle);
    }

    private ArrayList<LocalSession> getLocalSessions(int readingId) throws SQLException {
      return new ArrayList<LocalSession>(
        sessionDao.queryBuilder()
          .where().eq(LocalSession.READING_ID_FIELD_NAME, readingId)
          .query()
      );
    }

    private ArrayList<LocalHighlight> getHighlights(int readingId) throws SQLException {
      final String orderField = LocalHighlight.HIGHLIGHTED_AT_FIELD_NAME;

      return new ArrayList<LocalHighlight>(
        highlightDao.queryBuilder()
          .orderByRaw("datetime(" + orderField + ") DESC")
          .where()
          .eq(LocalHighlight.READING_ID_FIELD_NAME, readingId)
          .and()
          .eq(LocalHighlight.DELETED_BY_USER_FIELD_NAME, false)
          .query()
      );
    }
  }

  public class LocalReadingBundle {
    public LocalReading localReading;
    public ArrayList<LocalSession> localSessions = new ArrayList<LocalSession>();
    public ArrayList<LocalHighlight> localHighlights = new ArrayList<LocalHighlight>();

    public boolean isValid() {
      Log.d(TAG, "Checking validity of fetched bundle: " +
              "LocalReading: " + (localReading == null ? "null" : localReading.id) +
              ", readingSessions: " + (localSessions == null ? "null" : localSessions.size()) +
              ", readingHighlights: " + (localHighlights == null ? "null" : localHighlights.size())
      );
      return localReading != null && localSessions != null && localHighlights != null;
    }
  }
}

