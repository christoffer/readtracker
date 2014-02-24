package com.readtracker.android.activities;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;

import com.readtracker.android.IntentKeys;
import com.readtracker.android.R;
import com.readtracker.android.db.Book;
import com.readtracker.android.db.DatabaseManager;
import com.readtracker.android.thirdparty.views.Switch;

import java.lang.ref.WeakReference;

/**
 * Screen for adding a new book manually
 */
public class AddBookActivity extends BookBaseActivity {
  public static final String TAG = AddBookActivity.class.getName();

  private static final String KEY_EDIT_BOOK_ID = "EDIT_BOOK_ID";

  private static EditText mEditTitle;
  private static EditText mEditAuthor;
  private static EditText mEditPageCount;

  private static Button mButtonAddBook;

  private static Switch mSwitchPagesPercent;

  // Store the cover url from the intent that starts the activity
  private String mCoverURL;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_add_book);

    bindViews();

    Intent intent = getIntent();
    if(intent.hasExtra(KEY_EDIT_BOOK_ID)) {
      // Assume edit mode
      int bookId = intent.getIntExtra(KEY_EDIT_BOOK_ID, -1);
      loadBook(bookId); // defer setup to onBookLoaded
      Log.d(TAG, "Edit mode for book with id: " + mEditAuthor);
    } else {
      // Assume create mode
      Log.d(TAG, "Add book mode");
      setupCreateMode(intent);
    }
  }

  @Override
  protected void onBookLoaded(Book book) {
    setupEditMode(book);
  }

  private void bindViews() {
    mEditTitle = (EditText) findViewById(R.id.editTitle);
    mEditAuthor = (EditText) findViewById(R.id.editAuthor);
    mEditPageCount = (EditText) findViewById(R.id.editPageCount);
    mSwitchPagesPercent = (Switch) findViewById(R.id.togglePagesPercent);
    mButtonAddBook = (Button) findViewById(R.id.buttonAddBook);
  }

  private void bindEvents() {
    mButtonAddBook.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        onAddBookClicked();
      }
    });

    mSwitchPagesPercent.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
      @Override
      public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
        mEditPageCount.setEnabled(checked);
        String rememberedValue = (String) mEditPageCount.getTag();
        if(checked && rememberedValue != null) {
          mEditPageCount.setText(rememberedValue);
        } else {
          mEditPageCount.setTag(mEditPageCount.getText().toString());
          mEditPageCount.setText("100.00%");
        }
      }
    });
  }

  private void setupCreateMode(Intent intent) {
    mButtonAddBook.setText(R.string.add_book_add);
    mEditTitle.setText(intent.getStringExtra(IntentKeys.TITLE));
    mEditAuthor.setText(intent.getStringExtra(IntentKeys.AUTHOR));
    mCoverURL = intent.getStringExtra(IntentKeys.COVER_URL);
    setInitialPageCount(intent.getIntExtra(IntentKeys.PAGE_COUNT, 0));

    bindEvents();
  }

  private void setupEditMode(Book book) {
    mButtonAddBook.setText(R.string.add_book_save);

    mEditTitle.setEnabled(false);
    mEditAuthor.setEnabled(false);

    mEditTitle.setText(book.getTitle());
    mEditAuthor.setText(book.getAuthor());

    if(book.hasPageNumbers()) {
      mEditPageCount.setText(String.valueOf(book.getNumberPages()));
      mSwitchPagesPercent.setChecked(true);
    } else {
      mEditPageCount.setText("");
      mSwitchPagesPercent.setChecked(false);
    }

    mCoverURL = book.getCoverUrl();
    bindEvents();
  }

  private void setInitialPageCount(long pageCount) {
    if(pageCount > 0) {
      mEditPageCount.setText(Long.toString(pageCount));
    }
  }

  private void onAddBookClicked() {
    if(!validateFields()) {
      return;
    }

    Book book = getBook();
    if(book == null) {
      book = new Book();
    }

    book.setTitle(mEditTitle.getText().toString());
    book.setAuthor(mEditAuthor.getText().toString());
    book.setCoverUrl(mCoverURL);

    if(mSwitchPagesPercent.isChecked()) {
      int discretePages = Integer.parseInt(mEditPageCount.getText().toString());
      book.setNumberPages((float) discretePages);
    } else {
      book.setNumberPages(null);
    }

    saveBook(book);
  }

  /**
   * Validates the input fields and moves focus to any invalid ones.
   *
   * @return true if all fields were valid, otherwise false
   */
  private boolean validateFields() {
    if(mEditTitle.getText().length() < 1) {
      toast(R.string.add_book_missing_title);
      mEditTitle.requestFocus();
      return false;
    }

    if(mEditAuthor.getText().length() < 1) {
      toast(R.string.add_book_missing_author);
      mEditAuthor.requestFocus();
      return false;
    }

    // Validate a reasonable amount of page numbers
    if(mSwitchPagesPercent.isChecked()) {
      int pageCount = 0;

      try {
        pageCount = Integer.parseInt(mEditPageCount.getText().toString());
      } catch(NumberFormatException ignored) {
      }

      if(pageCount < 1) {
        toast(R.string.add_book_missing_page_count);
        mEditPageCount.requestFocus();
        return false;
      }
    }

    return true;
  }

  private void exitToReadingSession(Book book) {
    Intent intent = new Intent(this, BookActivity.class);
    intent.putExtra(BookBaseActivity.KEY_BOOK_ID, book.getId());
    setResult(RESULT_OK);
    startActivity(intent);
    finish();
  }

  private void saveBook(Book book) {
    new SaveTask(book, this).execute();
  }

  private void onBookSaved(Book book) {
    if(book != null) {
      exitToReadingSession(book);
    } else {
      Log.w(TAG, "Failed to save book");
    }
  }

  private static class SaveTask extends AsyncTask<Void, Void, Book> {
    private final WeakReference<AddBookActivity> mActivity;
    private final DatabaseManager mDatabaseMgr;
    private final Book mBook;

    public SaveTask(Book book, AddBookActivity activity) {
      mBook = book;
      mActivity = new WeakReference<AddBookActivity>(activity);
      mDatabaseMgr = activity.getApp().getDatabaseManager();
    }

    @Override
    protected Book doInBackground(Void... voids) {
      return mDatabaseMgr.save(mBook);
    }

    @Override
    protected void onPostExecute(Book book) {
      AddBookActivity activity = mActivity.get();
      if(activity != null) {
        activity.onBookSaved(book);
      }
    }
  }
}