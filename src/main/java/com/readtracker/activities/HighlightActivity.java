package com.readtracker.activities;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import com.readtracker.IntentKeys;
import com.readtracker.R;
import com.readtracker.ReadmillTransferIntent;
import com.readtracker.custom_views.ProgressPicker;
import com.readtracker.db.LocalHighlight;
import com.readtracker.db.LocalReading;
import com.readtracker.interfaces.PersistLocalHighlightListener;
import com.readtracker.support.DrawableGenerator;
import com.readtracker.tasks.PersistLocalHighlightTask;

import java.util.Date;

/**
 * Screen for adding a highlight
 */
public class HighlightActivity extends ReadTrackerActivity {
  private static EditText mEditHighlightText;
  private static EditText mEditHighlightComment;
  private static Button mButtonSaveHighlight;

  private static ProgressPicker mProgressPicker;

  private LocalReading mLocalReading;
  private LocalHighlight mLocalHighlight;

  private boolean mCreateMode = false;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_highlight);

    bindViews();
    bindButtonEvents();

    int currentPage;

    if(savedInstanceState != null) {
      Log.d(TAG, "unfreezing state");
      mLocalReading = savedInstanceState.getParcelable(IntentKeys.LOCAL_READING);
      mLocalHighlight = savedInstanceState.getParcelable(IntentKeys.LOCAL_HIGHLIGHT);
      mEditHighlightText.setText(savedInstanceState.getString(IntentKeys.TEXT));
      currentPage = savedInstanceState.getInt(IntentKeys.PAGE);
    } else {
      Bundle extras = getIntent().getExtras();
      mLocalReading = (LocalReading) extras.get(IntentKeys.LOCAL_READING);
      mLocalHighlight = (LocalHighlight) extras.get(IntentKeys.LOCAL_HIGHLIGHT);

      if(mLocalHighlight == null) {
        mLocalHighlight = new LocalHighlight();
        mCreateMode = true;
        mEditHighlightText.setText("");
        currentPage = (int) mLocalReading.currentPage;
      } else {
        mCreateMode = false;
        mEditHighlightText.setText(mLocalHighlight.content);
        currentPage = (int) (mLocalHighlight.position * mLocalReading.totalPages);
      }

      Log.d(TAG, "Starting activity in " + (mCreateMode ? "creation" : "edit") + " mode");
    }

    if(mLocalReading.hasPageInfo()) {
      mProgressPicker.setupForLocalReading(mLocalReading);
      mProgressPicker.setCurrentPage(currentPage);
    } else {
      mProgressPicker.setVisibility(View.GONE);
      findViewById(R.id.textLabelEnterPosition).setVisibility(View.GONE);
    }

    if(getCurrentUser() == null) {
      // Hide commenting for anonymous users
      findViewById(R.id.layoutHighlightComment).setVisibility(View.GONE);
    }

    setBackgroundDrawable(mEditHighlightText);
    setBackgroundDrawable(mEditHighlightComment);
    mButtonSaveHighlight.setBackgroundDrawable(DrawableGenerator.generateButtonBackground(mLocalReading.getColor()));

    ViewBindingBookHeader.bindWithDefaultClickHandler(this, mLocalReading);
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    Log.d(TAG, "freezing state");
    outState.putParcelable(IntentKeys.LOCAL_READING, mLocalReading);
    outState.putParcelable(IntentKeys.LOCAL_HIGHLIGHT, mLocalHighlight);
    outState.putString(IntentKeys.TEXT, mEditHighlightText.getText().toString());
    if(mLocalReading.hasPageInfo()) {
      outState.putInt(IntentKeys.PAGE, mProgressPicker.getCurrentPage());
    }
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
  }

  private void bindViews() {
    mEditHighlightText = (EditText) findViewById(R.id.editHighlight);
    mEditHighlightComment = (EditText) findViewById(R.id.editHighlightComment);
    mButtonSaveHighlight = (Button) findViewById(R.id.buttonSaveHighlight);
    mProgressPicker = (ProgressPicker) findViewById(R.id.progressPicker);
  }

  private void bindButtonEvents() {
    mButtonSaveHighlight.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        saveOrCreateHighlight();
      }
    });
  }

  private void setBackgroundDrawable(View view) {
    Drawable backgroundDrawable;
    backgroundDrawable = DrawableGenerator.generateEditTextOutline(
      mLocalReading.getColor(), getPixels(1), getPixels(3)
    );
    view.setBackgroundDrawable(backgroundDrawable);
  }

  private void saveOrCreateHighlight() {
    Log.i(TAG, "Save/Create highlight for LocalReading with id:" + mLocalReading.id);
    String content = mEditHighlightText.getText().toString();
    String comment = mEditHighlightComment.getText().toString();

    if(!validateHighlightContent(content)) {
      return;
    }

    long readmillUserId = getCurrentUserId();
    double position = 0.0f;

    if(mLocalReading.hasPageInfo()) {
      position = mProgressPicker.getProgress();
    }

    mLocalHighlight.content = content;
    mLocalHighlight.position = position;

    if(comment.length() > 0) {
      mLocalHighlight.comment = comment;
    }

    if(mCreateMode) {
      mLocalHighlight.highlightedAt = new Date();
      mLocalHighlight.readingId = mLocalReading.id;
      mLocalHighlight.readmillReadingId = mLocalReading.readmillReadingId;
      mLocalHighlight.readmillUserId = readmillUserId;
    }

    persistHighlight(mLocalHighlight);
  }

  private void persistHighlight(LocalHighlight localHighlight) {
    new PersistLocalHighlightTask(new PersistLocalHighlightListener() {
      @Override public void onLocalHighlightPersisted(int id, boolean created) {
        Log.d(TAG, "Persisted local highlight, id: " + id + " created: " + created);
        onHighlightPersisted(true);
      }

      @Override public void onLocalHighlightPersistedFailed() {
        Log.d(TAG, "Failed to persist local highlight");
        onHighlightPersisted(false);
      }
    }).execute(localHighlight);
  }

  private void onHighlightPersisted(boolean success) {
    if(!success) {
      toastLong("An error occurred. The highlight could not be saved.");
      return;
    }

    // Push the changes to Readmill
    startService(new Intent(this, ReadmillTransferIntent.class));

    Intent resultIntent = new Intent();
    resultIntent.putExtra(IntentKeys.READING_ID, mLocalReading.id);
    setResult(ActivityCodes.RESULT_OK, resultIntent);
    finish();
  }

  private boolean validateHighlightContent(String content) {
    content = content.trim();

    if(content.length() == 0) {
      toastLong("Please enter some text for the highlight.");
      return false;
    }

    if(content.length() > 2000) {
      toastLong("The highlight is " + (content.length() - 2000) + " characters to long.");
      return false;
    }

    return true;
  }
}
