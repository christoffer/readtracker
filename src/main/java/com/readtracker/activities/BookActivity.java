package com.readtracker.activities;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.view.PagerTabStrip;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import com.j256.ormlite.dao.Dao;
import com.readtracker.*;
import com.readtracker.db.LocalHighlight;
import com.readtracker.db.LocalReading;
import com.readtracker.db.LocalSession;
import com.readtracker.fragments.BookFragmentAdapter;
import com.readtracker.interfaces.EndSessionDialogListener;
import com.readtracker.support.SessionTimerStore;
import com.readtracker.support.SessionTimer;

import java.sql.SQLException;
import java.util.ArrayList;

/**
 * Fragmented screen for browsing and reading a book
 */
public class BookActivity extends ReadTrackerActivity implements EndSessionDialogListener {
  // Fragment pages
  public static final int PAGE_UNSPECIFIED = -1;
  public static final int PAGE_SESSIONS = 0;
  public static final int PAGE_READING = 1;
  public static final int PAGE_HIGHLIGHTS = 2;

  private static final int NO_GROUP = 0;
  private static final int MENU_EDIT_BOOK_SETTINGS = 1;

  private LocalReading mLocalReading;

  private BookFragmentAdapter mBookFragmentAdapter;
  private ViewPager mViewPagerReading;

  private boolean mManualShutdown;
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

    // Load information from database
    int readingId = getIntent().getExtras().getInt(IntentKeys.READING_ID);
    reloadLocalData(readingId);
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    Log.d(TAG, "freezing state");
    if(mViewPagerReading != null) {
      outState.putInt(IntentKeys.INITIAL_FRAGMENT_PAGE, mViewPagerReading.getCurrentItem());
    }
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
      case ActivityCodes.REQUEST_ADD_PAGE_NUMBERS:
        if(resultCode == RESULT_OK) {
          Log.d(TAG, "Came back from adding page number");
          mInitialPageForFragmentAdapter = PAGE_READING;
          int updateReadingId = data.getIntExtra(IntentKeys.READING_ID, -1);
          reloadLocalData(updateReadingId);
        }
        break;
      case ActivityCodes.CREATE_HIGHLIGHT:
        if(resultCode == RESULT_OK) {
          Log.d(TAG, "Came back from creating a highlight");
          mInitialPageForFragmentAdapter = PAGE_HIGHLIGHTS;
          int updateReadingId = data.getIntExtra(IntentKeys.READING_ID, -1);
          reloadLocalData(updateReadingId); // TODO optimally we should only reload the highlights here
        }
        break;
      case ActivityCodes.REQUEST_BOOK_SETTINGS:
        if(resultCode == ActivityCodes.RESULT_OK) {
          Log.d(TAG, "Came back from changing the book settings");
          // Something changed
          reloadLocalData(data.getIntExtra(IntentKeys.READING_ID, -1));
        } else if(resultCode == ActivityCodes.RESULT_DELETED_BOOK) {
          // finish with success to have the home screen reload
          shutdownWithResult(ActivityCodes.RESULT_OK);
        }
    }
    super.onActivityResult(requestCode, resultCode, data);
  }

  @Override public boolean onCreateOptionsMenu(Menu menu) {
    menu.add(NO_GROUP, MENU_EDIT_BOOK_SETTINGS, 0, "Edit book settings");
    return true;
  }

  @Override public boolean onMenuItemSelected(int featureId, MenuItem item) {
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
    // Fire off a transfer of the new session
    startService(new Intent(this, ReadmillTransferIntent.class));

    // And bail out
    shutdownWithResult(ActivityCodes.RESULT_OK);
  }

  /**
   * The session could not be created.
   */
  @Override
  public void onSessionFailed() {
    toast("Failed to save your reading session.\n\nPlease report this to the developer.");
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
      toastLong("An error occurred while loading data for this book");
      shutdownWithResult(ActivityCodes.RESULT_CANCELED);
      return;
    }

    mLocalReading = bundle.localReading;
    ArrayList<LocalSession> localSessions = bundle.localSessions;
    ArrayList<LocalHighlight> localHighlights = bundle.localHighlights;

    Log.d(TAG, "Got " + localSessions.size() + " reading sessions and " + localHighlights.size() + " highlights");

    // Book info
    ViewBindingBookHeader.bind(this, mLocalReading, new ViewBindingBookHeader.BookHeaderClickListener() {
      public void onBackButtonClick() {
        exitToHomeScreen();
      }
    });

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

    if(mBookFragmentAdapter != null) {
      Log.d(TAG, "Has FragmentAdapter");
      mBookFragmentAdapter.setBundle(bundle);
      mBookFragmentAdapter.notifyDataSetChanged();
    } else {
      mBookFragmentAdapter = new BookFragmentAdapter(getSupportFragmentManager(), bundle);
      mBookFragmentAdapter.setBrowserMode(browserMode);
    }

    SessionTimer activeSessionTimer = getIntent().getExtras().getParcelable(IntentKeys.READING_SESSION_STATE);
    Log.d(TAG, "Received reading session state " + activeSessionTimer);
    mBookFragmentAdapter.setReadingState(activeSessionTimer);

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
      case PAGE_HIGHLIGHTS:
        page = mBookFragmentAdapter.getHighlightsPageIndex();
        break;
    }
    mViewPagerReading.setCurrentItem(page, false);
  }

  // Private

  public void exitToBookInfoScreen(LocalReading localReading) {
    Intent intentEditBook = new Intent(this, AddBookActivity.class);
    intentEditBook.putExtra(IntentKeys.LOCAL_READING, localReading);
    intentEditBook.putExtra(IntentKeys.FROM_READING_SESSION, true);
    startActivityForResult(intentEditBook, ActivityCodes.REQUEST_ADD_PAGE_NUMBERS);
  }

  public void exitToCreateHighlightScreen() {
    Intent activityAddHighlight = new Intent(this, HighlightActivity.class);
    activityAddHighlight.putExtra(IntentKeys.LOCAL_READING, mLocalReading);
    startActivityForResult(activityAddHighlight, ActivityCodes.CREATE_HIGHLIGHT);
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
      toast("Pausing " + mLocalReading.title + "\n\nClick it again to resume");
      finish();
    } else {
      shutdownWithResult(ActivityCodes.RESULT_CANCELED);
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
    setResult(resultCode, resultBundle);

    mManualShutdown = true;
    SessionTimerStore.clear();
    finish();
  }

  private void finishWithGenericError() {
    toast("An error occurred while reading the book");
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
      sessionDao = ApplicationReadTracker.getSessionDao();
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
          .where().eq(LocalHighlight.READING_ID_FIELD_NAME, readingId)
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

