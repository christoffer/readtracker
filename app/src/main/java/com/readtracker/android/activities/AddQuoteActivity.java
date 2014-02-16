package com.readtracker.android.activities;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.readtracker.android.IntentKeys;
import com.readtracker.android.R;
import com.readtracker.android.custom_views.ProgressPicker;
import com.readtracker.android.db.LocalHighlight;
import com.readtracker.android.db.LocalReading;
import com.readtracker.android.interfaces.PersistLocalHighlightListener;
import com.readtracker.android.support.DrawableGenerator;
import com.readtracker.android.tasks.PersistLocalHighlightTask;

import java.util.Date;

/** Screen for adding a quote */
public class AddQuoteActivity extends BookBaseActivity {
  private static EditText mQuoteTextEdit;
  private static Button mButtonSaveQuote;

  private static ProgressPicker mProgressPicker;

  private LocalReading mLocalReading;
  private LocalHighlight mLocalHighlight;

  private boolean mCreateMode = false;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.add_quote_activity);

    bindViews();
    bindButtonEvents();

    int currentPage;

    if (savedInstanceState != null) {
      Log.d(TAG, "unfreezing state");
      mLocalReading = savedInstanceState.getParcelable(IntentKeys.LOCAL_READING);
      mLocalHighlight = savedInstanceState.getParcelable(IntentKeys.LOCAL_HIGHLIGHT);
      mQuoteTextEdit.setText(savedInstanceState.getString(IntentKeys.TEXT));
      currentPage = savedInstanceState.getInt(IntentKeys.PAGE);
    } else {
      Bundle extras = getIntent().getExtras();
      mLocalReading = (LocalReading) extras.get(IntentKeys.LOCAL_READING);
      mLocalHighlight = (LocalHighlight) extras.get(IntentKeys.LOCAL_HIGHLIGHT);

      if (mLocalHighlight == null) {
        mLocalHighlight = new LocalHighlight();
        mCreateMode = true;
        mQuoteTextEdit.setText("");
        currentPage = (int) mLocalReading.currentPage;
      } else {
        mCreateMode = false;
        mQuoteTextEdit.setText(mLocalHighlight.content);
        currentPage = (int) (mLocalHighlight.position * mLocalReading.totalPages);
      }

      Log.d(TAG, "Starting activity in " + (mCreateMode ? "creation" : "edit") + " mode");
    }

    if (mLocalReading.hasPageInfo()) {
      mProgressPicker.setupForLocalReading(mLocalReading);
      mProgressPicker.setCurrentPage(currentPage);
    } else {
      mProgressPicker.setVisibility(View.GONE);
      findViewById(R.id.textLabelEnterPosition).setVisibility(View.GONE);
    }

    setEditTextBackground(mQuoteTextEdit);
    setButtonBackground(mButtonSaveQuote);

    setReading(mLocalReading);
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    Log.d(TAG, "freezing state");
    outState.putParcelable(IntentKeys.LOCAL_READING, mLocalReading);
    outState.putParcelable(IntentKeys.LOCAL_HIGHLIGHT, mLocalHighlight);
    outState.putString(IntentKeys.TEXT, mQuoteTextEdit.getText().toString());
    if (mLocalReading.hasPageInfo()) {
      outState.putInt(IntentKeys.PAGE, mProgressPicker.getCurrentPage());
    }
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
  }

  private void bindViews() {
    mQuoteTextEdit = (EditText) findViewById(R.id.quote_text_edit);
    mButtonSaveQuote = (Button) findViewById(R.id.save_button);
    mProgressPicker = (ProgressPicker) findViewById(R.id.progressPicker);
  }

  private void bindButtonEvents() {
    mButtonSaveQuote.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        saveOrCreateHighlight();
      }
    });
  }

  private void setEditTextBackground(EditText editText) {
    Drawable backgroundDrawable;
    backgroundDrawable = DrawableGenerator.generateEditTextOutline(
      mLocalReading.getColor(), getPixels(1), getPixels(3)
    );
    editText.setBackgroundDrawable(backgroundDrawable);
  }

  private void setButtonBackground(Button button) {
    final Drawable background = DrawableGenerator.generateButtonBackground(mLocalReading.getColor());
    button.setBackgroundDrawable(background);
  }

  private void saveOrCreateHighlight() {
    Log.i(TAG, "Save/Create highlight for LocalReading with id:" + mLocalReading.id);
    String content = mQuoteTextEdit.getText().toString().trim();

    if (!validateFields()) {
      return;
    }

    double position = 0.0f;

    if (mLocalReading.hasPageInfo()) {
      position = mProgressPicker.getProgress();
    }

    mLocalHighlight.content = content;
    mLocalHighlight.position = position;

    if (mCreateMode) {
      mLocalHighlight.highlightedAt = new Date();
      mLocalHighlight.readingId = mLocalReading.id;
      mLocalHighlight.readmillReadingId = mLocalReading.readmillReadingId;
    } else {
      mLocalHighlight.editedAt = new Date();
    }

    persistHighlight(mLocalHighlight);
  }

  private void persistHighlight(LocalHighlight localHighlight) {
    PersistLocalHighlightTask.persist(localHighlight, new PersistLocalHighlightListener() {
      @Override public void onLocalHighlightPersisted(int id, boolean created) {
        Log.d(TAG, "Persisted local highlight, id: " + id + " created: " + created);
        onHighlightPersisted(true);
      }

      @Override public void onLocalHighlightPersistedFailed() {
        Log.d(TAG, "Failed to persist local highlight");
        onHighlightPersisted(false);
      }
    });
  }

  private void onHighlightPersisted(boolean success) {
    if (!success) {
      toastLong(getString(R.string.add_quote_error_could_not_be_saved));
      return;
    }

    Intent resultIntent = new Intent();
    resultIntent.putExtra(IntentKeys.BOOK_ID, mLocalReading.id);
    setResult(ActivityCodes.RESULT_OK, resultIntent);
    finish();
  }

  private boolean validateFields() {
    final String quoteText = mQuoteTextEdit.getText().toString().trim();

    if (quoteText.length() == 0) {
      toastLong(getString(R.string.add_book_enter_quote));
      mQuoteTextEdit.requestFocus();
      return false;
    }

    return true;
  }
}
