package com.readtracker.android.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.view.PagerTabStrip;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.readtracker.android.BuildConfig;
import com.readtracker.android.IntentKeys;
import com.readtracker.android.R;
import com.readtracker.android.db.Book;
import com.readtracker.android.fragments.BookFragmentAdapter;
import com.readtracker.android.support.SessionTimer;
import com.squareup.otto.Produce;

import static com.readtracker.android.fragments.BookFragmentAdapter.Page;

/** Browse data for, and interact with, a book. */
public class BookActivity extends BookBaseActivity implements EndSessionDialog.EndSessionDialogListener, SessionTimer.SessionTimerListener {
  protected static final String TAG = BookActivity.class.getSimpleName();

  private static final int NO_GROUP = 0;
  private static final int MENU_EDIT_BOOK_SETTINGS = 1;

  public static final int REQUEST_EDIT_PAGE_NUMBERS = 1;
  public static final int REQUEST_ADD_QUOTE = 2;
  public static final int REQUEST_BOOK_SETTINGS = 3;
  public static final int REQUEST_FINISH_BOOK = 4; // used by ReadFragment

  private ViewPager mViewPager;
  private PagerTabStrip mPagerTabStrip;

  private Page mInitialFragmentPage;
  private SessionTimer mSessionTimer = new SessionTimer();

  public void onCreate(Bundle in) {
    super.onCreate(in);

    setContentView(R.layout.book_activity);
    mViewPager = (ViewPager) findViewById(R.id.fragment_view_pager);
    mPagerTabStrip = (PagerTabStrip) findViewById(R.id.pager_tab_strip);

    mPagerTabStrip.setVisibility(View.INVISIBLE);

    // Load information from database
    loadBookFromIntent();
  }

  @Override protected void onStart() {
    super.onStart();
    if(!mSessionTimer.loadFromPreferences(getPreferences())) {
      Log.d(TAG, "No stored sessions found, resetting timer");
      mSessionTimer.reset();
    } else {
      Log.d(TAG, "Using reset timer state: " + mSessionTimer);
    }

    mSessionTimer.setOnTimerListener(this);
  }

  @Override protected void onStop() {
    super.onStop();
    if(isFinishing()) {
      mSessionTimer.saveToPreferences(getPreferences());
    } else {
      mSessionTimer.clearFromPreferences(getPreferences());
    }
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    Log.d(TAG, "freezing state");
    if(mViewPager != null) {
      outState.putInt(IntentKeys.INITIAL_FRAGMENT_PAGE, mViewPager.getCurrentItem());
    }
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    Log.v(TAG, "onActivityResult()");
    if(resultCode == ActivityCodes.RESULT_CANCELED) {
      return;
    }

    Log.v(TAG, "onActivityResult: requestCode: " + requestCode + ", resultCode: " + resultCode);
    switch(requestCode) {
      case REQUEST_EDIT_PAGE_NUMBERS:
        if(resultCode == RESULT_OK) {
          Log.d(TAG, "Came back from editing page number");
          mInitialFragmentPage = Page.READING;
          loadBookFromIntent();
        }
        break;
      case REQUEST_ADD_QUOTE:
        if(resultCode == RESULT_OK) {
          Log.d(TAG, "Came back from adding a quote");
          mInitialFragmentPage = Page.QUOTES;
          loadBookFromIntent();
        }
        break;
      case REQUEST_BOOK_SETTINGS:
        if(resultCode == ActivityCodes.RESULT_REQUESTED_BOOK_SETTINGS) {
          // TODO Replace this with event
          final Book book = getBook();
          if(book != null) {
            // exitToBookEditScreen(book);
          } else {
            Log.w(TAG, "Ignoring request for book settings, book is null");
          }
        } else if(resultCode == ActivityCodes.RESULT_DELETED_BOOK) {
          // shutdownWithResult(RESULT_OK);
        } else if(resultCode == ActivityCodes.RESULT_OK) {
          Log.d(TAG, "Came back from changing the book settings");
          loadBookFromIntent();
        }
        break;
      case REQUEST_FINISH_BOOK:
        if(resultCode == RESULT_OK) {
          Log.v(TAG, "Finished book");
          String closingRemark = null;
          if(data.hasExtra(FinishBookActivity.KEY_CLOSING_REMARK)) {
            closingRemark = data.getStringExtra(FinishBookActivity.KEY_CLOSING_REMARK);
          }
          finishBookWithClosingRemark(closingRemark);
        }
        break;
    }
    super.onActivityResult(requestCode, resultCode, data);
  }

