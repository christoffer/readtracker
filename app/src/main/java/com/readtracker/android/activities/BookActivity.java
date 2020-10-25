package com.readtracker.android.activities;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;

import androidx.fragment.app.FragmentManager;
import androidx.viewpager.widget.PagerTabStrip;
import androidx.viewpager.widget.ViewPager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import com.readtracker.R;
import com.readtracker.android.db.Book;
import com.readtracker.android.db.DatabaseManager;
import com.readtracker.android.db.Model;
import com.readtracker.android.db.Session;
import com.readtracker.android.fragments.BookFragmentAdapter;
import com.readtracker.android.fragments.ReadFragment;
import com.readtracker.android.support.ColorUtils;
import com.readtracker.databinding.BookActivityBinding;
import com.squareup.otto.Produce;
import com.squareup.otto.Subscribe;

import java.lang.ref.WeakReference;

import static com.readtracker.android.fragments.BookFragmentAdapter.Page;

/**
 * Browse data for, and interact with, a book.
 */
public class BookActivity extends BookBaseActivity implements EndSessionDialog.EndSessionDialogListener {
  protected static final String TAG = BookActivity.class.getSimpleName();

  public static final int REQUEST_ADD_QUOTE = 1;
  public static final int REQUEST_EDIT_BOOK = 2;
  public static final int REQUEST_FINISH_BOOK = 3;
  public static final int REQUEST_EDIT_SESSION = 4;

  private static final String END_SESSION_FRAGMENT_TAG = "end-session-tag";

  private static final String STATE_END_POSITION = "END_POSITION";
  private static final String STATE_DURATION = "DURATION";
  private static final String STATE_VIEW_PAGER_PAGE = "VIEW_PAGER_PAGE";
  public static final String KEY_FINISHED = "FINISHED";

  private Session mCurrentSession;

  private Page mInitialFragmentPage;
  private BookActivityBinding binding;

  private ViewPager mViewPager;
  private PagerTabStrip mPagerTabStrip;

  @Override
  public void onCreate(Bundle in) {
    super.onCreate(in);

    binding = BookActivityBinding.inflate(getLayoutInflater());
    mViewPager = binding.fragmentViewPager;
    mPagerTabStrip = binding.pagerTabStrip;
    setContentView(binding.getRoot());

    mPagerTabStrip.setVisibility(View.INVISIBLE);
    mPagerTabStrip.setDrawFullUnderline(false);

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
        if(resultCode == BookSettingsActivity.RESULT_DELETED_BOOK) {
          Log.d(TAG, "Book deleted, shutting down");
          setResult(RESULT_OK); // use OK to trigger reload in HomeActivity
          finish();
        } else if(resultCode == ActivityCodes.RESULT_OK) {
          Log.d(TAG, "Came back from changing the book settings");
          loadBookFromIntent(); // reload book
        }
        break;
      case REQUEST_EDIT_SESSION:
        if (resultCode == RESULT_OK) {
          mInitialFragmentPage = Page.SUMMARY;
          Log.d(TAG, "Session modified (or deleted)");
          loadBookFromIntent();
          setResult(RESULT_OK);
        }
        break;
    }
    super.onActivityResult(requestCode, resultCode, data);
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.book_activity_menu, menu);
    return super.onCreateOptionsMenu(menu);
  }

  @Override
  protected void onBookLoaded(Book book) {
    Log.v(TAG, "Book loaded: " + book);
    mCurrentSession.setBook(book);
    mCurrentSession.setStartPosition(book.getCurrentPosition());
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
    if(item.getItemId() == R.id.book_activity_edit_book_menu_item) {
      openSettingsForCurrentBook();
    }

    return false;
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

  @Subscribe public void onBookEditRequestedEvent(final BookActivity.BookEditRequestedEvent event) {
    openSettingsForCurrentBook();
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

  private void openSettingsForCurrentBook() {
    Intent intent = new Intent(this, BookSettingsActivity.class);
    intent.putExtra(BookBaseActivity.KEY_BOOK_ID, getBookIdFromIntent());
    startActivityForResult(intent, REQUEST_EDIT_BOOK);
  }

  private void saveSessionAndExit() {
    runWhenBookIsReady(new Runnable() {
      @Override public void run() {
        Log.d(TAG, "Saving book and session");
        final Book book = getBook();

        mCurrentSession.setTimestampMs(System.currentTimeMillis());

        // Snapshot the session as the latest state of the book
        book.setCurrentPosition(mCurrentSession.getEndPosition());
        book.setCurrentPositionTimestampMs(mCurrentSession.getTimestampMs());

        // Pass a flag to the receiver, allowing it to act on the fact that
        // the resulting book is finished.
        Intent data = new Intent();
        data.putExtra(BookActivity.KEY_FINISHED, book.isInState(Book.State.Finished));

        new SaveAndExitTask(BookActivity.this, data).execute(mCurrentSession, book);
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
    final int bookColor = ColorUtils.getColorForBook(book);
    mPagerTabStrip.setTabIndicatorColor(bookColor);

    final Page initialPage = mInitialFragmentPage == null ? Page.READING : mInitialFragmentPage;
    mViewPager.setCurrentItem(bookFragmentAdapter.getPageIndex(initialPage), false);
  }

  /**
   * Task that saves an arbitrary number of models in the background and closes down the activity
   * with a successful result when done.
   */
  private static class SaveAndExitTask extends AsyncTask<Model, Void, Boolean> {
    private final WeakReference<BookActivity> mActivityRef;
    private final Intent mResultData;

    SaveAndExitTask(BookActivity activity, Intent resultData) {
      mResultData = resultData;
      mActivityRef = new WeakReference<>(activity);
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
        if(mResultData != null) {
          activity.setResult(RESULT_OK, mResultData);
        } else {
          activity.setResult(RESULT_OK);
        }

        activity.finish();
      }
    }
  }

  public static class BookEditRequestedEvent {

  }
}
