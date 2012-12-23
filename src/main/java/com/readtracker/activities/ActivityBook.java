package com.readtracker.activities;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.util.Log;
import com.j256.ormlite.dao.Dao;
import com.readtracker.*;
import com.readtracker.db.LocalHighlight;
import com.readtracker.db.LocalReading;
import com.readtracker.db.LocalSession;
import com.readtracker.fragments.BookFragmentAdapter;
import com.readtracker.support.SessionTimerStore;
import com.readtracker.support.SessionTimer;

import java.sql.SQLException;
import java.util.ArrayList;

/**
 * Fragmented screen for browsing and reading a book
 */
public class ActivityBook extends ReadTrackerActivity {
  // Fragment pages
  public static final int PAGE_UNSPECIFIED = -1;
  public static final int PAGE_SESSIONS = 0;
  public static final int PAGE_READING = 1;
  public static final int PAGE_HIGHLIGHTS = 2;

  private LocalReading mLocalReading;

  private BookFragmentAdapter mBookFragmentAdapter;
  private ViewPager mViewPagerReading;

  private boolean mIsSessionStarted;
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
    Log.d(TAG, "onActivityResult: requestCode: " + requestCode + ", resultCode: " + resultCode);
    switch(requestCode) {
      case ActivityCodes.CREATE_PING:
        // Set result to OK to state that something was changed
        if(resultCode == RESULT_OK) {
          Log.d(TAG, "Came back from ping creation");
          finishWithResult(ActivityCodes.RESULT_OK);
          return;
        }
        break;
      case ActivityCodes.REQUEST_EDIT_BOOK:
        if(resultCode == RESULT_OK) {
          Log.d(TAG, "Came back from editing the book");
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
    }
    super.onActivityResult(requestCode, resultCode, data);
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
   * Sets the hasSessionStarted flag to true.
   */
  public void markSessionStarted() {
    mIsSessionStarted = true;
  }

  /**
   * Returns a flag indicating if the user has started a sessions or not.
   *
   * @return true if a session has started.
   */
  private boolean hasSessionStarted() {
    return mIsSessionStarted;
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

    if(bundle.isValid()) {
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
      @Override public void onBackButtonClick() {
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
    Intent intentReadingSessionEnd = new Intent(this, ActivityReadingSessionEnd.class);
    intentReadingSessionEnd.putExtra(IntentKeys.LOCAL_READING, mLocalReading);
    intentReadingSessionEnd.putExtra(IntentKeys.SESSION_LENGTH_MS, elapsedMilliseconds);
    startActivityForResult(intentReadingSessionEnd, ActivityCodes.CREATE_PING);
  }

  public void exitToBookInfoScreen(LocalReading localReading) {
    Intent intentEditBook = new Intent(this, ActivityAddBook.class);
    intentEditBook.putExtra(IntentKeys.LOCAL_READING, localReading);
    intentEditBook.putExtra(IntentKeys.FROM_READING_SESSION, true);
    startActivityForResult(intentEditBook, ActivityCodes.REQUEST_EDIT_BOOK);
  }

  public void exitToCreateHighlightScreen() {
    Intent activityAddHighlight = new Intent(this, ActivityHighlight.class);
    activityAddHighlight.putExtra(IntentKeys.LOCAL_READING, mLocalReading);
    startActivityForResult(activityAddHighlight, ActivityCodes.CREATE_HIGHLIGHT);
  }

  /**
   * Exits to the home activity with correct result information.
   */
  public void exitToHomeScreen() {
    if(hasSessionStarted()) {
      SessionTimer sessionTimer = mBookFragmentAdapter.getReadingState();
      sessionTimer.pause();
      finishWithResult(ActivityCodes.RESULT_CANCELED, mLocalReading.id, sessionTimer);
      toast("Pausing " + mLocalReading.title);
    } else {
      finishWithResult(ActivityCodes.RESULT_OK);
    }
  }

  public void finishWithResult(int resultCode) {
    finishWithResult(resultCode, -1, null);
  }

  public void finishWithResult(int resultCode, int localReadingId, SessionTimer sessionTimer) {
    if(localReadingId < 1 || sessionTimer == null) {
      Log.v(TAG, "Finishing without extra information");
      setResult(resultCode);
    } else {
      Log.v(TAG, String.format("Finishing with reading id: %d and session timer: %s", localReadingId, sessionTimer.toString()));
      Intent resultIntent = new Intent();
      resultIntent.putExtra(IntentKeys.READING_SESSION_STATE, sessionTimer);
      resultIntent.putExtra(IntentKeys.READING_SESSION_READING_ID, localReadingId);
      setResult(resultCode, resultIntent);
    }
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
      return localReading == null || localSessions == null || localHighlights == null;
    }
  }

}

