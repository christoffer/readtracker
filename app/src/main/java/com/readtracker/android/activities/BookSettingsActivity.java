package com.readtracker.android.activities;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.graphics.Palette;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.Toast;

import com.readtracker.R;
import com.readtracker.android.IntentKeys;
import com.readtracker.android.custom_views.ColorPickerButton;
import com.readtracker.android.db.Book;
import com.readtracker.android.db.DatabaseManager;
import com.readtracker.android.support.ColorPickerDialog;
import com.readtracker.android.support.ColorUtils;
import com.readtracker.android.support.DrawableGenerator;
import com.readtracker.android.support.GoogleBook;
import com.readtracker.android.support.GoogleBookSearch;
import com.readtracker.android.support.Utils;
import com.readtracker.android.tasks.GoogleBookSearchTask;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import butterknife.ButterKnife;
import butterknife.InjectView;

/** Activity for adding or editing a book. */
public class BookSettingsActivity extends BookBaseActivity implements GoogleBookSearchTask.BookSearchResultListener {
  private static final String TAG = BookSettingsActivity.class.getName();

  public static final int RESULT_ADDED_BOOK = RESULT_FIRST_USER + 1;
  public static final int RESULT_DELETED_BOOK = RESULT_FIRST_USER + 2;

  public static final String KEY_QUOTE_ID = "QUOTE_ID";
  private static final int BUTTON_SIZE_DP = 32;

  @InjectView(R.id.title_edit) EditText mTitleEdit;
  @InjectView(R.id.author_edit) EditText mAuthorEdit;
  @InjectView(R.id.page_count_edit) EditText mPageCountEdit;
  @InjectView(R.id.add_or_save_button) Button mSaveButton;
  @InjectView(R.id.track_using_pages) CheckBox mTrackUsingPages;
  @InjectView(R.id.book_cover_image) ImageView mCoverImage;
  @InjectView(R.id.refresh_cover_button) ImageButton mRefreshCoverButton;
  @InjectView(R.id.layout_color_buttons) LinearLayout mColorButtonContainer;

  // Store the cover url from the intent that starts the activity
  private String mCoverURL;
  private boolean mEditMode;
  private int mCurrentBookColor = -1;

