package com.readtracker_beta.activities;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import com.j256.ormlite.dao.Dao;
import com.readtracker_beta.*;
import com.readtracker_beta.db.LocalHighlight;
import com.readtracker_beta.db.LocalReading;
import com.readtracker_beta.db.LocalSession;
import com.readtracker_beta.fragments.BookFragmentAdapter;
import com.readtracker_beta.support.SessionTimerStore;
import com.readtracker_beta.support.SessionTimer;

import java.sql.SQLException;
import java.util.ArrayList;

/**
 * Fragmented screen for browsing and reading a book
 */
public class BookActivity extends ReadTrackerActivity {
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
    if(resultCode == ActivityCodes.RESULT_CANCELED) {
      return;
    }

    Log.v(TAG, "onActivityResult: requestCode: " + requestCode + ", resultCode: " + resultCode);
    switch(requestCode) {
      case ActivityCodes.CREATE_PING:
        // Set result to OK to state that something was changed
        if(resultCode == RESULT_OK) {
          Log.d(TAG, "Came back from ping creation");
          finishWithResult(ActivityCodes.RESULT_OK);
          return;
        }
        break;
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
          // Something changed
          reloadLocalData(data.getIntExtra(IntentKeys.READING_ID, -1));
        } else if(resultCode == ActivityCodes.RESULT_DELETED_BOOK) {
          finishWithResult(ActivityCodes.RESULT_OK); // finish with success to have the home screen reload
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
      Log.w(TAG, "Activity is shutting down - not initializing");
      return;
    }

    if(!bundle.isValid()) {
      toastLong("An error occurred while loading data for this book");
      finishWithResult(ActivityCodes.RESULT_CANCELED);
      return;
    }

    mLocalReading = bundle.localReading;
    ArrayList<LocalSession> localSessions = bundle.localSessions;
    ArrayList<LocalHighlight> localHighlights = bundle.localHighlights;

    Log.d(TAG, "Got " + localSessions.size() + " reading sessions and " + localHighlights.size() + " highlights");

    // Book info
    ViewBindingBookHeader.bind(this, mLocalReading, new ViewBindingBookHeader.BookHeaderClickListener() {
      @Override
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

  public void exitToSessionEndScreen(long elapsedMilliseconds) {
    Intent intentReadingSessionEnd = new Intent(this, EndSessionActivity.class);
    intentReadingSessionEnd.putExtra(IntentKeys.LOCAL_READING, mLocalReading);
    intentReadingSessionEnd.putExtra(IntentKeys.SESSION_LENGTH_MS, elapsedMilliseconds);
    startActivityForResult(intentReadingSessionEnd, ActivityCodes.CREATE_PING);
  }

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
      SessionTimer sessionTimer = mBookFragmentAdapter.getSessionTimer();
      sessionTimer.stop();
      finishWithResultAndPausedSession(ActivityCodes.RESULT_CANCELED, mLocalReading.id, sessionTimer);
      toast("Pausing " + mLocalReading.title + "\n\nClick it again to resume");
    } else {
      finishWithResult(ActivityCodes.RESULT_CANCELED);
    }
  }

  public void exitToBookSettings() {
    Intent bookSettings = new Intent(this, BookSettingsActivity.class);
    bookSettings.putExtra(IntentKeys.LOCAL_READING, mLocalReading);
    startActivityForResult(bookSettings, ActivityCodes.REQUEST_BOOK_SETTINGS);
  }

  public void finishWithResult(int resultCode) {
    setResult(resultCode);
    shutdown();
  }

  public void finishWithResultAndPausedSession(int resultCode, int localReadingId, SessionTimer sessionTimer) {
    Log.v(TAG, String.format("Finishing with reading id: %d and session timer: %s", localReadingId, sessionTimer.toString()));
    Intent resultIntent = new Intent();
    resultIntent.putExtra(IntentKeys.READING_SESSION_STATE, sessionTimer);
    setResult(resultCode, resultIntent);
    shutdown();
  }

  private void shutdown() {
    Log.v(TAG, "Shutting down");
    mManualShutdown = true;
    SessionTimerStore.clear();
    finish();
  }

  private void finishWithGenericError() {
    toast("An error occurred while reading the book");
    finishWithResult(ActivityCodes.RESULT_CANCELED);
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

