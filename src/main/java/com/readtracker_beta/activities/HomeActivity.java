package com.readtracker_beta.activities;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import com.j256.ormlite.dao.Dao;
import com.readtracker_beta.ApplicationReadTracker;
import com.readtracker_beta.IntentKeys;
import com.readtracker_beta.R;
import com.readtracker_beta.ReadmillTransferIntent;
import com.readtracker_beta.db.LocalReading;
import com.readtracker_beta.db.LocalSession;
import com.readtracker_beta.fragments.HomeFragmentAdapter;
import com.readtracker_beta.interfaces.LocalReadingInteractionListener;
import com.readtracker_beta.support.ReadmillApiHelper;
import com.readtracker_beta.support.ReadmillSyncStatusUIHandler;
import com.readtracker_beta.support.SessionTimer;
import com.readtracker_beta.tasks.ReadmillSyncAsyncTask;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static com.readtracker_beta.support.ReadmillSyncStatusUIHandler.SyncStatus;
import static com.readtracker_beta.support.ReadmillSyncStatusUIHandler.SyncUpdateHandler;

public class HomeActivity extends ReadTrackerActivity {

  private static ImageButton mButtonAddBook;
  private static MenuItem mMenuReadmillSync;
  private static ViewPager mPagerHomeActivity;

  private static final int MENU_SYNC_BOOKS = 1;
  private static final int MENU_SETTINGS = 2;

  private ReadmillSyncAsyncTask mReadmillSyncTask;

  // Fragment adapter that manages the reading list fragments
  HomeFragmentAdapter mHomeFragmentAdapter;

  // Handles UI handling of Readmill Sync process
  private ReadmillSyncStatusUIHandler mSyncStatusHandler;

  // Keep a reference to the active session so the user can go back to it
  private static SessionTimer mActiveSessionTimer;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    boolean cameFromSignIn = getIntent().getBooleanExtra(IntentKeys.SIGNED_IN, false);

    // Show welcome screen for first time users
    if(getApp().getFirstTimeFlag() || (getCurrentUser() == null && !cameFromSignIn)) {
      getApp().signOut();
      finish();
      return;
    }

    setContentView(R.layout.activity_home);

    bindViews();

    // Set correct font of header
    applyRoboto(R.id.textHeader);

    // Initialize the adapter with empty list of readings (populated later)
    mHomeFragmentAdapter = new HomeFragmentAdapter(getSupportFragmentManager(), new ArrayList<LocalReading>());

    mPagerHomeActivity.setAdapter(mHomeFragmentAdapter);

    mPagerHomeActivity.setCurrentItem(mHomeFragmentAdapter.getDefaultPage());

    // Handler for showing sync status
    mSyncStatusHandler = new ReadmillSyncStatusUIHandler(R.id.stub_sync_progress, this, new SyncUpdateHandler() {
      @Override public void onReadingUpdate(LocalReading localReading) {
        mHomeFragmentAdapter.put(localReading);
      }

      @Override public void onSyncComplete(SyncStatus status) {
        Log.d(TAG, "Sync is complete with " + status);
        toggleSyncMenuOption(true);
        mReadmillSyncTask = null;

        if(status == SyncStatus.INVALID_TOKEN) {
          toastLong("An error occurred, which requires you to sign in to Readmill again.\nSorry about that.");
          getApp().signOut();
          finish();
        }
      }

      @Override public void onReadingDelete(int localReadingId) {
        mHomeFragmentAdapter.removeReadingsWithId(localReadingId);
        mHomeFragmentAdapter.refreshFragments();
      }
    });

    bindEvents();

