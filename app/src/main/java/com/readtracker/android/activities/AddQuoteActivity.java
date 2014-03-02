package com.readtracker.android.activities;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.readtracker.android.R;
import com.readtracker.android.custom_views.ProgressPicker;
import com.readtracker.android.db.Book;
import com.readtracker.android.db.DatabaseManager;
import com.readtracker.android.db.Quote;
import com.readtracker.android.support.DrawableGenerator;
import com.readtracker.android.support.Utils;

import java.lang.ref.WeakReference;
import java.util.Date;

/**
 * Screen for adding a quote
 */
public class AddQuoteActivity extends BookBaseActivity {
  private static final String TAG = AddQuoteActivity.class.getSimpleName();
  private static EditText mQuoteTextEdit;
  private static Button mButtonSaveQuote;
  private static ProgressPicker mProgressPicker;

  public static final String KEY_QUOTE_ID = "QUOTE_ID";

  private Book mBook;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.add_quote_activity);

    bindViews();

    loadBookFromIntent();
  }

  @Override
  protected void onBookLoaded(Book book) {
    mBook = book;

    final int color = Utils.calculateBookColor(book);
    final Drawable backgroundDrawable = DrawableGenerator.generateEditTextOutline(color, getPixels(1), getPixels(3));
    mQuoteTextEdit.setBackgroundDrawable(backgroundDrawable);

    final Drawable background = DrawableGenerator.generateButtonBackground(color);
    mButtonSaveQuote.setBackgroundDrawable(background);

    if(book != null && book.hasPageNumbers()) {
      mProgressPicker.setBook(book);
      mProgressPicker.setCurrentPage(book.getCurrentPage());
    } else {
      mProgressPicker.setVisibility(View.GONE);
      findViewById(R.id.textLabelEnterPosition).setVisibility(View.GONE);
    }

    bindButtonEvents();
  }

  private void bindViews() {
    mQuoteTextEdit = (EditText) findViewById(R.id.quote_text_edit);
    mButtonSaveQuote = (Button) findViewById(R.id.save_button);
    mProgressPicker = (ProgressPicker) findViewById(R.id.progress_picker);
  }

  private void bindButtonEvents() {
    mButtonSaveQuote.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        if(validateInput()) {
          Float progress = mProgressPicker.getVisibility() == View.VISIBLE ? mProgressPicker.getProgress() : null;
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
    new SaveTask(mBook, quoteText, position, this).execute();
  }

  private void onQuoteSaved(Quote quote) {
    if(quote != null) {
      Log.d(TAG, "Saved " + quote);
      Intent data = new Intent();
      data.putExtra(KEY_QUOTE_ID, quote.getId());
      setResult(RESULT_OK, data);
      finish();
    } else {
      Log.w(TAG, "Failed to create quote for some reason");
    }
  }

  private static class SaveTask extends AsyncTask<Void, Void, Quote> {
    private Quote mQuote;

    private final WeakReference<AddQuoteActivity> mActivity;
    private final DatabaseManager mDatabaseManager;

    public SaveTask(Book book, String quoteText, Float position, AddQuoteActivity activity) {
      mQuote = new Quote();
      mQuote.setBook(book);
      mQuote.setAddTimestamp(new Date().getTime());
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
