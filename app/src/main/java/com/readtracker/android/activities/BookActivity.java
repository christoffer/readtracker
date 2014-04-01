package com.readtracker.android.activities;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.PagerTabStrip;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.readtracker.android.R;
import com.readtracker.android.db.Book;
import com.readtracker.android.db.DatabaseManager;
import com.readtracker.android.db.Model;
import com.readtracker.android.db.Session;
import com.readtracker.android.fragments.BookFragmentAdapter;
import com.readtracker.android.fragments.ReadFragment;
import com.squareup.otto.Produce;
import com.squareup.otto.Subscribe;

import java.lang.ref.WeakReference;

import static com.readtracker.android.fragments.BookFragmentAdapter.Page;

/**
 * Browse data for, and interact with, a book.
 */
public class BookActivity extends BookBaseActivity implements EndSessionDialog.EndSessionDialogListener {
  protected static final String TAG = BookActivity.class.getSimpleName();

  private static final int NO_GROUP = 0;
  private static final int MENU_EDIT_BOOK = 1;

  public static final int REQUEST_ADD_QUOTE = 1;
  public static final int REQUEST_EDIT_BOOK = 2;
  public static final int REQUEST_FINISH_BOOK = 3;

  private static final String END_SESSION_FRAGMENT_TAG = "end-session-tag";

  private static final String STATE_END_POSITION = "END_POSITION";
  private static final String STATE_DURATION = "DURATION";
  private static final String STATE_VIEW_PAGER_PAGE = "VIEW_PAGER_PAGE";

  private Session mCurrentSession;

  private ViewPager mViewPager;
  private PagerTabStrip mPagerTabStrip;

  private Page mInitialFragmentPage;

  public void onCreate(Bundle in) {
    super.onCreate(in);

    setContentView(R.layout.book_activity);
    mViewPager = (ViewPager) findViewById(R.id.fragment_view_pager);
    mPagerTabStrip = (PagerTabStrip) findViewById(R.id.pager_tab_strip);

    mPagerTabStrip.setVisibility(View.INVISIBLE);

    mCurrentSession = new Session();
    if(in != null) {
      if(in.containsKey(STATE_VIEW_PAGER_PAGE)) {
        mInitialFragmentPage = Page.values()[in.getInt(STATE_VIEW_PAGER_PAGE)];
      }

      mCurrentSession.setEndPosition(in.getFloat(STATE_END_POSITION));
      mCurrentSession.setDurationSeconds(in.getLong(STATE_DURATION));
    }

    // Load information from database
    loadBookFromIntent();
  }

  @Override
  protected void onStart() {
    super.onStart();
  }

  @Override
  protected void onStop() {
    super.onStop();
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    Log.d(TAG, "freezing state");
    if(mViewPager != null) {
      outState.putInt(STATE_VIEW_PAGER_PAGE, mViewPager.getCurrentItem());
    }

    outState.putFloat(STATE_END_POSITION, mCurrentSession.getEndPosition());
    outState.putLong(STATE_DURATION, mCurrentSession.getDurationSeconds());
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    if(resultCode == ActivityCodes.RESULT_CANCELED) {
      return;
    }

    Log.v(TAG, "onActivityResult: requestCode: " + requestCode + ", resultCode: " + resultCode);
    switch(requestCode) {
      case REQUEST_FINISH_BOOK:
        if(resultCode == RESULT_OK) {
          final String closingRemark = data.getStringExtra(FinishBookActivity.KEY_CLOSING_REMARK);
          runWhenBookIsReady(new Runnable() {
            @Override public void run() {
              final Book book = getBook();
              Log.d(TAG, String.format("Finishing book %s with closing remark %s", book, closingRemark));
              mCurrentSession.setEndPosition(1f);
              book.setClosingRemark(closingRemark);
              book.setState(Book.State.Finished);
              saveSessionAndExit();
            }
          });
        }
      case REQUEST_ADD_QUOTE:
        if(resultCode == RESULT_OK) {
          Log.d(TAG, "Came back from adding a quote");
          mInitialFragmentPage = Page.QUOTES;
          loadBookFromIntent(); // reload book
        }
        break;
      case REQUEST_EDIT_BOOK:
        if(resultCode == AddBookActivity.RESULT_DELETED_BOOK) {
          Log.d(TAG, "Book deleted, shutting down");
          setResult(RESULT_OK); // use OK to trigger reload in HomeActivity
          finish();
        } else if(resultCode == ActivityCodes.RESULT_OK) {
          Log.d(TAG, "Came back from changing the book settings");
          loadBookFromIntent(); // reload book
        }
        break;
    }
    super.onActivityResult(requestCode, resultCode, data);
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    menu.add(NO_GROUP, MENU_EDIT_BOOK, 0, "Edit book settings");
    return true;
  }

