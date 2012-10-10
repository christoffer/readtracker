package com.readtracker;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import com.j256.ormlite.dao.Dao;
import com.readtracker.customviews.ViewBindingBookHeader;
import com.readtracker.db.LocalHighlight;
import com.readtracker.db.LocalReading;
import com.readtracker.db.LocalSession;
import com.readtracker.thirdparty.SafeViewFlipper;
import com.readtracker.thirdparty.TitlePageIndicator;
import com.readtracker.value_objects.ReadingState;

import java.sql.SQLException;
import java.util.ArrayList;

/**
 * Fragmented screen for browsing and reading a book
 */
public class ActivityBook extends ReadTrackerActivity {
  private static ViewGroup mLayoutHeader;

  private static TextView mTextBookmarkPage;
  private static ImageView mImageBookmark;
  private static Button mButtonAddHighlight;

  // Toggle visibility of the add highlight button
  private static SafeViewFlipper mFlipperActionButtons;

  private LocalReading mLocalReading;
  private ArrayList<LocalSession> mLocalSessions;
  private ArrayList<LocalHighlight> mLocalHighlights;

  private BookFragmentAdapter mBookFragmentAdapter;
  private ViewPager mViewPagerReading;
  private TitlePageIndicator mPageTitleIndicator;

  private boolean mIsDirty;
  private boolean mLocalReadingDirty;
  private boolean mManualShutdown;
  private int mStartingPage;

  // Fragment pages
  public static final int PAGE_HISTORY = 0;
  public static final int PAGE_READING = 1;
  public static final int PAGE_HIGHLIGHT = 2;

  public void onCreate(Bundle in) {
    super.onCreate(in);
    Log.d(TAG, "onCreate()");
    setContentView(R.layout.activity_book);

    if(in != null) {
      Log.d(TAG, "unfreezing state");
      mStartingPage = in.getInt(IntentKeys.STARTING_PAGE, PAGE_HISTORY);
    } else {
      if(getIntent() != null) {
        Log.d(TAG, "Started from intent");
        mStartingPage = getIntent().getIntExtra(IntentKeys.STARTING_PAGE, PAGE_READING);
      }
    }

    bindViews();

    setupActionBar();

    ReadingState readingState = getIntent().getExtras().getParcelable(IntentKeys.READING_SESSION_STATE);

    // Load information from database
    int readingId = getIntent().getExtras().getInt(IntentKeys.READING_ID);
    reloadLocalData(readingId);
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    Log.d(TAG, "freezing state");
    if(mViewPagerReading != null) {
      outState.putInt(IntentKeys.STARTING_PAGE, mViewPagerReading.getCurrentItem());
    }
  }

  @Override
  public void onBackPressed() {
    if(isDirty()) {
      // Do nothing when the session is dirty
      ReadingState readingState = mBookFragmentAdapter.getReadingState();
      finishWithResult(ActivityCodes.RESULT_CANCELED, readingState);
      toast("Pausing " + mLocalReading.title);
    } else {
      // Maybe set the result code to trigger a reading refresh
      int result = mLocalReadingDirty ? ActivityCodes.RESULT_OK : ActivityCodes.RESULT_CANCELED;
      finishWithResult(result);
    }
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    switch(requestCode) {
      case ActivityCodes.CREATE_PING:
        // Set result to OK to state that something was changed
        if(resultCode == RESULT_OK) {
          Log.d(TAG, "Created ping: Falling through");
          finishWithResult(ActivityCodes.RESULT_OK);
        }
        break;
      case ActivityCodes.CREATE_HIGHLIGHT:
        if(resultCode == RESULT_OK) {
          mStartingPage = PAGE_HIGHLIGHT;
          // Use a provided reading id since localReading might have been destroyed
          int updateReadingId = data.getIntExtra(IntentKeys.READING_ID, -1);
          if(updateReadingId == -1) {
            Log.e(TAG, "Highlight Activity did not provide LocalReading id");
            finish();
          }
          reloadLocalData(updateReadingId);
        }
        break;
    }
    super.onActivityResult(requestCode, resultCode, data);
  }

  /**
   * Loads local reading data for a given reading.
   * Loads the LocalReading, LocalSessions and LocalHighlights.
   *
   * @param readingId    id of LocalReading to load data for.
   */
  private void reloadLocalData(int readingId) {
    try {
      (new LoadLocalReadingAndSessionsTask()).execute(readingId);
    } catch(SQLException e) {
      Log.e(TAG, "Failed to reload local reading data", e);
      toast("Book data corrupted");
      finishWithResult(ActivityCodes.RESULT_CANCELED);
    }
  }

  public void setDirty(boolean dirty) {
    mIsDirty = dirty;
  }

  private void exitToHighlightScreen() {
    Intent intentAddHighlight = new Intent(ActivityBook.this, ActivityHighlight.class);
    intentAddHighlight.putExtra(IntentKeys.LOCAL_READING, mLocalReading);
    startActivityForResult(intentAddHighlight, ActivityCodes.CREATE_HIGHLIGHT);
  }

  private void bindViews() {
    mLayoutHeader = (ViewGroup) findViewById(R.id.layoutHeaderBookActivity);

    mTextBookmarkPage = (TextView) findViewById(R.id.textBookmarkPage);
    mImageBookmark = (ImageView) findViewById(R.id.imageBookmark);

    mFlipperActionButtons = (SafeViewFlipper) findViewById(R.id.flipperActionButtons);
    mButtonAddHighlight = (Button) findViewById(R.id.buttonAddHighlight);

    mPageTitleIndicator = (TitlePageIndicator) findViewById(R.id.titleBookActivity);
    mViewPagerReading = (ViewPager) findViewById(R.id.pagerBookActivity);
  }