    if(cameFromSignIn && getCurrentUser() != null) {
      Log.d(TAG, "Fresh from sign in, doing initial full sync.");
      sync(true);
    }
  }

  @Override
  protected void onPostCreate(Bundle savedInstanceState) {
    super.onPostCreate(savedInstanceState);
    Log.d(TAG, "onPostCreate ReadingList");

    // This is in onPostCreate instead of onCreate to avoid issues with un-dismissible dialogs
    // (as suggested at: http://stackoverflow.com/questions/891451/android-dialog-does-not-dismiss-the-dialog)
    refreshReadingList();
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    // Abort any ongoing readmill sync
    if(mReadmillSyncTask != null) {
      mReadmillSyncTask.cancel(true);
      mReadmillSyncTask = null;
    }
  }

  @Override
  protected void onResume() {
    super.onResume();
    Log.d(TAG, "Registering data receiver for readmill sync");
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    Log.d(TAG, "Creating Options menu for reading list");

    mMenuReadmillSync = menu.add(0, MENU_SYNC_BOOKS, 1, "Sync list with Readmill");
    mMenuReadmillSync.setTitleCondensed("Sync");

    MenuItem menuSettings = menu.add(0, MENU_SETTINGS, 3, "Settings");
    menuSettings.setTitleCondensed("Settings");
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    int clickedId = item.getItemId();

    switch(clickedId) {
      case MENU_SYNC_BOOKS:
        sync(false);
        break;
      case MENU_SETTINGS:
        exitToPreferences();
      default:
        return false;
    }

    return true;
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    switch(resultCode) {
      case ActivityCodes.RESULT_CANCELED:
        // Save the canceled reading state so the user can get back to it
        if(data != null) {
          mActiveSessionTimer = data.getParcelableExtra(IntentKeys.READING_SESSION_STATE);
          Log.v(TAG, "Cancelled with session timer: " + mActiveSessionTimer);
        }
        break;
      case ActivityCodes.RESULT_OK:
        // Refresh the list of readings after a session, and start a sync
        // with Readmill to send the new data
        if(requestCode == ActivityCodes.REQUEST_READING_SESSION ||
          requestCode == ActivityCodes.REQUEST_ADD_BOOK) {
          Log.v(TAG, "Result OK from :" + requestCode);
          refreshReadingList();
          sync(false);
        }
        break;
      case ActivityCodes.RESULT_SIGN_OUT:
        if(requestCode == ActivityCodes.SETTINGS) {
          getApp().signOut();
          finish();
        }
        break;
    }
  }

  @Override
  public boolean onSearchRequested() {
    startActivityForResult(new Intent(this, BookSearchActivity.class), ActivityCodes.REQUEST_ADD_BOOK);
    return true;
  }

  private void bindViews() {
    mButtonAddBook = (ImageButton) findViewById(R.id.buttonAddBook);
    mPagerHomeActivity = (ViewPager) findViewById(R.id.pagerHomeActivity);
  }

  private void bindEvents() {
    mButtonAddBook.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View view) { exitToBookSearch(); }
    });

    mHomeFragmentAdapter.setLocalReadingInteractionListener(new LocalReadingInteractionListener() {
      @Override public void onLocalReadingClicked(LocalReading localReading) {
        exitToActivityBook(localReading.id);
      }
    });
  }

  /**
   * Initiates a sync with Readmill.
   */
  private void sync(boolean fullSync) {
    if(!shouldSync()) {
      return;
    }
    Log.i(TAG, "Performing " + (fullSync ? "full" : "partial") + " sync");

    startService(new Intent(this, ReadmillTransferIntent.class));

    Log.i(TAG, "Starting ASyncTask for Syncing Readmill Readings");

    ReadmillApiHelper api = ApplicationReadTracker.getReadmillApiHelper();
    mReadmillSyncTask = new ReadmillSyncAsyncTask(mSyncStatusHandler, api, fullSync);

    // Prevent starting another sync while this is ongoing
    toggleSyncMenuOption(false);

    long readmillUserId = getCurrentUserId();
    mReadmillSyncTask.execute(readmillUserId);
  }

  /**
   * Determines if it is appropriate to start a sync with Readmill.
   *
   * @return true if a sync should be started, false otherwise
   */
  private boolean shouldSync() {
    if(!isNetworkAvailable()) {
      Log.i(TAG, "No internet connection - skipping sync");
      return false;
    }

    if(mReadmillSyncTask != null) {
      Log.d(TAG, "Sync already in progress - skipping sync.");
      return false;
    }

    if(getCurrentUser() == null) {
      Log.i(TAG, "No user signed in - skipping sync");
      return false;
    }
    return true;
  }

  /**
   * Enables the menu option to start a Readmill sync
   *
   * @param enabled the enabled state of the sync menu option
   */
  private void toggleSyncMenuOption(boolean enabled) {
    if(mMenuReadmillSync != null) {
      mMenuReadmillSync.setEnabled(enabled);
    }
  }

  // Private

  private void exitToBookSearch() {
    mActiveSessionTimer = null; // Clear any paused sessions
    startActivityForResult(new Intent(this, BookSearchActivity.class), ActivityCodes.REQUEST_ADD_BOOK);
  }

  private void exitToPreferences() {
    Intent intentSettings = new Intent(this, SettingsActivity.class);
    startActivityForResult(intentSettings, ActivityCodes.SETTINGS);
  }

  private void exitToActivityBook(int localReadingId) {
    Intent intentReadingSession = new Intent(this, BookActivity.class);
    intentReadingSession.putExtra(IntentKeys.READING_ID, localReadingId);

    if(mActiveSessionTimer != null) {
      Log.d(TAG, "Has Active Reading state: " + mActiveSessionTimer);
      if(mActiveSessionTimer.getLocalReadingId() == localReadingId) {
        Log.v(TAG, "Passing active state for reading " + localReadingId + ": " + mActiveSessionTimer);
        intentReadingSession.putExtra(IntentKeys.READING_SESSION_STATE, mActiveSessionTimer);
      }

      mActiveSessionTimer = null;
    }

    startActivityForResult(intentReadingSession, ActivityCodes.REQUEST_READING_SESSION);
  }

  private void refreshReadingList() {
    Log.d(TAG, "Refreshing reading list...");
    getApp().showProgressDialog(this, "Loading book list...");
    (new RefreshBookListTask()).execute(getCurrentUserId());
  }

  private void onFetchedReadings(List<LocalReading> localReadingList) {
    Log.d(TAG, "Listing " + localReadingList.size() + " existing readings");

    mHomeFragmentAdapter.setLocalReadings(localReadingList);

    getApp().clearProgressDialog();
  }

  /**
   * Reloads readings for a given user
   */
  class RefreshBookListTask extends AsyncTask<Long, Void, List<LocalReading>> {

    @Override
    protected List<LocalReading> doInBackground(Long... readmillIds) {
      long readmillUserId = readmillIds[0];
      Log.d(TAG, "Fetching list of readings for user " + readmillUserId + " from database...");
      return loadLocalReadingsForUser(readmillUserId);
    }

    @Override protected void onPostExecute(List<LocalReading> localReadings) {
      onFetchedReadings(localReadings);
    }

    private List<LocalReading> loadLocalReadingsForUser(long readmillUserId) {
      try {
        Dao<LocalReading, Integer> readingDao = ApplicationReadTracker.getReadingDao();
        Dao<LocalSession, Integer> sessionDao = ApplicationReadTracker.getSessionDao();

        ArrayList<LocalReading> localReadings = fetchLocalReadingsForUser(readmillUserId, readingDao);
        return readingsWithPopulateSessionSegments(localReadings, sessionDao);
      } catch(SQLException e) {
        Log.d(TAG, "Failed to get list of existing readings", e);
        return new ArrayList<LocalReading>();
      }
    }

    private ArrayList<LocalReading> fetchLocalReadingsForUser(long readmillUserId, Dao<LocalReading, Integer> dao) throws SQLException {
      return (ArrayList<LocalReading>) dao.queryBuilder()
        .where().eq(LocalReading.READMILL_USER_ID_FIELD_NAME, readmillUserId)
        .and().eq(LocalReading.DELETED_BY_USER_FIELD_NAME, false)
        .query();
    }

    /**
     * Load all reading sessions for each of the given local readings, and use them to set the progress stops array on
     * the reading.
     *
     * @param localReadings List of local readings to set progress stops for
     * @param sessionsDao   DAO from which to load sessions
     * @return the given local readings, with progress stops populated
     * @throws SQLException
     */
    private List<LocalReading> readingsWithPopulateSessionSegments(ArrayList<LocalReading> localReadings, Dao<LocalSession, Integer> sessionsDao) throws SQLException {
      for(LocalReading localReading : localReadings) {
        List<LocalSession> sessions = sessionsDao.queryBuilder()
          .where().eq(LocalSession.READING_ID_FIELD_NAME, localReading.id)
          .query();
        Log.d(TAG, "Got " + sessions.size() + " sessions for " + localReading.toString());
        localReading.setProgressStops(sessions);
      }
      return localReadings;
    }
  }
}