  @Override
  protected void onBookLoaded(Book book) {
    Log.v(TAG, "Book loaded: " + book);
    mCurrentSession.setBook(book);
    postEvent(new BookLoadedEvent(book));
    setupFragments(book);
    mPagerTabStrip.setVisibility(View.VISIBLE);
  }

  @Override
  protected boolean shouldLoadRelatedBookData() {
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch(item.getItemId()) {
      case MENU_EDIT_BOOK:
        Intent intent = new Intent(this, AddBookActivity.class);
        intent.putExtra(BookBaseActivity.KEY_BOOK_ID, getBookIdFromIntent());
        startActivityForResult(intent, REQUEST_EDIT_BOOK);
        break;
      default:
        return false;
    }
    return true;
  }

  @Produce public BookLoadedEvent produceBookLoadedEvent() {
    return new BookLoadedEvent(getBook());
  }

  @Subscribe public void onSessionDoneEvent(final ReadFragment.SessionDoneEvent event) {
    mCurrentSession.setDurationSeconds(event.getDurationMillis() / 1000);
    runWhenBookIsReady(new Runnable() {
      @Override public void run() {
        final EndSessionDialog dialog = EndSessionDialog.newInstance(getBook());
        FragmentManager fragmentManager = getSupportFragmentManager();
        dialog.show(fragmentManager, END_SESSION_FRAGMENT_TAG);
      }
    });
  }

  @Override public void onConfirmedSessionEndPosition(float position) {
    mCurrentSession.setEndPosition(position);
    if(position == 1f) {
      // Finishing book, ask the user for a closing remark before finishing up
      Intent intent = new Intent(this, FinishBookActivity.class);
      intent.putExtra(BookBaseActivity.KEY_BOOK_ID, getBook().getId());
      startActivityForResult(intent, REQUEST_FINISH_BOOK);
    } else {
      // Updated session, finish up
      saveSessionAndExit();
    }
  }

  private void saveSessionAndExit() {
    runWhenBookIsReady(new Runnable() {
      @Override public void run() {
        Log.d(TAG, "Saving book and session");
        final Book book = getBook();

        mCurrentSession.setTimestamp(System.currentTimeMillis());

        // Snapshot the session as the latest state of the book
        book.setCurrentPosition(mCurrentSession.getEndPosition());
        book.setCurrentPositionTimestamp(mCurrentSession.getTimestamp());

        new SaveAndExitTask(BookActivity.this).execute(mCurrentSession, book);
      }
    });
  }

  private void setupFragments(Book book) {
    Page[] pages;
    if(book.isInState(Book.State.Finished)) {
      pages = new Page[]{Page.SUMMARY, Page.QUOTES};
    } else {
      pages = new Page[]{Page.SUMMARY, Page.READING, Page.QUOTES};
    }

    BookFragmentAdapter bookFragmentAdapter = new BookFragmentAdapter(getApplicationContext(), getSupportFragmentManager(), pages);

    mViewPager.setAdapter(bookFragmentAdapter);

    final Page initialPage = mInitialFragmentPage == null ? Page.READING : mInitialFragmentPage;
    mViewPager.setCurrentItem(bookFragmentAdapter.getPageIndex(initialPage), false);
  }

  /**
   * Task that saves an arbitrary number of models in the background and closes down the activity
   * with a successful result when done.
   */
  private static class SaveAndExitTask extends AsyncTask<Model, Void, Boolean> {
    private final WeakReference<BookActivity> mActivityRef;

    SaveAndExitTask(BookActivity activity) {
      mActivityRef = new WeakReference<BookActivity>(activity);
    }

    @Override protected Boolean doInBackground(Model... modelsToSave) {
      BookActivity activity = mActivityRef.get();
      if(activity == null) {
        return Boolean.FALSE;
      }

      DatabaseManager db = activity.getDatabaseManager();
      for(Model model : modelsToSave) {
        Log.v(TAG, "Saving: " + model);
        if(!db.save(model)) {
          activity.toast(R.string.book_error_updating);
          return Boolean.FALSE;
        }
      }

      return Boolean.TRUE;
    }

    @Override protected void onPostExecute(Boolean result) {
      BookActivity activity = mActivityRef.get();
      if(activity != null && result.equals(Boolean.TRUE)) {
        activity.setResult(RESULT_OK);
        activity.finish();
      }
    }
  }
}
