package com.readtracker;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import com.readtracker.customviews.ProgressPicker;
import com.readtracker.customviews.ViewBindingBookHeader;
import com.readtracker.db.LocalHighlight;
import com.readtracker.db.LocalReading;
import com.readtracker.interfaces.CreateHighlightTaskListener;
import com.readtracker.tasks.CreateHighlightAsyncTask;

import java.util.Date;

/**
 * Screen for adding a highlight
 */
public class ActivityHighlight extends ReadTrackerActivity {
  private static EditText mEditHighlightText;
  private static Button mSaveHighlightButton;

  private static ProgressPicker mProgressPicker;

  private LocalReading mLocalReading;

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
      mEditHighlightText.setText(savedInstanceState.getString(IntentKeys.TEXT));
      currentPage = savedInstanceState.getInt(IntentKeys.PAGE);
    } else {
      Bundle extras = getIntent().getExtras();
      mLocalReading = (LocalReading) extras.get(IntentKeys.LOCAL_READING);
      mEditHighlightText.setText("");
      currentPage = (int) mLocalReading.currentPage;
    }

    if(mLocalReading.hasPageInfo()) {
      if(mLocalReading.isMeasuredInPercent()) {
        mProgressPicker.setupPercentMode(currentPage);
      } else {
        mProgressPicker.setupPagesMode(currentPage, (int) mLocalReading.totalPages);
      }
    } else {
      mProgressPicker.setVisibility(View.GONE);
    }

    ViewBindingBookHeader.bind(this, mLocalReading);
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    Log.d(TAG, "freezing state");
    outState.putParcelable(IntentKeys.LOCAL_READING, mLocalReading);
    outState.putString(IntentKeys.TEXT, mEditHighlightText.getText().toString());
    if(mLocalReading.hasPageInfo()) {
      outState.putInt(IntentKeys.PAGE, mProgressPicker.getPage());
    }
  }

  private void bindViews() {
    mEditHighlightText = (EditText) findViewById(R.id.editHighlight);
    mSaveHighlightButton = (Button) findViewById(R.id.buttonSaveHighlight);
    mProgressPicker = (ProgressPicker) findViewById(R.id.progressPicker);
  }

  private void bindButtonEvents() {
    mSaveHighlightButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        saveHighlight();
      }
    });
  }

  private void saveHighlight() {
    Log.i(TAG, "Saving highlight for LocalReading with id:" + mLocalReading.id);
    String content = mEditHighlightText.getText().toString();

    if(!validateHighlightContent(content)) {
      return;
    }

    long readmillUserId = getCurrentUserId();

    double position = 0.0f;

    if(mLocalReading.hasPageInfo()) {
      position = mProgressPicker.getProgress();
    }

    LocalHighlight highlight = new LocalHighlight();

    highlight.content = content;
    highlight.readingId = mLocalReading.id;
    highlight.readmillReadingId = mLocalReading.readmillReadingId;
    highlight.readmillUserId = readmillUserId;
    highlight.position = position;
    highlight.highlightedAt = new Date();

    new CreateHighlightAsyncTask(new CreateHighlightTaskListener() {
      @Override
      public void onReadingHighlightCreated(boolean result) {
        onHighlightCreated(result);
      }
    }).execute(highlight);
  }

  private void onHighlightCreated(boolean success) {
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

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
  }
}
