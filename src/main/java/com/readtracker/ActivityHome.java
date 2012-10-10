package com.readtracker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.DeleteBuilder;
import com.j256.ormlite.stmt.Where;
import com.readtracker.db.LocalHighlight;
import com.readtracker.db.LocalReading;
import com.readtracker.db.LocalSession;
import com.readtracker.helpers.ReadmillSyncStatusUIHandler;
import com.readtracker.interfaces.LocalReadingInteractionListener;
import com.readtracker.readmill.ReadmillApiHelper;
import com.readtracker.tasks.ReadmillSyncAsyncTask;
import com.readtracker.thirdparty.TitlePageIndicator;
import com.readtracker.value_objects.ReadingState;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ActivityHome extends ReadTrackerActivity {

  private static LinearLayout mLayoutBlankState;
  private static Button mButtonSyncReadmill;
  private static Button mButtonAddBook;
  private static MenuItem mMenuReadmillSync;
  private static ViewPager mPagerHomeActivity;

  private static final int MENU_SYNC_BOOKS = 1;
  private static final int MENU_SETTINGS = 2;

  private static final int CONTEXT_MENU_DELETE = 0;

  private BroadcastReceiver mNetworkStateReceiver;
  private DataUpdateReceiver mDataUpdateReceiver;
  private ReadmillSyncAsyncTask mReadmillSyncTask;
  private TitlePageIndicator mTitleHomeActivity;

  // Fragment adapter that manages the reading list fragments
  HomeFragmentAdapter mHomeFragmentAdapter;

  // Handles UI handling of Readmill Sync process
  private ReadmillSyncStatusUIHandler mSyncStatusHandler;

  // Keep a reference to the active session so the user can go back to it
  private static ReadingState mActiveReadingState;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_home);

    bindViews();

    mHomeFragmentAdapter = new HomeFragmentAdapter(getSupportFragmentManager(), new ArrayList<LocalReading>(), new LocalReadingInteractionListener() {
      @Override public void onLocalReadingClicked(LocalReading localReading) {
        if(!localReading.isInteresting()) {
          exitToActivityBook(localReading);
        }
      }
    });

    mPagerHomeActivity.setAdapter(mHomeFragmentAdapter);
    mTitleHomeActivity.setViewPager(mPagerHomeActivity);

    mPagerHomeActivity.setCurrentItem(mHomeFragmentAdapter.getDefaultPage());

    // Handler for showing sync status
    mSyncStatusHandler = new ReadmillSyncStatusUIHandler(R.id.stub_sync_progress, this, new ReadmillSyncStatusUIHandler.SyncUpdateHandler() {
      @Override public void onReadingUpdate(LocalReading localReading) {
        mHomeFragmentAdapter.put(localReading);
      }

      @Override public void onSyncComplete() {
        toggleSyncMenuOption(true);
        mReadmillSyncTask = null;
      }
    });

    bindEvents();

    updateConnectivityState();

    boolean signedIn = getIntent().getBooleanExtra(IntentKeys.SIGNED_IN, false);
    if(signedIn) {
      Log.d(TAG, "After sign in: Starting sync");
      startSyncWithReadmill();
    }
  }

  @Override
  protected void onPostCreate(Bundle savedInstanceState) {
    super.onPostCreate(savedInstanceState);
    Log.d(TAG, "onPostCreate ReadingList");

    // This is in onPostCreate instead of onCreate to avoid issues with un-dismissible dialogs
    // (as suggested at: http://stackoverflow.com/questions/891451/android-dismissdialog-does-not-dismiss-the-dialog)
    refreshReadingList();
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    unregisterReceiver(mNetworkStateReceiver);
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
    if(mDataUpdateReceiver == null) {
      mDataUpdateReceiver = new DataUpdateReceiver();
    }
    IntentFilter intentFilter = new IntentFilter(IntentKeys.READMILL_SYNC_READINGS_COMPLETE);
    registerReceiver(mDataUpdateReceiver, intentFilter);
  }

  @Override
  protected void onPause() {
    super.onPause();
    if(mDataUpdateReceiver != null) unregisterReceiver(mDataUpdateReceiver);
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    Log.d(TAG, "Creating Options menu for reading list");

    mMenuReadmillSync = menu.add(0, MENU_SYNC_BOOKS, 1, "Sync list with Readmill");
    mMenuReadmillSync.setIcon(R.drawable.readmill_sync);
    mMenuReadmillSync.setTitleCondensed("Sync");

    MenuItem menuSettings = menu.add(0, MENU_SETTINGS, 3, "Settings");
    menuSettings.setTitleCondensed("Settings");
    menuSettings.setIcon(android.R.drawable.ic_menu_preferences);

    updateConnectivityState();

    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    int clickedId = item.getItemId();

    switch(clickedId) {
      case MENU_SYNC_BOOKS:
        startSyncWithReadmill();
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
          mActiveReadingState = data.getParcelableExtra(IntentKeys.READING_SESSION_STATE);
        }
        break;
      case ActivityCodes.RESULT_OK:
        // Refresh the list of readings after a session, and start a sync
        // with Readmill to send the new data
        if(requestCode == ActivityCodes.REQUEST_READING_SESSION ||
            requestCode == ActivityCodes.REQUEST_ADD_BOOK) {
          refreshReadingList();
          startSyncWithReadmill();
        }
        break;
      case ActivityCodes.RESULT_SIGN_OUT:
        if(requestCode == ActivityCodes.SETTINGS) {
          getApp().clearSettings();
          exitToSignInScreen();
        }
        break;
    }
  }

  @Override
  public boolean onSearchRequested() {
    startActivityForResult(new Intent(this, ActivityBookSearch.class), ActivityCodes.REQUEST_ADD_BOOK);
    return true;
  }

  private void bindViews() {
    mLayoutBlankState = (LinearLayout) findViewById(R.id.readingListBlankState);
    mButtonSyncReadmill = (Button) findViewById(R.id.buttonSyncReadmill);
    mButtonAddBook = (Button) findViewById(R.id.buttonAddBook);
    mPagerHomeActivity = (ViewPager) findViewById(R.id.pagerHomeActivity);
    mTitleHomeActivity = (TitlePageIndicator) findViewById(R.id.titleHomeActivity);
  }

  private void bindEvents() {
    mButtonAddBook.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        startAddBookScreen();
      }
    });

    mButtonSyncReadmill.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        startSyncWithReadmill();
      }
    });

    mNetworkStateReceiver = new BroadcastReceiver() {
      @Override
      public void onReceive(Context context, Intent intent) {
        ActivityHome.this.updateConnectivityState();
      }
    };

    IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
    registerReceiver(mNetworkStateReceiver, filter);
  }

  private void startAddBookScreen() {
    startActivityForResult(new Intent(this, ActivityBookSearch.class), ActivityCodes.REQUEST_ADD_BOOK);
  }

  private void exitToPreferences() {
    Intent intentSettings = new Intent(this, ActivitySettings.class);
    startActivityForResult(intentSettings, ActivityCodes.SETTINGS);
  }

  /**
   * Initiates a full sync with Readmill.
   * Pushes new reading sessions, readings and highlights to Readmill.
   */
  private void startSyncWithReadmill() {
    if(!isNetworkAvailable() || mReadmillSyncTask != null) {
      Log.d(TAG, "No internet connection or sync already in progress. Skipping sync.");
      return;
    }

    if(getCurrentUser() == null) {
      Log.i(TAG, "Not logged in, skipping sync");
      return;
    }

    startService(new Intent(this, ReadmillTransferIntent.class));

    Log.i(TAG, "Starting AsyncTask for Syncing Readmill Readings");
    mButtonSyncReadmill.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_out));

    ReadmillApiHelper api = ApplicationReadTracker.getReadmillApi();
    mReadmillSyncTask = new ReadmillSyncAsyncTask(mSyncStatusHandler, api);

    // Prevent starting another sync while this is ongoing
    toggleSyncMenuOption(false);

    long readmillUserId = getCurrentUserId();
    mReadmillSyncTask.execute(readmillUserId);
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

  private void exitToSignInScreen() {
    Intent intentWelcome = new Intent(this, ActivityWelcome.class);
    intentWelcome.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
    startActivity(intentWelcome);
    finish();
  }

  private void removeFromDevice(LocalReading localReading) {
    Log.d(TAG, "Removing Reading Data from device: " + localReading.title);
    (new DeleteReadingTask()).execute(localReading);
  }

  private void onPostRemovedLocalReading(int deletedLocalReadingId) {
    if(deletedLocalReadingId == -1) {
      Log.i(TAG, "Nothing deleted");
      return;
    }

    //    LocalReading deletedItem = mReadingListAdapter.getItemByLocalReadingId(deletedLocalReadingId);
    //    if(deletedItem != null) {
    //      ApplicationReadTracker.getDrawableManager().deletePersistedCover(deletedItem.coverURL);
    //      mReadingListAdapter.remove(deletedItem);
    //    } else {
    //      Log.d(TAG, "Could not find LocalReading in list after being deleted id:" + deletedLocalReadingId);
    //    }
  }

  private void exitToActivityBook(LocalReading localReading) {
    Intent intentReadingSession = new Intent(this, ActivityBook.class);
    intentReadingSession.putExtra(IntentKeys.READING_ID, localReading.id);

    // Resume the session if going back to the previously read
    if(mActiveReadingState != null) {
      Log.d(TAG, "Has Active Reading state: " + mActiveReadingState);
    }

    if(mActiveReadingState != null && mActiveReadingState.getLocalReadingId() == localReading.id) {
      Log.d(TAG, "Passing active state for reading " + localReading.id + ": " + mActiveReadingState);
      intentReadingSession.putExtra(IntentKeys.READING_SESSION_STATE, mActiveReadingState);
    }

    // Jump to the read page directly if the local reading is ready to be read
    if(localReading.isActive()) {
      intentReadingSession.putExtra(IntentKeys.STARTING_PAGE, ActivityBook.PAGE_READING);
    } else {
      intentReadingSession.putExtra(IntentKeys.STARTING_PAGE, ActivityBook.PAGE_HISTORY);
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

  private void updateConnectivityState() {
    if(isNetworkAvailable()) {
      if(mButtonAddBook != null) {
        mButtonAddBook.setVisibility(View.VISIBLE);
      }
    } else {
      if(mButtonAddBook != null) {
        mButtonAddBook.setVisibility(View.INVISIBLE);
      }
    }
  }

  /**
   * Reloads readings for a given user
   */
  class RefreshBookListTask extends AsyncTask<Long, Void, List<LocalReading>> {
    @Override
    protected List<LocalReading> doInBackground(Long... readmillIds) {
      if(readmillIds.length != 1) {
        Log.d(TAG, "Have to receive an array with exactly one readmill user id");
        return null;
      }
      long readmillUserId = readmillIds[0];
      Log.d(TAG, "Fetching list of readings for user " + readmillUserId + " from database...");
      try {
        Dao<LocalReading, Integer> dao = ApplicationReadTracker.getReadingDao();
        Where<LocalReading, Integer> stmt = dao.queryBuilder().where().eq(LocalReading.READMILL_USER_ID_FIELD_NAME, readmillUserId);
        return stmt.query();
      } catch(SQLException e) {
        Log.d(TAG, "Failed to get list of existing readings", e);
        return new ArrayList<LocalReading>();
      }
    }

    @Override
    protected void onPostExecute(List<LocalReading> localReadings) {
      onFetchedReadings(localReadings);
    }
  }

  /**
   * Handles received messages from ReadmillTransferIntent
   */
  private class DataUpdateReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
      if(intent.getAction().equals(IntentKeys.READMILL_SYNC_READINGS_COMPLETE)) {
        boolean syncSuccess = intent.getBooleanExtra(IntentKeys.FINISHED_WITH_SUCCESS, false);

        if(syncSuccess) {
          Toast.makeText(ActivityHome.this, "Synced!", Toast.LENGTH_SHORT).show();
        } else {
          Toast.makeText(ActivityHome.this, "Failed to sync with Readmill", Toast.LENGTH_LONG).show();
          return;
        }

        refreshReadingList();
      }
    }
  }

  private class DeleteReadingTask extends AsyncTask<LocalReading, Void, Integer> {

    @Override
    protected Integer doInBackground(LocalReading... localReadings) {
      LocalReading localReading = localReadings[0];
      Log.d(TAG, "Attempting to delete " + localReading.toString());
      try {
        Log.i(TAG, "Deleting reading periods for reading " + localReading.id);
        DeleteBuilder<LocalSession, Integer> queryBuilderReadings = ApplicationReadTracker.getSessionDao().deleteBuilder();
        queryBuilderReadings.where().eq(LocalSession.READING_ID_FIELD_NAME, localReading.id);
        ApplicationReadTracker.getSessionDao().delete(queryBuilderReadings.prepare());
        Log.i(TAG, "Deleting reading data with id: " + localReading.id);

        Log.i(TAG, "Deleting reading highlight for reading " + localReading.id);
        DeleteBuilder<LocalHighlight, Integer> queryBuilderHighlights = ApplicationReadTracker.getHighlightDao().deleteBuilder();
        queryBuilderHighlights.where().eq(LocalHighlight.READING_ID_FIELD_NAME, localReading.id);
        ApplicationReadTracker.getHighlightDao().delete(queryBuilderHighlights.prepare());

        ApplicationReadTracker.getReadingDao().delete(localReading);
        return localReading.id;
      } catch(SQLException e) {
        Log.d(TAG, "Failed to remove reading data from device", e);
      }
      return -1;
    }

    @Override
    protected void onPostExecute(Integer LocalReadingId) {
      onPostRemovedLocalReading(LocalReadingId);
    }
  }

}