  /**
   * Called when the local information about a reading has finished being laoded
   * from the database.
   *
   * @param bundle The result bundle with all local data
   */
  public void onLocalReadingLoaded(LocalReadingBundle bundle) {
    Log.i(TAG, "Loaded LocalReading");

    if(mManualShutdown) {
      Log.w(TAG, "Activity is shutting down - not initilizing");
      return;
    }

    if(bundle.isValid()) {
      toastLong("An error occurred while loading data for this book");
      finishWithResult(ActivityCodes.RESULT_CANCELED);
      return;
    }

    mLocalReading = bundle.localReading;
    mLocalSessions = bundle.localSessions;
    mLocalHighlights = bundle.localHighlights;

    Log.d(TAG, "Got " + mLocalSessions.size() + " reading sessions and " + mLocalHighlights.size() + " highlights");

    // Book info
    ViewBindingBookHeader.bind(this, mLocalReading);

    setBookMarkPage(mLocalReading.currentPage);

    setupFragments(bundle);

    mFlipperActionButtons.setVisibility(View.VISIBLE);
    animateBookBar();

    Log.i(TAG, "Initialized for reading with id:" + mLocalReading.id);
  }

  private boolean isDirty() {
    return mIsDirty;
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

    ReadingState activeReadingState = getIntent().getExtras().getParcelable(IntentKeys.READING_SESSION_STATE);
    mBookFragmentAdapter.setReadingState(activeReadingState);

    mViewPagerReading.setAdapter(mBookFragmentAdapter);
    mViewPagerReading.setOffscreenPageLimit(2);

    //Bind the title indicator to the adapter
    mPageTitleIndicator.setViewPager(mViewPagerReading);

    int page = 0;
    switch(mStartingPage) {
      case PAGE_HISTORY:
        page = mBookFragmentAdapter.getHistoryPageIndex();
        break;
      case PAGE_READING:
        page = mBookFragmentAdapter.getReadPageIndex();
        break;
      case PAGE_HIGHLIGHT:
        page = mBookFragmentAdapter.getHighlightPageIndex();
        break;
    }
    mViewPagerReading.setCurrentItem(page, false);
  }

  private void setupActionBar() {
    mButtonAddHighlight.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View view) { exitToHighlightScreen(); }
    });

    // Book bar + history button
    mPageTitleIndicator.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
      @Override public void onPageSelected(int position) {
        Log.i(TAG, "Selected page " + position);
        boolean isOnHighlightPage = position == mBookFragmentAdapter.getHighlightPageIndex();
        int highlightButtonState = isOnHighlightPage ? 1 : 0;
        // Show add highlight button when on highlight page, hide it when
        // leaving for another page
        if(mFlipperActionButtons.getDisplayedChild() != highlightButtonState) {
          mFlipperActionButtons.setDisplayedChild(highlightButtonState);
        }
      }

      @Override public void onPageScrollStateChanged(int i) { }

      @Override public void onPageScrolled(int p, float po, int pop) { }
    });
  }

  private void setBookMarkPage(long currentPage) {
    if(currentPage > 0) {
      mTextBookmarkPage.setText("" + mLocalReading.currentPage);
    } else {
      mTextBookmarkPage.setText("");
    }
  }

  // Private

  private void animateBookBar() {
    boolean shouldShowBookmark = mLocalReading.currentPage > 0;
    if(shouldShowBookmark) {
      Log.d(TAG, "Showing with bookmark for page: " + mLocalReading.currentPage);
      Animation bookmarkAppear = AnimationUtils.loadAnimation(this, R.anim.bookmark_appear);
      Animation fadeIn = AnimationUtils.loadAnimation(this, android.R.anim.fade_in);
      fadeIn.setStartOffset(318);
      bookmarkAppear.setStartOffset(200);

      mImageBookmark.startAnimation(bookmarkAppear);
      mTextBookmarkPage.startAnimation(fadeIn);

      mImageBookmark.setVisibility(View.VISIBLE);
      mTextBookmarkPage.setVisibility(View.VISIBLE);
    } else {
      Log.d(TAG, "Showing without bookmark");
      mTextBookmarkPage.setVisibility(View.GONE);
      mImageBookmark.setVisibility(View.GONE);
    }

    Animation fadeIn = AnimationUtils.loadAnimation(this, android.R.anim.fade_in);
    mLayoutHeader.startAnimation(fadeIn);
    mLayoutHeader.setVisibility(View.VISIBLE);
    mFlipperActionButtons.setVisibility(View.VISIBLE);
  }

  public void exitToSessionEndScreen(long elapsedMilliseconds) {
    Intent intentReadingSessionEnd = new Intent(this, ActivityReadingSessionEnd.class);
    intentReadingSessionEnd.putExtra(IntentKeys.LOCAL_READING, mLocalReading);
    intentReadingSessionEnd.putExtra(IntentKeys.SESSION_LENGTH_MS, elapsedMilliseconds);
    startActivityForResult(intentReadingSessionEnd, ActivityCodes.CREATE_PING);
  }

  public void finishWithResult(int resultCode) {
    finishWithResult(resultCode, null);
  }

  public void finishWithResult(int resultCode, ReadingState readingState) {
    if(readingState == null) {
      setResult(resultCode);
    } else {
      Log.d(TAG, "Finishing with result and reading state: " + readingState.toString());
      Intent resultIntent = new Intent();
      resultIntent.putExtra(IntentKeys.READING_SESSION_STATE, readingState);
      setResult(resultCode, resultIntent);
    }
    mManualShutdown = true;
    ReadingStateHandler.clear();
    finish();
  }

  /**
   * Callback from fragment informing that number of pages of the Local reading
   * has been updated.
   */
  public void onLocalReadingChanged() {
    mLocalReadingDirty = true;
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

