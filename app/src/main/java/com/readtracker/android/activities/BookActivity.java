package com.readtracker.android.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.view.PagerTabStrip;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.readtracker.android.IntentKeys;
import com.readtracker.android.R;
import com.readtracker.android.db.Book;
import com.readtracker.android.db.Quote;
import com.readtracker.android.fragments.BookFragmentAdapter;
import com.readtracker.android.support.SessionTimerStore;
import com.squareup.otto.Produce;

import static com.readtracker.android.fragments.BookFragmentAdapter.Page;

/** Browse data for, and interact with, a book. */
public class BookActivity extends BookBaseActivity {
  protected static final String TAG = BookActivity.class.getSimpleName();

  // Fragment pages
  public static final int PAGE_UNSPECIFIED = -1;
  public static final int PAGE_SESSIONS = 0;
  public static final int PAGE_READING = 1;
  public static final int PAGE_QUOTES = 2;

  private static final int NO_GROUP = 0;
  private static final int MENU_EDIT_BOOK_SETTINGS = 1;

  private static final int REQUEST_EDIT_PAGE_NUMBERS = 1;
  private static final int REQUEST_ADD_QUOTE = 2;
  private static final int REQUEST_BOOK_SETTINGS = 3;

  private BookFragmentAdapter mBookFragmentAdapter;

  private ViewPager mViewPager;
  private PagerTabStrip mPagerTabStrip;

  private boolean mManualShutdown;

  private Page mInitialFragmentPage;

  // open the book for read tracking
  // if book is finished
  // open book for summary viewing
  // if coming back from book edit
  // reload book
  // when coming back from session edit
  // set result and finish

  public void onCreate(Bundle in) {
    super.onCreate(in);

    setContentView(R.layout.book_activity);
    mViewPager = (ViewPager) findViewById(R.id.fragment_view_pager);
    mPagerTabStrip = (PagerTabStrip) findViewById(R.id.pager_tab_strip);

    mPagerTabStrip.setVisibility(View.INVISIBLE);

    // Load information from database
    loadBookFromIntent();
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
            exitToBookEditScreen(book);
          } else {
            Log.w(TAG, "Ignoring request for book settings, book is null");
          }
        } else if(resultCode == ActivityCodes.RESULT_DELETED_BOOK) {
          shutdownWithResult(RESULT_OK);
        } else if(resultCode == ActivityCodes.RESULT_OK) {
          Log.d(TAG, "Came back from changing the book settings");
          loadBookFromIntent();
        }
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

  @Produce public BookLoadedEvent produceBookLoadedEvent() {
    return new BookLoadedEvent(getBook());
  }

  private void setupFragments(Book book) {
    Page[] pages;
    if(book.isInState(Book.State.Finished)) {
      pages = new Page[] { Page.SUMMARY, Page.QUOTES };
    } else {
      pages = new Page[] { Page.SUMMARY, Page.READING, Page.QUOTES };
    }

    mBookFragmentAdapter = new BookFragmentAdapter(getApplicationContext(), getSupportFragmentManager(), pages);

    mViewPager.setAdapter(mBookFragmentAdapter);

    final Page initialPage = mInitialFragmentPage == null ? Page.READING : mInitialFragmentPage;
    mViewPager.setCurrentItem(mBookFragmentAdapter.getPageIndex(initialPage), false);
  }

  // Private

  public void exitToBookEditScreen(Book book) {
    Log.d(TAG, "Exit to book edit screen for book: " + book);
//    Log.d(TAG, String.format("Exiting to edit book: %s", localReading.toString()));
//    Intent intentEditBook = new Intent(this, AddBookActivity.class);
//    intentEditBook.putExtra(IntentKeys.LOCAL_READING, localReading);
//    intentEditBook.putExtra(IntentKeys.EDIT_MODE, true);
//    startActivityForResult(intentEditBook, ActivityCodes.REQUEST_EDIT_PAGE_NUMBERS);
  }

  public void exitToAddQuoteScreen(Quote quote) {
//    Intent activityAddHighlight = new Intent(this, AddQuoteActivity.class);
//    activityAddHighlight.putExtra(IntentKeys.LOCAL_READING, mLocalReading);
//    activityAddHighlight.putExtra(IntentKeys.LOCAL_HIGHLIGHT, highlight);
//    startActivityForResult(activityAddHighlight, ActivityCodes.ADD_QUOTE);
  }

  /**
   * Exits to the home activity with correct result information.
   */
  public void exitToHomeScreen() {
    shutdownWithResult(ActivityCodes.RESULT_OK);
  }

  public void exitToBookSettings() {
//    Intent bookSettings = new Intent(this, BookSettingsActivity.class);
//    bookSettings.putExtra(IntentKeys.LOCAL_READING, mLocalReading);
//    startActivityForResult(bookSettings, ActivityCodes.REQUEST_BOOK_SETTINGS);
  }

  private void shutdownWithResult(int resultCode) {
    Log.v(TAG, "Shutting down");

    Intent data = null;
    Book book = getBook();
    if(book != null && resultCode == RESULT_OK) {
      data = new Intent();
      data.putExtra(KEY_BOOK_ID, book.getId());
    }

    if(data != null) setResult(resultCode, data);

    mManualShutdown = true;
    SessionTimerStore.clear();
    finish();
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
}
