package com.readtracker.android.activities;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;

import com.readtracker.R;
import com.readtracker.android.IntentKeys;
import com.readtracker.android.db.Book;
import com.readtracker.android.db.DatabaseManager;
import com.readtracker.android.thirdparty.views.Switch;

import java.lang.ref.WeakReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import butterknife.ButterKnife;
import butterknife.InjectView;

/** Activity for adding or editing a book. */
public class AddBookActivity extends BookBaseActivity {
  public static final String TAG = AddBookActivity.class.getName();

  public static final int RESULT_ADDED_BOOK = RESULT_FIRST_USER + 1;
  public static final int RESULT_DELETED_BOOK = RESULT_FIRST_USER + 2;

  public static final String KEY_QUOTE_ID = "QUOTE_ID";

  @InjectView(R.id.title_edit) EditText mTitleEdit;
  @InjectView(R.id.author_edit) EditText mAuthorEdit;
  @InjectView(R.id.page_count_edit) EditText mPageCountEdit;
  @InjectView(R.id.add_or_save_button) Button mAddOrSaveButton;
  @InjectView(R.id.page_pct_toggle) Switch mPagePctToggle;

  // Store the cover url from the intent that starts the activity
  private String mCoverURL;
  private boolean mEditMode;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_add_book);
    ButterKnife.inject(this);

    Intent intent = getIntent();
    if(intent.hasExtra(KEY_BOOK_ID)) {
      // Assume edit mode if we get a book id passed in
      int bookId = intent.getExtras().getInt(KEY_BOOK_ID);
      loadBook(bookId); // defer setup to onBookLoaded
    } else {
      mEditMode = false;
      // Assume create mode
      Log.d(TAG, "Add book mode");
      initializeForAddingBook(intent);
    }
  }

  @Override public boolean onCreateOptionsMenu(Menu menu) {
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.add_book_menu, menu);
    return super.onCreateOptionsMenu(menu);
  }

  @Override public boolean onPrepareOptionsMenu(Menu menu) {
    MenuItem deleteItem = menu.findItem(R.id.add_book_delete_item);
    if(deleteItem != null) {
      deleteItem.setVisible(mEditMode);
    }
    return super.onPrepareOptionsMenu(menu);
  }

  @Override public boolean onOptionsItemSelected(MenuItem item) {
    if(item.getItemId() == R.id.add_book_delete_item) {
      confirmDeleteBook();
      return true;
    }
    return super.onOptionsItemSelected(item);
  }

  private void confirmDeleteBook() {
    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    final String title = getString(R.string.add_book_confirm_delete, mTitleEdit.getText().toString());
    builder.setTitle(title);
    builder.setMessage(R.string.add_book_delete_explanation);

    builder.setPositiveButton(R.string.add_book_delete, new DialogInterface.OnClickListener() {
      @Override public void onClick(DialogInterface dialogInterface, int i) {
        new DeleteBookTask(AddBookActivity.this, getBook()).execute();
      }
    });

    builder.setNegativeButton(R.string.general_cancel, new DialogInterface.OnClickListener() {
      @Override public void onClick(DialogInterface dialogInterface, int i) {
        dialogInterface.cancel();
      }
    });

    builder.setCancelable(true);
    builder.create().show();
  }

  @Override
  protected void onBookLoaded(Book book) {
    initializeForEditMode(book);
  }

  private void bindEvents() {
    mAddOrSaveButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        onAddOrUpdateClicked();
      }
    });

    mPagePctToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
      @Override
      public void onCheckedChanged(CompoundButton compoundButton, boolean isInPageMode) {
        mPageCountEdit.setEnabled(isInPageMode);
        if(isInPageMode) {
          final String rememberedValue = (String) mPageCountEdit.getTag();
          mPageCountEdit.setText(rememberedValue == null ? "0" : rememberedValue);
        } else {
          mPageCountEdit.setTag(mPageCountEdit.getText().toString());
          mPageCountEdit.setText("100%");
        }
      }
    });
  }

  private void initializeForAddingBook(Intent intent) {
    mAddOrSaveButton.setText(R.string.add_book_add);
    mTitleEdit.setText(intent.getStringExtra(IntentKeys.TITLE));
    mAuthorEdit.setText(intent.getStringExtra(IntentKeys.AUTHOR));
    mCoverURL = intent.getStringExtra(IntentKeys.COVER_URL);
    setInitialPageCount(intent.getIntExtra(IntentKeys.PAGE_COUNT, 0));

    bindEvents();
  }

  private void initializeForEditMode(Book book) {
    mAddOrSaveButton.setText(R.string.add_book_save);

    mTitleEdit.setText(book.getTitle());
    mAuthorEdit.setText(book.getAuthor());

    if(book.hasPageNumbers()) {
      int pageCount = (int) ((float) book.getPageCount());
      mPageCountEdit.setText(String.valueOf(pageCount));
      mPagePctToggle.setChecked(true);
      mPagePctToggle.setEnabled(true);
    } else {
      mPageCountEdit.setText("100%");
      mPagePctToggle.setChecked(false);
      mPagePctToggle.setEnabled(false);
    }

    mCoverURL = book.getCoverImageUrl();
    bindEvents();

    mEditMode = true;
    supportInvalidateOptionsMenu();
  }

  private void setInitialPageCount(long pageCount) {
    if(pageCount > 0) {
      mPageCountEdit.setText(Long.toString(pageCount));
    }
  }

  private void onAddOrUpdateClicked() {
    if(!validateFields()) {
      return;
    }

    final String newTitle = mTitleEdit.getText().toString();
    final String newAuthor = mAuthorEdit.getText().toString();

    // Be careful about changing the book title when we aren't changing the title.

    Book book = getBook();
    if(book == null) {
      book = new Book();
    }

    boolean didChangeTitle = !book.getTitle().equals(newTitle);

    book.setTitle(newTitle);
    book.setAuthor(newAuthor);
    book.setCoverImageUrl(mCoverURL);

    if(mPagePctToggle.isChecked()) {
      int discretePages = Integer.parseInt(mPageCountEdit.getText().toString());
      book.setPageCount((float) discretePages);
    } else {
      book.setPageCount(null);
    }

    new UpdateBookTask(this, book, didChangeTitle).execute();
  }

  /**
   * Validates the input fields and moves focus to any invalid ones.
   *
   * @return true if all fields were valid, otherwise false
   */
  private boolean validateFields() {
    if(mTitleEdit.getText().length() < 1) {
      toast(R.string.add_book_missing_title);
      mTitleEdit.requestFocus();
      return false;
    }

    if(mAuthorEdit.getText().length() < 1) {
      toast(R.string.add_book_missing_author);
      mAuthorEdit.requestFocus();
      return false;
    }

    // Validate a reasonable amount of page numbers
    if(mPagePctToggle.isChecked()) {
      int pageCount = 0;

      try {
        pageCount = Integer.parseInt(mPageCountEdit.getText().toString());
      } catch(NumberFormatException ignored) {
      }

      if(pageCount < 1) {
        toast(R.string.add_book_missing_page_count);
        mPageCountEdit.requestFocus();
        return false;
      }
    }

    return true;
  }

  private void onBookUpdated(int bookId, boolean success) {
    if(!success) {
      Log.w(TAG, "Failed to save book");
      return;
    }

    Intent data = new Intent();
    data.putExtra(KEY_BOOK_ID, bookId);

    if(mEditMode) {
      setResult(RESULT_OK, data);
    } else {
      setResult(RESULT_ADDED_BOOK, data);
    }
    finish();
  }

  private void onBookDeleted(int bookId, boolean success) {
    if(success) {
      Intent data = new Intent();
      data.putExtra(KEY_BOOK_ID, bookId);
      setResult(RESULT_DELETED_BOOK, data);
      finish();
    } else {
      toast(R.string.add_book_delete_failed);
    }
  }

  abstract static class BackgroundBookTask extends AsyncTask<Void, Void, Boolean> {
    // choose to prefix because the titles are truncated in the list, which would
    // make dupes of long titles invisible to the user
    public static final Pattern DUPE_COUNT_PATTERN = Pattern.compile("^[(](\\d+)[)](.*)");

    protected final WeakReference<AddBookActivity> mActivity;
    protected final DatabaseManager mDatabaseMgr;
    protected final Book mBook;
    protected final String mUnknownTitleString;

    public BackgroundBookTask(AddBookActivity activity, Book book) {
      mBook = book;
      mActivity = new WeakReference<AddBookActivity>(activity);
      mDatabaseMgr = activity.getApp().getDatabaseManager();
      mUnknownTitleString = activity.getString(R.string.general_unknown_title);
    }

    abstract protected boolean run();

    abstract protected void onComplete(AddBookActivity activity, boolean success);

    @Override
    protected Boolean doInBackground(Void... voids) {
      return run();
    }

    @Override protected void onPostExecute(Boolean success) {
      AddBookActivity activity = mActivity.get();
      if(activity != null) {
        onComplete(activity, success);
      }
    }
  }

  static class UpdateBookTask extends BackgroundBookTask {
    private final boolean mShouldMakeTitleUnique;

    public UpdateBookTask(AddBookActivity activity, Book book, boolean shouldMakeTitleUnique) {
      super(activity, book);
      mShouldMakeTitleUnique = shouldMakeTitleUnique;
    }

    @Override protected boolean run() {
      if(mShouldMakeTitleUnique) {
        mBook.setTitle(getUniqueTitle(mBook.getTitle()));
      }
      return mDatabaseMgr.save(mBook);
    }

    @Override protected void onComplete(AddBookActivity activity, boolean success) {
      activity.onBookUpdated(mBook.getId(), success);
    }

    private String getUniqueTitle(String title) {
      if(TextUtils.isEmpty(title)) {
        title = mUnknownTitleString;
      }

      boolean unique = mDatabaseMgr.isUniqueTitle(title);
      int dupeNumber = 1;
      while(!unique) { // found dupe title
        String cleanTitle = getTitleWithoutDupeCount(title).trim();
        title = String.format("(%d) %s", dupeNumber++, cleanTitle);
        unique = mDatabaseMgr.isUniqueTitle(title);
      }

      return title;
    }

    private String getTitleWithoutDupeCount(String title) {
      Matcher matcher = DUPE_COUNT_PATTERN.matcher(title);
      if(matcher.find()) {
        return matcher.group(2);
      } else {
        return title;
      }
    }
  }

  static class DeleteBookTask extends BackgroundBookTask {
    public DeleteBookTask(AddBookActivity activity, Book book) {
      super(activity, book);
    }

    @Override protected boolean run() {
      return mDatabaseMgr.delete(mBook);
    }

    @Override protected void onComplete(AddBookActivity activity, boolean success) {
      activity.onBookDeleted(mBook.getId(), success);
    }
  }
}