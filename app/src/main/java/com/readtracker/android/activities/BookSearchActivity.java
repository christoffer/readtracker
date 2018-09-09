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
import butterknife.ButterKnife;
import butterknife.InjectView;
import com.readtracker.R;
import com.readtracker.android.IntentKeys;
import com.readtracker.android.adapters.BookItem;
import com.readtracker.android.adapters.SearchResultAdapter;
import com.readtracker.android.thirdparty.SafeViewFlipper;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.readtracker.android.support.GoogleBooksSearchService.GoogleBookSearchApi;
import static com.readtracker.android.support.GoogleBooksSearchService.GoogleBooksResponse;
import static com.readtracker.android.support.GoogleBooksSearchService.GoogleVolumeDto;

/**
 * Activity for adding a new book by searching for books on Google Books
 */
public class BookSearchActivity extends BaseActivity {
    private static final String TAG = BookSearchActivity.class.getSimpleName();

    private static final int REQUEST_ADD_BOOK = 1;

    // Indices of flipper pages
    private static final int FLIPPER_INDEX_ADD = 0;
    private static final int FLIPPER_INDEX_SEARCH = 1;

    @InjectView(R.id.listSearchResult)
    ListView mListSearchResults;
    @InjectView(R.id.textSearch)
    EditText mEditTextSearch;
    @InjectView(R.id.flipperBookSearchActions)
    SafeViewFlipper mFlipperBookSearchActions;
    @InjectView(R.id.buttonNew)
    Button mButtonNew;
    @InjectView(R.id.buttonSearch)
    Button mButtonSearch;

    private SearchResultAdapter mBookSearchAdapter;
    private InputMethodManager mInputMethodManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book_search);
        ButterKnife.inject(this);

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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Fall through when coming back from a successful book activity
        if (requestCode == REQUEST_ADD_BOOK && resultCode == BookSettingsActivity.RESULT_ADDED_BOOK) {
            Intent searchResultData = new Intent();
            searchResultData.putExtra(
                    BookBaseActivity.KEY_BOOK_ID,
                    data.getExtras().getInt(BookBaseActivity.KEY_BOOK_ID)
            );
            setResult(RESULT_OK, searchResultData);
            finish();
        }
    }

    public void onSearchResultsRetrieved(List<GoogleVolumeDto> result) {
        getApp().clearProgressDialog();
        mEditTextSearch.setEnabled(true);
        mBookSearchAdapter.clear();

        if (result == null || result.size() == 0) {
            toastLong(getString(R.string.book_search_no_results));
            result = new ArrayList<>();
        }

        Log.d(TAG, "Setting book search results. Got " + result.size() + " books");

        for (GoogleVolumeDto volume : result) {
            if (volume != null && volume.getBook().isValid()) {
                mBookSearchAdapter.add(volume.getBook().convertToItem());
            }
        }
        mInputMethodManager.hideSoftInputFromWindow(
                mEditTextSearch.getWindowToken(),
                InputMethodManager.HIDE_NOT_ALWAYS
        );
    }

    /**
     * Connects events to bound views
     */
    public void bindEvents() {
        // Handle enter key press in the search bar
        mEditTextSearch.setOnEditorActionListener(new OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH || isDoneAction(actionId, keyEvent)) {
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
            @Override
            public void afterTextChanged(Editable editable) {
                final boolean hasText = mEditTextSearch.getText().length() > 0;
                final int displayedChild = mFlipperBookSearchActions.getDisplayedChild();

                // Ensure the correct page is set for the
                if (hasText && displayedChild != FLIPPER_INDEX_SEARCH) {
                    mFlipperBookSearchActions.setDisplayedChild(FLIPPER_INDEX_SEARCH);
                } else if (!hasText && displayedChild != FLIPPER_INDEX_ADD) {
                    mFlipperBookSearchActions.setDisplayedChild(FLIPPER_INDEX_ADD);
                }
            }

            @Override
            public void beforeTextChanged(CharSequence cs, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence cs, int i, int i1, int i2) {
            }
        });

        // Handle clicking a search result
        mListSearchResults.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView parent, View view, int position, long id) {
                BookItem clickedBook = mBookSearchAdapter.getItem(position);
                if (clickedBook != null) {
                    exitToBookInit(
                            clickedBook.getTitle(),
                            clickedBook.getAuthor(),
                            clickedBook.getCoverUrl(),
                            (int) clickedBook.getPageCount()
                    );
                }
            }
        });

        mButtonNew.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onNewClicked();
            }
        });

        mButtonSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onSearchClicked();
            }
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
        getApp().showProgressDialog(BookSearchActivity.this, R.string.book_search_searching);

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(GoogleBookSearchApi.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        GoogleBookSearchApi postApi = retrofit.create(GoogleBookSearchApi.class);

        String isbnQuery = parseISBNQueryString(query);
        Call<GoogleBooksResponse> call = postApi.getVolumes(isbnQuery != null ? isbnQuery : query);

        call.enqueue(new Callback<GoogleBooksResponse>() {
            @Override
            public void onResponse(Call<GoogleBooksResponse> call, Response<GoogleBooksResponse> response) {
                if (response.isSuccessful()) {
                    GoogleBooksResponse responseBody = response.body();
                    if (responseBody != null) {
                        onSearchResultsRetrieved(responseBody.getItems());
                    }
                } else {
                    Log.d(
                            TAG,
                            "Something went wrong with error code " + response.code() + ":\n" + response.errorBody()
                    );
                }
            }

            @Override
            public void onFailure(Call<GoogleBooksResponse> call, Throwable error) {
                Log.e(TAG, error.getMessage(), error);
            }
        });
    }

    private static String parseISBNQueryString(String query) {
        Matcher isbnMatcher = Pattern.compile("^(?:isbn[ :]+)([0-9 -]+)$").matcher(query.toLowerCase().trim());
        if(isbnMatcher.matches()) {
            String cleanedNumber = isbnMatcher.group(0).replaceAll("[^0-9]+", "");
            return String.format("isbn:%s", cleanedNumber);
        }
        return null;
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