  @Override public boolean onCreateOptionsMenu(Menu menu) {
    menu.add(NO_GROUP, MENU_EDIT_BOOK_SETTINGS, 0, "Edit book settings");
    return true;
  }

  @Override protected void onBookLoaded(Book book) {
    Log.v(TAG, "Book loaded: " + book);
    postEvent(new BookLoadedEvent(book));
    setupFragments(book);
    mPagerTabStrip.setVisibility(View.VISIBLE);
  }

  @Override protected boolean shouldLoadRelatedBookData() {
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch(item.getItemId()) {
      case MENU_EDIT_BOOK_SETTINGS:
        // exitToBookSettings();
        break;
      default:
        return false;
    }
    return true;
  }

  @Produce public BookLoadedEvent produceBookLoadedEvent() {
    return new BookLoadedEvent(getBook());
  }

  @Override public void onSessionEndWithNewPosition(final float position) {
    runWhenBookIsReady(new Runnable() {
      @Override public void run() {
        Log.d(TAG, "Ending session with new position" + position);
        Float previousPosition = getBook().getCurrentPosition();

        // TODO create session

        final Book book = getBook();
        book.setCurrentPosition(position);
        book.setCurrentPositionTimestamp(System.currentTimeMillis());

        saveAndFinish();
      }
    });
  }

  @Override public void onSessionEndWithFinish() {
    Log.d(TAG, "Ending session with finish");
    Intent finishReading = new Intent(this, FinishBookActivity.class);
    finishReading.putExtra(BookBaseActivity.KEY_BOOK_ID, getBook().getId());
    startActivityForResult(finishReading, BookActivity.REQUEST_FINISH_BOOK);
  }

  @Override public void onSessionTimerStarted() {

  }

  @Override public void onSessionTimerStopped() {
    postEvent(new SessionTimerChangedEvent(mSessionTimer));
  }

  private void finishBookWithClosingRemark(String closingRemark) {
    final Book book = getBook();
    if(BuildConfig.DEBUG) {
      Log.d(TAG, String.format("Finishing with closing remark: %s", closingRemark));
    }
    if(!TextUtils.isEmpty(closingRemark)) {
      book.setClosingRemark(closingRemark);
    }
    book.setState(Book.State.Finished);
    book.setCurrentPositionTimestamp(System.currentTimeMillis());

    saveAndFinish();
  }

  private void setupFragments(Book book) {
    Page[] pages;
    if(book.isInState(Book.State.Finished)) {
      pages = new Page[] { Page.SUMMARY, Page.QUOTES };
    } else {
      pages = new Page[] { Page.SUMMARY, Page.READING, Page.QUOTES };
    }

    BookFragmentAdapter bookFragmentAdapter = new BookFragmentAdapter(getApplicationContext(), getSupportFragmentManager(), pages);

    mViewPager.setAdapter(bookFragmentAdapter);

    final Page initialPage = mInitialFragmentPage == null ? Page.READING : mInitialFragmentPage;
    mViewPager.setCurrentItem(bookFragmentAdapter.getPageIndex(initialPage), false);
  }

  private void saveAndFinish() {
    // Perform database operations on the main thread here, since it's very quick operation
    // and because we actually want the UI to be locked until we have saved since we're exiting
    // after.
    final boolean success = getDatabaseManager().save(getBook()) != null;
    if(success) {
      setResult(RESULT_OK);
      finish();
    } else {
      toast(R.string.book_error_updating);
    }
  }

  /** Signlas that the SessionTimer has changed in some way. */
  public static class SessionTimerChangedEvent {
    private final SessionTimer sessionTimer;

    public SessionTimerChangedEvent(SessionTimer sessionTimer) {
      this.sessionTimer = sessionTimer;
    }

    public SessionTimer getSessionTimer() {
      return sessionTimer;
    }
  }
}
