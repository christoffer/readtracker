package com.readtracker.android.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import com.readtracker.R;
import com.readtracker.android.IntentKeys;
import com.readtracker.android.adapters.BookItem;
import com.readtracker.android.adapters.GoogleBookItem;
import com.readtracker.android.adapters.SearchResultAdapter;
import com.readtracker.android.support.GoogleBook;
import com.readtracker.android.tasks.GoogleBookSearchTask;
import com.readtracker.android.thirdparty.SafeViewFlipper;
import com.readtracker.databinding.ActivityBookSearchBinding;

import java.util.ArrayList;

import androidx.annotation.NonNull;

/**
 * Activity for adding a new book by searching for books on Google Books
 */
public class BookSearchActivity extends BaseActivity implements GoogleBookSearchTask.BookSearchResultListener {
  private static final String TAG = BookSearchActivity.class.getSimpleName();

  private static final int REQUEST_ADD_BOOK = 1;

  // Indices of flipper pages
  private static final int FLIPPER_INDEX_ADD = 0;
  private static final int FLIPPER_INDEX_SEARCH = 1;

  private ListView mListSearchResults;
  private EditText mEditTextSearch;
  private SafeViewFlipper mFlipperBookSearchActions;
  private Button mButtonNew;
  private Button mButtonSearch;

  private SearchResultAdapter mBookSearchAdapter;
  private InputMethodManager mInputMethodManager;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    @NonNull ActivityBookSearchBinding binding = ActivityBookSearchBinding.inflate(getLayoutInflater());

    setContentView(binding.getRoot());

    mListSearchResults = binding.listSearchResult;
    mEditTextSearch = binding.textSearch;
    mFlipperBookSearchActions = binding.flipperBookSearchActions;
    mButtonNew = binding.buttonNew;
    mButtonSearch = binding.buttonSearch;

    mBookSearchAdapter = new SearchResultAdapter(this, new ArrayList<BookItem>());
    mListSearchResults.setAdapter(mBookSearchAdapter);

    // Suggest that the soft input keyboard is visible at once
    mInputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
    mInputMethodManager.showSoftInput(mEditTextSearch, InputMethodManager.SHOW_IMPLICIT);

    bindEvents();
  }

  @Override
  public boolean onSearchRequested() {
    mEditTextSearch.requestFocus();
    return true;
  }

  @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);

    // Fall through when coming back from a successful book activity
    if(requestCode == REQUEST_ADD_BOOK && resultCode == BookSettingsActivity.RESULT_ADDED_BOOK) {
      Intent searchResultData = new Intent();
      searchResultData.putExtra(BookBaseActivity.KEY_BOOK_ID, data.getExtras().getInt(BookBaseActivity.KEY_BOOK_ID));
      setResult(RESULT_OK, searchResultData);
      finish();
    }
  }

  @Override public void onSearchResultsRetrieved(ArrayList<GoogleBook> result) {
    getApp().clearProgressDialog();
    mEditTextSearch.setEnabled(true);
    mBookSearchAdapter.clear();

    if(result == null || result.size() == 0) {
      toastLong(getString(R.string.book_search_no_results));
      result = new ArrayList<>();
    }

    Log.d(TAG, "Setting book search results. Got " + result.size() + " books");

    for(GoogleBook book : result) {
      mBookSearchAdapter.add(new GoogleBookItem(book));
    }
    mInputMethodManager.hideSoftInputFromWindow(mEditTextSearch.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
  }

  /**
   * Connects events to bound views
   */
  public void bindEvents() {
    // Handle enter key press in the search bar
    mEditTextSearch.setOnEditorActionListener(new OnEditorActionListener() {
      @Override
      public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
        if(actionId == EditorInfo.IME_ACTION_SEARCH || isDoneAction(actionId, keyEvent)) {
          mEditTextSearch.setEnabled(false);
          search(mEditTextSearch.getText().toString());
          return true;
        }
        return false;
      }
    });

    // Switch between the "Add" and "Search" button whenever the text goes from
    // empty to filled or vice versa
    mEditTextSearch.addTextChangedListener(new TextWatcher() {
      @Override public void afterTextChanged(Editable editable) {
        final boolean hasText = mEditTextSearch.getText().length() > 0;
        final int displayedChild = mFlipperBookSearchActions.getDisplayedChild();

        // Ensure the correct page is set for the
        if(hasText && displayedChild != FLIPPER_INDEX_SEARCH) {
          mFlipperBookSearchActions.setDisplayedChild(FLIPPER_INDEX_SEARCH);
        } else if(!hasText && displayedChild != FLIPPER_INDEX_ADD) {
          mFlipperBookSearchActions.setDisplayedChild(FLIPPER_INDEX_ADD);
        }
      }

      @Override
      public void beforeTextChanged(CharSequence cs, int i, int i1, int i2) { }

      @Override
      public void onTextChanged(CharSequence cs, int i, int i1, int i2) { }
    });

    // Handle clicking a search result
    mListSearchResults.setOnItemClickListener(new OnItemClickListener() {
      @Override
      public void onItemClick(AdapterView parent, View view, int position, long id) {
        BookItem clickedBook = mBookSearchAdapter.getItem(position);
        if(clickedBook != null) {
          exitToBookInit(clickedBook.title, clickedBook.author, clickedBook.coverURL, (int) clickedBook.pageCount);
        }
      }
    });

    mButtonNew.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View view) { onNewClicked(); }
    });

    mButtonSearch.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View view) { onSearchClicked(); }
    });
  }

  private boolean isDoneAction(int actionId, KeyEvent event) {
    boolean isActionEnter = event != null
        && event.getAction() == KeyEvent.ACTION_DOWN
        && event.getKeyCode() == KeyEvent.KEYCODE_ENTER;
    return actionId == EditorInfo.IME_ACTION_DONE || isActionEnter;
  }

  /**
   * Called when the new button is being clicked
   */
  public void onNewClicked() {
    exitToBookInitForNewBook();
  }

  /**
   * Called when the search button is being clicked
   */
  public void onSearchClicked() {
    String query = mEditTextSearch.getText().toString();
    search(query);
  }

  /**
   * Search Google books for the given query
   *
   * @param query search term
   */
  private void search(String query) {
    // TODO replace with a spinner in the text editor field
    getApp().showProgressDialog(BookSearchActivity.this, R.string.book_search_searching);
    GoogleBookSearchTask.search(query, this); // calls onSearchResultsRetrieved()
  }

  /**
   * Exits to the Add book dialog with the given pre-filled data
   *
   * @param title     title of book
   * @param author    author of book
   * @param coverURL  cover url
   * @param pageCount number of pages in the book (Use -1 if not available)
   */
  private void exitToBookInit(String title, String author, String coverURL, int pageCount) {
    Intent intent = new Intent(this, BookSettingsActivity.class);
    intent.putExtra(IntentKeys.TITLE, title);
    intent.putExtra(IntentKeys.AUTHOR, author);
    intent.putExtra(IntentKeys.COVER_URL, coverURL);
    intent.putExtra(IntentKeys.PAGE_COUNT, pageCount);
    startActivityForResult(intent, REQUEST_ADD_BOOK);
  }

  /**
   * Exists to the Add book dialog with the intention to create a completely
   * new book
   */
  public void exitToBookInitForNewBook() {
    Intent intent = new Intent(this, BookSettingsActivity.class);
    startActivityForResult(intent, REQUEST_ADD_BOOK);
  }
}
