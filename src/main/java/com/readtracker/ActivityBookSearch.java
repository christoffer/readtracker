package com.readtracker;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethod;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.TextView.OnEditorActionListener;
import com.readtracker.thirdparty.SafeViewFlipper;
import com.readtracker.utils.GoogleBook;
import com.readtracker.utils.GoogleBookSearch;
import com.readtracker.utils.GoogleBookSearchException;

import java.util.ArrayList;

/**
 * Screen for searching for books on Google Books
 */
public class ActivityBookSearch extends ReadTrackerActivity {

  private static ListView mListSearchResults;
  private static EditText mEditTextSearch;

  private static SafeViewFlipper mFlipperBookSearchActions;

  private static Button mButtonNew;
  private static Button mButtonSearch;

  private static ListAdapterBook mListAdapterBookSearch;
  private static InputMethodManager mInputMethodManager;

  // Indices of flipper pages
  private static final int FLIPPER_INDEX_ADD = 0;
  private static final int FLIPPER_INDEX_SEARCH = 1;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_book_search);

    bindViews();

    mListAdapterBookSearch = new ListAdapterBook(this, R.layout.list_item_book, R.id.textTitle, new ArrayList<ListItemBook>());
    mListSearchResults.setAdapter(mListAdapterBookSearch);

    // Suggest that the soft input keyboard is visible at once
    mInputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
    mInputMethodManager.showSoftInput(mEditTextSearch, InputMethodManager.SHOW_IMPLICIT);

    bindEvents();
  }

  @Override
  protected void onDestroy() {
    mListAdapterBookSearch.cleanUpDrawables(); // recycles images in drawable manager
    super.onDestroy();
  }

  @Override
  public boolean onSearchRequested() {
    mEditTextSearch.requestFocus();
    return true;
  }

  @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    // Fall through when coming back from a successful book activity
    if(requestCode == ActivityCodes.REQUEST_ADD_BOOK && resultCode == ActivityCodes.RESULT_OK) {
      setResult(ActivityCodes.RESULT_OK);
      finish();
    }
  }

  /**
   * Bind local variables to views in the layout
   */
  private void bindViews() {
    mListSearchResults = (ListView) findViewById(R.id.listSearchResult);
    mEditTextSearch = (EditText) findViewById(R.id.textSearch);

    mFlipperBookSearchActions = (SafeViewFlipper) findViewById(R.id.flipperBookSearchActions);
    mButtonNew = (Button) findViewById(R.id.buttonNew);
    mButtonSearch = (Button) findViewById(R.id.buttonSearch);
  }

  /**
   * Connects events to bound views
   */
  public void bindEvents() {
    // Handle enter key press in the search bar
    mEditTextSearch.setOnEditorActionListener(new OnEditorActionListener() {
      @Override
      public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
        if(actionId == EditorInfo.IME_ACTION_SEARCH || Helpers.isDoneAction(actionId, keyEvent)) {
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
        ListItemBook clickedBook = mListAdapterBookSearch.getItem(position);
        exitToBookInit(clickedBook.title, clickedBook.author, clickedBook.coverURL, clickedBook.pageCount);
      }
    });

    mButtonNew.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View view) { onNewClicked(); }
    });

    mButtonSearch.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View view) { onSearchClicked(); }
    });
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
    getApp().showProgressDialog(ActivityBookSearch.this, "Searching...");
    (new BookSearchTask()).execute(query);
  }

  /**
   * Exits to the Add book dialog with the given pre-filled data
   *
   * @param title     title of book
   * @param author    author of book
   * @param coverURL  cover url
   * @param pageCount number of pages in the book (Use -1 if not available)
   */
  private void exitToBookInit(String title, String author, String coverURL, Long pageCount) {
    Intent intent = new Intent(this, ActivityAddBook.class);
    intent.putExtra(IntentKeys.TITLE, title);
    intent.putExtra(IntentKeys.AUTHOR, author);
    intent.putExtra(IntentKeys.COVER_URL, coverURL);
    intent.putExtra(IntentKeys.PAGE_COUNT, pageCount);
    startActivityForResult(intent, ActivityCodes.REQUEST_ADD_BOOK);
  }

  /**
   * Exists to the Add book dialog with the intention to create a completely
   * new book
   */
  public void exitToBookInitForNewBook() {
    Intent intent = new Intent(this, ActivityAddBook.class);
    startActivity(intent);
  }

  public void setSearchResults(ArrayList<GoogleBook> foundBooks) {
    getApp().clearProgressDialog();
    mEditTextSearch.setEnabled(true);
    mListAdapterBookSearch.clear();

    if(foundBooks == null) {
      Log.d(TAG, "Book search result was null");
      toastLong("There was a problem with searching");
      foundBooks = new ArrayList<GoogleBook>();
    }

    Log.d(TAG, "Setting book search results. Got " + foundBooks.size() + " books");

    if(foundBooks.size() > 0) {
      for(GoogleBook book : foundBooks) {
        mListAdapterBookSearch.add(new ListItemGoogleBook(book));
      }
      mInputMethodManager.hideSoftInputFromWindow(mEditTextSearch.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
    }
  }

  /**
   * Perform a book search on the server
   */
  private class BookSearchTask extends AsyncTask<String, Void, ArrayList<GoogleBook>> {

    protected void onPostExecute(ArrayList<GoogleBook> foundBooks) {
      setSearchResults(foundBooks);
    }

    @Override
    protected ArrayList<GoogleBook> doInBackground(String... searchWords) {
      try {
        return GoogleBookSearch.search(searchWords[0]);
      } catch(GoogleBookSearchException e) {
        Log.e(TAG, "Error while searching GoogleBooks", e);
        return null;
      }
    }
  }

}
