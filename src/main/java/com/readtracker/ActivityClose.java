package com.readtracker;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import com.readtracker.custom_views.ViewBindingBookHeader;
import com.readtracker.db.LocalReading;
import com.readtracker.helpers.ReadmillApiHelper;
import com.readtracker.interfaces.SaveLocalReadingListener;
import com.readtracker.tasks.SaveLocalReadingTask;

import java.util.Date;

/**
 * Screen for finishing a book with an optional closing remark
 */
public class ActivityClose extends ReadTrackerActivity {
  private static boolean mIsAbandon;
  private static LocalReading mLocalReading;

  private static TextView mTextFinishTitle;
  private static TextView mTextFinishAction;
  private static TextView mTextFinishTime;
  private static EditText mEditClosingRemark;
  private static Button mButtonSubmit;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.close);
    mLocalReading = getIntent().getParcelableExtra(IntentKeys.LOCAL_READING);
    mIsAbandon = getIntent().getBooleanExtra(IntentKeys.SHOULD_ABANDON, false);

    bindViews();
    bindEvents();
    setupViews();
    initClosingRemark();
    mButtonSubmit.setText(mIsAbandon ? "Abandon" : "Finish");
  }

  private void bindViews() {
    mTextFinishTitle = (TextView) findViewById(R.id.textFinishTitle);
    mTextFinishAction = (TextView) findViewById(R.id.textFinishAction);
    mTextFinishTime = (TextView) findViewById(R.id.textFinishTime);
    mEditClosingRemark = (EditText) findViewById(R.id.editClosingRemark);
    mButtonSubmit = (Button) findViewById(R.id.buttonSubmitClosingRemark);
  }

  private void bindEvents() {
    mButtonSubmit.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) { finishReading(); }
    });
  }

  private void setupViews() {
    String actionText = mIsAbandon ? "Abandoning" : "Finishing";
    String buttonText = mIsAbandon ? "Abandon" : "Finish";
    String readingTime = Utils.longHumanTimeFromMillis(mLocalReading.timeSpentMillis);

    ViewBindingBookHeader.bindWithDefaultClickHandler(this, mLocalReading);

    mTextFinishAction.setText(actionText);
    mTextFinishTitle.setText(mLocalReading.title);
    mTextFinishTime.setText(readingTime);
    mButtonSubmit.setText(buttonText);
  }

  private void finishReading() {
    if(!validateClosingRemark()) {
      return;
    }

    mLocalReading.readmillClosingRemark = mEditClosingRemark.getText().toString();

    mLocalReading.readmillState = mIsAbandon ?
        ReadmillApiHelper.ReadingState.ABANDONED :
        ReadmillApiHelper.ReadingState.FINISHED;

    mLocalReading.locallyClosedAt = (new Date()).getTime();

    saveLocalReading();
  }

  private boolean validateClosingRemark() {
    int remarkLength = mEditClosingRemark.getText().length();
    if(remarkLength > 999) {
      toast("The maximum lenght for a closing remark is 999 characters.\nYou've typed " + (remarkLength - 999) + " characters more than that.");
      return false;
    }

    return true;
  }

  private void initClosingRemark() {
    mEditClosingRemark.setText(mLocalReading.readmillClosingRemark);
  }

  private void saveLocalReading() {
    (new SaveLocalReadingTask(new SaveLocalReadingListener() {
      @Override
      public void onLocalReadingSaved(LocalReading localReading) {
        postLocalReadingSaved(localReading);
      }
    })).execute(mLocalReading);
  }

  private void postLocalReadingSaved(LocalReading localReading) {
    if(localReading == null) {
      toast("Unfortunately the closing remark could not be saved");
      setResult(ActivityCodes.RESULT_CANCELED);
    } else {
      Intent data = new Intent();
      data.putExtra(IntentKeys.LOCAL_READING, mLocalReading);
      setResult(ActivityCodes.RESULT_OK, data);
    }
    finish();
  }

}
