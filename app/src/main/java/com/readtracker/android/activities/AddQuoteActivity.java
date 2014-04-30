package com.readtracker.android.activities;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.readtracker.R;
import com.readtracker.android.custom_views.ProgressPicker;
import com.readtracker.android.db.Book;
import com.readtracker.android.db.DatabaseManager;
import com.readtracker.android.db.Quote;
import com.readtracker.android.support.DrawableGenerator;
import com.readtracker.android.support.Utils;

import java.lang.ref.WeakReference;

import butterknife.ButterKnife;
import butterknife.InjectView;

/**
 * Screen for adding a quote
 */
public class AddQuoteActivity extends BookBaseActivity {
  public static final String KEY_QUOTE_ID = "QUOTE_ID";
  public static final int RESULT_DELETED = RESULT_FIRST_USER + 1;

  private static final String TAG = AddQuoteActivity.class.getSimpleName();

  @InjectView(R.id.quote_text_edit) EditText mQuoteTextEdit;
  @InjectView(R.id.save_button) Button mSaveButton;
  @InjectView(R.id.progress_picker) ProgressPicker mProgressPicker;

  private Book mBook;
  private Quote mEditQuote;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.add_quote_activity);
    ButterKnife.inject(this);

    mSaveButton.setEnabled(false); // hold until book is loaded

    if(getIntent().hasExtra(KEY_QUOTE_ID)) {
      int quoteId = getIntent().getExtras().getInt(KEY_QUOTE_ID);
      Log.v(TAG, "Loading in edit mode: " + quoteId);
      mEditQuote = loadQuote(quoteId);
      if(mEditQuote == null) {
        Log.w(TAG, String.format("Got invalid quote id, no such quote: %d", quoteId));
        setResult(RESULT_CANCELED);
        finish();
      }
    } else {
      Log.v(TAG, "Loading in add mode");
    }

    loadBookFromIntent();
  }

  @Override public boolean onCreateOptionsMenu(Menu menu) {
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.add_quote_menu, menu);
    return super.onCreateOptionsMenu(menu);
  }

  @Override public boolean onPrepareOptionsMenu(Menu menu) {
    MenuItem deleteItem = menu.findItem(R.id.add_quote_delete);
    if(deleteItem != null) {
      deleteItem.setVisible(mEditQuote != null);
    }
    return super.onPrepareOptionsMenu(menu);
  }

  @Override public boolean onOptionsItemSelected(MenuItem item) {
    if(item.getItemId() == R.id.add_quote_delete) {
      confirmDeleteQuote();
      return true;
    }
    return super.onOptionsItemSelected(item);
  }

  private void confirmDeleteQuote() {
    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    final String title = getString(R.string.add_quote_delete_quote);
    builder.setTitle(title);

    builder.setMessage(R.string.add_quote_delete_explanation);

    builder.setPositiveButton(R.string.general_delete, new DialogInterface.OnClickListener() {
      @Override public void onClick(DialogInterface dialogInterface, int i) {
        exitWithResult(mEditQuote, RESULT_DELETED);
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
    mBook = book;

    final int color = Utils.calculateBookColor(book);
    final Drawable backgroundDrawable = DrawableGenerator.generateEditTextOutline(color, getPixels(1), getPixels(3));
    mQuoteTextEdit.setBackgroundDrawable(backgroundDrawable);

    DrawableGenerator.applyButtonBackground(color, mSaveButton);
    mSaveButton.setEnabled(true);

    Float quotePosition = book.getCurrentPosition();
    if(mEditQuote != null) {
      mQuoteTextEdit.setText(mEditQuote.getContent());
      quotePosition = mEditQuote.getPosition();
      mSaveButton.setText(R.string.general_save);
    } else {
      mSaveButton.setText(R.string.general_add);
    }

    if(book.hasPageNumbers()) {
      mProgressPicker.setPositionAndPageCount(quotePosition == null ? 0 : quotePosition, book.getPageCount());
    } else {
      mProgressPicker.setVisibility(View.GONE);
      findViewById(R.id.textLabelEnterPosition).setVisibility(View.GONE);
    }

    bindButtonEvents();
  }

  private Quote loadQuote(int quoteId) {
    return getDatabaseManager().get(Quote.class, quoteId);
  }

  private void bindButtonEvents() {
    mSaveButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        if(validateInput()) {
          Float progress = mProgressPicker.getVisibility() == View.VISIBLE ? mProgressPicker.getPosition() : null;
          saveQuote(mQuoteTextEdit.getText().toString(), progress);
        }
      }
    });
  }

  private boolean validateInput() {
    final String quoteContent = mQuoteTextEdit.getText().toString();
    if(TextUtils.isEmpty(quoteContent)) {
      mQuoteTextEdit.selectAll();
      mQuoteTextEdit.requestFocus();
      return false;
    }

    return true;
  }

  private void saveQuote(String quoteText, Float position) {
    Log.d(TAG, "Saving quote for " + mBook.getTitle() + " [" + quoteText + "] " + position);
    new CreateOrUpdateQuoteTask(mEditQuote, mBook, quoteText, position, this).execute();
  }

  private void onQuoteSaved(Quote quote) {
    if(quote != null) {
      Log.d(TAG, "Saved " + quote);
      exitWithResult(quote, RESULT_OK);
    } else {
      Log.w(TAG, "Failed to create quote for some reason");
    }
  }

  private void exitWithResult(Quote quote, int resultCode) {
    Intent data = new Intent();
    data.putExtra(KEY_QUOTE_ID, quote.getId());
    setResult(resultCode, data);
    finish();
  }

  private static class CreateOrUpdateQuoteTask extends AsyncTask<Void, Void, Quote> {
    private final Quote mQuote;

    private final WeakReference<AddQuoteActivity> mActivity;
    private final DatabaseManager mDatabaseManager;

    public CreateOrUpdateQuoteTask(Quote quote, Book book, String quoteText, Float position, AddQuoteActivity activity) {
      if(quote == null) {
        Log.v(TAG, "Creating quote");
        mQuote = new Quote();
        mQuote.setAddTimestampMs(System.currentTimeMillis());
        mQuote.setBook(book);
      } else {
        mQuote = quote;
      }

      mQuote.setContent(quoteText);
      mQuote.setPosition(position);

      mActivity = new WeakReference<AddQuoteActivity>(activity);
      mDatabaseManager = activity.getApp().getDatabaseManager();
    }

    @Override
    protected Quote doInBackground(Void... voids) {
      mDatabaseManager.save(mQuote);
      return mQuote;
    }

    @Override
    protected void onPostExecute(Quote quote) {
      AddQuoteActivity activity = mActivity.get();
      if(activity != null) {
        activity.onQuoteSaved(quote);
      }
    }
  }
}