  private HashSet<Integer> mSuggestedColors = new HashSet<>();
  private int mButtonSizePx;
  private int mInitialBookColor;
  private int mColorButtonDistance;
  private View.OnClickListener mColorButtonClickListener = new View.OnClickListener() {
    @Override public void onClick(View view) {
      ColorPickerButton button = (ColorPickerButton) view;
      onColorPickerButtonClick(button);
    }
  };

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_add_book);
    ButterKnife.inject(this);

    Intent intent = getIntent();
    if(intent.hasExtra(KEY_BOOK_ID)) {
      // Assume edit mode if we get a book id passed in
      final Bundle extras = intent.getExtras();
      if(extras == null) {
        Log.w(TAG, "Unexpectedly failed to receive extras from intent");
      } else {
        int bookId = intent.getExtras().getInt(KEY_BOOK_ID);
        loadBook(bookId); // defer setup to onBookLoaded
      }
    } else {
      mEditMode = false;
      // Assume create mode
      Log.d(TAG, "Add book mode");
      initializeForAddingBook(intent);
    }

    final TextWatcher textWatcher = new TextWatcher() {
      @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

      @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

      @Override public void afterTextChanged(Editable s) {
        validateFieldsAndUpdateState();
      }
    };

    mTitleEdit.addTextChangedListener(textWatcher);
    mAuthorEdit.addTextChangedListener(textWatcher);
    mPageCountEdit.addTextChangedListener(textWatcher);

    mButtonSizePx = Utils.convertDPtoPixels(this, BUTTON_SIZE_DP);
    mInitialBookColor = ContextCompat.getColor(this, R.color.defaultBookColor);
    setActiveColorPick(mInitialBookColor);

    mColorButtonDistance = Utils.convertDPtoPixels(this, 16);

    validateFieldsAndUpdateState();
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

  /** Update the UI to reflect the current color choice */
  private void setActiveColorPick(int color) {
    mCurrentBookColor = color;
    mSaveButton.setBackground(DrawableGenerator.generateButtonBackground(mCurrentBookColor));

    for(int i = 0; i < mColorButtonContainer.getChildCount(); i++) {
      View view = mColorButtonContainer.getChildAt(i);
      if(view instanceof ColorPickerButton) {
        ColorPickerButton colorPickerButton = (ColorPickerButton) view;
        Integer buttonColor = colorPickerButton.getColor();
        colorPickerButton.setIsCurrentColor(buttonColor != null && (buttonColor == color));
      }
    }
  }

  private void confirmDeleteBook() {
    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    final String title = getString(R.string.add_book_confirm_delete, mTitleEdit.getText().toString());
    builder.setTitle(title);
    builder.setMessage(R.string.add_book_delete_explanation);

    builder.setPositiveButton(R.string.add_book_delete, new DialogInterface.OnClickListener() {
      @Override public void onClick(DialogInterface dialogInterface, int i) {
        new DeleteBookTask(BookSettingsActivity.this, getBook()).execute();
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

  @Override public void onSearchResultsRetrieved(ArrayList<GoogleBook> result) {
    // NOTE(christoffer) Ideally we'd like to give the user an option of picking the book here,
    // but for now we just pick the first result that has a cover.
    getApp().clearProgressDialog();
    boolean didFindCover = false;

    if(result != null && result.size() > 0) {
      for(int i = 0; i < result.size(); i++) {
        GoogleBook googleBook = result.get(i);
        String coverURL = googleBook.getCoverURL();
        if(coverURL != null && coverURL.length() > 0) {
          populateFieldsfromGoogleBook(googleBook);
          didFindCover = true;
          break;
        }
      }
    }

    if(!didFindCover) {
      Toast.makeText(this, R.string.add_book_no_cover_found, Toast.LENGTH_SHORT).show();
    }
  }

  private void populateFieldsfromGoogleBook(GoogleBook googleBook) {
    loadCoverFromURL(googleBook.getCoverURL());

    // Populate fields that haven't been filled out by the user from the book search result
    // as well.
    if(mTitleEdit.getText().length() == 0) {
      mTitleEdit.setText(googleBook.getTitle());
    }

    if(mAuthorEdit.getText().length() == 0) {
      mAuthorEdit.setText(googleBook.getAuthor());
    }

    if(mTrackUsingPages.isChecked() && mPageCountEdit.getText().length() == 0) {
      mPageCountEdit.setText(String.format(Locale.getDefault(), "%d", googleBook.getPageCount()));
    }
  }

  private void bindEvents() {
    mSaveButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        onAddOrUpdateClicked();
      }
    });

    mTrackUsingPages.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
      @Override
      public void onCheckedChanged(CompoundButton compoundButton, boolean isInPageMode) {
        setTrackUsingPages(isInPageMode);
      }
    });

    mRefreshCoverButton.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View view) {
        final String title = mTitleEdit.getText().toString();
        final String author = mAuthorEdit.getText().toString();
        mRefreshCoverButton.setEnabled(false);

        final String searchQuery = GoogleBookSearch.buildQueryForTitleAndAuthor(title, author);
        if(searchQuery != null) {
          getApp().showProgressDialog(BookSettingsActivity.this, R.string.add_book_looking_for_book);
          GoogleBookSearchTask.search(searchQuery, BookSettingsActivity.this);
        }
      }
    });

    mCoverImage.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        Toast.makeText(BookSettingsActivity.this, R.string.add_book_long_press_to_delete, Toast.LENGTH_SHORT).show();
      }
    });

    mCoverImage.setOnLongClickListener(new View.OnLongClickListener() {
      @Override public boolean onLongClick(View v) {
        mCoverURL = null;
        mCoverImage.setImageResource(R.drawable.icon_book);
        mRefreshCoverButton.setVisibility(View.VISIBLE);
        return true;
      }
    });
  }

  private void onColorPickerButtonClick(ColorPickerButton button) {
    Integer pickedColor = button.getColor();
    if(pickedColor == null) {
      final ColorPickerDialog colorDialog = new ColorPickerDialog(this, mCurrentBookColor, new ColorPickerDialog.Listener() {
        @Override public void onColorSelected(int color) {
          setActiveColorPick(color);
          resetColorButtons();
        }
      });
      colorDialog.show();
    } else {
      setActiveColorPick(pickedColor);
    }
  }

  private void setTrackUsingPages(boolean shouldTrackUsingPages) {
    boolean stateDidChange = false;

    if(mTrackUsingPages != null) {
      stateDidChange = mTrackUsingPages.isChecked() != shouldTrackUsingPages;
      mTrackUsingPages.setChecked(shouldTrackUsingPages);
    }
    if(mPageCountEdit != null) {
      mPageCountEdit.setEnabled(shouldTrackUsingPages);
      mPageCountEdit.setVisibility(shouldTrackUsingPages ? View.VISIBLE : View.INVISIBLE);

      if(stateDidChange) {
        mPageCountEdit.requestFocus();
      }
    }

    validateFieldsAndUpdateState();
  }

  private void initializeForAddingBook(Intent intent) {
    mTitleEdit.setText(intent.getStringExtra(IntentKeys.TITLE));
    mAuthorEdit.setText(intent.getStringExtra(IntentKeys.AUTHOR));
    loadCoverFromURL(intent.getStringExtra(IntentKeys.COVER_URL));
    setInitialPageCount(intent.getIntExtra(IntentKeys.PAGE_COUNT, 0));

    validateFieldsAndUpdateState();

    bindEvents();
  }

  private void initializeForEditMode(Book book) {
    mTitleEdit.setText(book.getTitle());
    mAuthorEdit.setText(book.getAuthor());

    mInitialBookColor = ColorUtils.getColorForBook(book);
    setActiveColorPick(mInitialBookColor);
    resetColorButtons();

    if(book.hasPageNumbers()) {
      int pageCount = (int) ((float) book.getPageCount());
      mPageCountEdit.setText(String.valueOf(pageCount));
      setTrackUsingPages(true);
    } else {
      setTrackUsingPages(false);
    }

    loadCoverFromURL(book.getCoverImageUrl());
    bindEvents();

    mEditMode = true;
    supportInvalidateOptionsMenu();
  }

  private void resetColorButtons() {
    mColorButtonContainer.removeAllViews();

    boolean hasActiveCurrentColor = false;

    // Add color options, if any
    for(Integer suggestedColor : mSuggestedColors) {
      if(suggestedColor != mInitialBookColor) {
        ColorPickerButton button = addColorButton(suggestedColor);
        final boolean isActiveColor = suggestedColor == mCurrentBookColor;
        button.setIsCurrentColor(isActiveColor);
        hasActiveCurrentColor = hasActiveCurrentColor || isActiveColor;
      }
    }

    // Add initial color button
    ColorPickerButton initialColorButton = addColorButton(mInitialBookColor);
    initialColorButton.setIsCurrentColor(mInitialBookColor == mCurrentBookColor);
    hasActiveCurrentColor = hasActiveCurrentColor || mInitialBookColor == mCurrentBookColor;

    // Add current color unless it's already represented. This can happen if the user uses
    // the color wheel to pick a new color.
    if (!hasActiveCurrentColor) {
      ColorPickerButton activeColorButton = addColorButton(mCurrentBookColor);
      activeColorButton.setIsCurrentColor(true);
    }

    // Add color wheel button
    ColorPickerButton pickColorButton = addColorButton(0);
    pickColorButton.setColor(null);
  }

  private ColorPickerButton addColorButton(int color) {
    ColorPickerButton button = new ColorPickerButton(this);
    button.setColor(color);
    LayoutParams layoutParams = new LayoutParams(
        ViewGroup.LayoutParams.WRAP_CONTENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
    );
    if(mColorButtonContainer.getChildCount() > 0) {
      layoutParams.setMargins(mColorButtonDistance, 0, 0, 0);
    }
    button.setLayoutParams(layoutParams);
    button.setOnClickListener(mColorButtonClickListener);
    mColorButtonContainer.addView(button);
    return button;
  }

  private void loadCoverFromURL(final String coverUrl) {
    Log.d(TAG, "picasso: load");
    final boolean hasCoverUrl = !TextUtils.isEmpty(coverUrl);
    if(hasCoverUrl) {
      final CoverLoadPicassoTarget target = new CoverLoadPicassoTarget(coverUrl);
      Picasso.with(this).load(coverUrl).into(target);
    }
  }

  @SuppressLint("SetTextI18n")
  private void setInitialPageCount(long pageCount) {
    if(pageCount > 0) {
      mPageCountEdit.setText(Long.toString(pageCount));
    }
  }

  private void onAddOrUpdateClicked() {
    if(!validateFieldsAndUpdateState()) {
      Log.d(TAG, "The button was enabled and clicked while fields were invalid. This should never happen.");
      toast(R.string.add_book_please_fill_out_required_fields);
      return;
    }

    final String newTitle = mTitleEdit.getText().toString();
    final String newAuthor = mAuthorEdit.getText().toString();

    Book book = getBook();
    if(book == null) {
      book = new Book();
      book.setCurrentPositionTimestampMs(System.currentTimeMillis());
    }

    // Be mindful about changing the book title when we aren't changing the title.
    boolean didChangeTitle = !book.getTitle().equals(newTitle);

    book.setTitle(newTitle);
    book.setAuthor(newAuthor);
    book.setCoverImageUrl(mCoverURL);

    if(mTrackUsingPages.isChecked()) {
      int discretePages = Integer.parseInt(mPageCountEdit.getText().toString());
      book.setPageCount((float) discretePages);
    } else {
      book.setPageCount(null);
    }

    if(mCurrentBookColor != 1) {
      book.setColor(mCurrentBookColor);
    }

    new UpdateBookTask(this, book, didChangeTitle).execute();
  }

  /**
   * Validates the input fields and moves focus to any invalid ones.
   *
   * @return true if all fields were valid, otherwise false
   */
  private boolean validateFieldsAndUpdateState() {
    mTitleEdit.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
    mAuthorEdit.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
    mPageCountEdit.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);

    boolean fieldsAreValid = true;

    if(mTitleEdit.getText().length() < 1) {
      mTitleEdit.setCompoundDrawablesWithIntrinsicBounds(0, 0, android.R.drawable.stat_notify_error, 0);
      fieldsAreValid = false;
    }

    if(mAuthorEdit.getText().length() < 1) {
      mAuthorEdit.setCompoundDrawablesWithIntrinsicBounds(0, 0, android.R.drawable.stat_notify_error, 0);
      fieldsAreValid = false;
    }

    // Validate a reasonable amount of page numbers
    if(mTrackUsingPages.isChecked()) {
      int pageCount = 0;

      try {
        pageCount = Integer.parseInt(mPageCountEdit.getText().toString());
      } catch(NumberFormatException ignored) {
      }

      if(pageCount < 1) {
        mPageCountEdit.setCompoundDrawablesWithIntrinsicBounds(0, 0, android.R.drawable.stat_notify_error, 0);
        fieldsAreValid = false;
      }
    }

    mSaveButton.setEnabled(fieldsAreValid);

    return fieldsAreValid;
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
    static final Pattern DUPE_COUNT_PATTERN = Pattern.compile("^[(](\\d+)[)](.*)");

    final WeakReference<BookSettingsActivity> mActivity;
    final DatabaseManager mDatabaseMgr;
    final Book mBook;
    final String mUnknownTitleString;

    BackgroundBookTask(BookSettingsActivity activity, Book book) {
      mBook = book;
      mActivity = new WeakReference<>(activity);
      mDatabaseMgr = activity.getApp().getDatabaseManager();
      mUnknownTitleString = activity.getString(R.string.general_unknown_title);
    }

    abstract protected boolean run();

    abstract protected void onComplete(BookSettingsActivity activity, boolean success);

    @Override
    protected Boolean doInBackground(Void... voids) {
      return run();
    }

    @Override protected void onPostExecute(Boolean success) {
      BookSettingsActivity activity = mActivity.get();
      if(activity != null) {
        onComplete(activity, success);
      }
    }
  }

  static class UpdateBookTask extends BackgroundBookTask {
    private final boolean mShouldMakeTitleUnique;

    UpdateBookTask(BookSettingsActivity activity, Book book, boolean shouldMakeTitleUnique) {
      super(activity, book);
      mShouldMakeTitleUnique = shouldMakeTitleUnique;
    }

    @Override protected boolean run() {
      if(mShouldMakeTitleUnique) {
        mBook.setTitle(getUniqueTitle(mBook.getTitle()));
      }
      return mDatabaseMgr.save(mBook);
    }

    @Override protected void onComplete(BookSettingsActivity activity, boolean success) {
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
    DeleteBookTask(BookSettingsActivity activity, Book book) {
      super(activity, book);
    }

    @Override protected boolean run() {
      return mDatabaseMgr.delete(mBook);
    }

    @Override protected void onComplete(BookSettingsActivity activity, boolean success) {
      activity.onBookDeleted(mBook.getId(), success);
    }
  }

  private class CoverLoadPicassoTarget implements Target {
    final String attemptedCoverUrl;

    public CoverLoadPicassoTarget(String coverUrl) {
      this.attemptedCoverUrl = coverUrl;
    }

    @Override public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
      // Set the cover url field, so that the it's saved when the book is updated
      Log.d(TAG, "picasso: onSuccess");
      // Write the cover url in case the image was set using the refresh cover button. By setting
      // the value we ensure that it's written into the book when saved.
      mCoverURL = attemptedCoverUrl;
      mCoverImage.setImageBitmap(bitmap);
      new Palette.Builder(bitmap).maximumColorCount(32).generate(new Palette.PaletteAsyncListener() {
        @Override public void onGenerated(@NonNull Palette palette) {
          mRefreshCoverButton.setVisibility(View.GONE);
          mSuggestedColors.clear();
          final Palette.Swatch vibrant = palette.getVibrantSwatch();
          if(vibrant != null) {
            mSuggestedColors.add(vibrant.getRgb());
          }

          final Palette.Swatch dominantSwatch = palette.getDominantSwatch();
          if(dominantSwatch != null) {
            mSuggestedColors.add(dominantSwatch.getRgb());
          }

          final Palette.Swatch muteSwatch = palette.getMutedSwatch();
          if(muteSwatch != null) {
            mSuggestedColors.add(muteSwatch.getRgb());
          }

          resetColorButtons();
        }
      });
    }

    @Override public void onBitmapFailed(Drawable errorDrawable) {
      Log.d(TAG, "Loading cover %s failed");
    }

    @Override public void onPrepareLoad(Drawable placeHolderDrawable) {
      // NOOP
    }
  }
}
