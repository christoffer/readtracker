package com.readtracker;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import com.readtracker.customviews.ViewBindingBookHeader;
import com.readtracker.db.LocalHighlight;
import com.readtracker.db.LocalReading;
import com.readtracker.interfaces.CreateHighlightTaskListener;
import com.readtracker.tasks.CreateHighlightAsyncTask;
import com.readtracker.thirdparty.widget.WheelView;

import java.util.Date;

/**
 * Screen for adding a highlight
 */
public class ActivityHighlight extends ReadTrackerActivity {
  private static EditText mEditHighlightText;
  private static WheelView mWheelHighlightPage;
  private static Button mSaveHighlightButton;
  private static ViewGroup mPageWheelLayout;

  private LocalReading mLocalReading;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_highlight);

    bindViews();
    bindButtonEvents();

    if(savedInstanceState != null) {
      Log.d(TAG, "unfreezing state");
      mLocalReading = savedInstanceState.getParcelable(IntentKeys.LOCAL_READING);
      mEditHighlightText.setText(savedInstanceState.getString(IntentKeys.TEXT));
      mWheelHighlightPage.setTag(savedInstanceState.getInt(IntentKeys.PAGE));
    } else {
      Bundle extras = getIntent().getExtras();
      mLocalReading = (LocalReading) extras.get(IntentKeys.LOCAL_READING);
      mEditHighlightText.setText("");
      mWheelHighlightPage.setTag(null);
    }

//    if(mLocalReading.hasPageInfo()) {
//      NumericWheelAdapter mHighlightPageAdapter = new NumericWheelAdapter(this, 0, (int) mLocalReading.totalPages);
//      mWheelHighlightPage.setViewAdapter(mHighlightPageAdapter);
//      if(mWheelHighlightPage.getTag() == null) {
//        mWheelHighlightPage.setCurrentItem((int) mLocalReading.currentPage);
//      } else {
//        mWheelHighlightPage.setCurrentItem((Integer) mWheelHighlightPage.getTag());
//      }
//      mWheelHighlightPage.setInterpolator(null);
//      mWheelHighlightPage.setVisibleItems(3);
//      mPageWheelLayout.setVisibility(View.VISIBLE);
//    } else {
//      mPageWheelLayout.setVisibility(View.GONE);
//    }
//

    // Remove page entry for now. The page picker should be extracted into a
    // custom view at some point instead of duplicating it here.
    mPageWheelLayout.setVisibility(View.GONE);

    ViewBindingBookHeader.bind(this, mLocalReading);
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    Log.d(TAG, "freezing state");
    outState.putParcelable(IntentKeys.LOCAL_READING, mLocalReading);
    outState.putString(IntentKeys.TEXT, mEditHighlightText.getText().toString());
    outState.putInt(IntentKeys.PAGE, mWheelHighlightPage.getCurrentItem());
  }

  private void bindViews() {
    mEditHighlightText = (EditText) findViewById(R.id.editHighlightText);
    mWheelHighlightPage = (WheelView) findViewById(R.id.wheelHighlightPage);
    mSaveHighlightButton = (Button) findViewById(R.id.btnSaveHighlight);
    mPageWheelLayout = (ViewGroup) findViewById(R.id.pageWheelLayout);
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
      position = (double) mWheelHighlightPage.getCurrentItem() / (double) mLocalReading.totalPages;
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

    if(content.length() > 999) {
      toastLong("The maximum lenght of a highlight is 999 characters. You have entered " + (content.length() - 999) + " characters to many.");
      return false;
    }

    return true;
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
  }
}
