package com.readtracker.android.activities;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.readtracker.android.IntentKeys;
import com.readtracker.android.R;
import com.readtracker.android.db.LocalReading;
import com.readtracker.android.interfaces.SaveLocalReadingListener;
import com.readtracker.android.support.DrawableGenerator;
import com.readtracker.android.tasks.SaveLocalReadingTask;
import com.readtracker.android.thirdparty.views.Switch;

import java.util.Date;

public class FinishBookActivity extends BookBaseActivity {
  private LocalReading mLocalReading;
  private static EditText mEditClosingRemark;
  private static Button mButtonFinish;
  private static Switch mSwitchRecommended;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_finish_book);
    bindViews();
    bindEvents();

    if(savedInstanceState == null) {
      Bundle extras = getIntent().getExtras();
      mLocalReading = extras.getParcelable(IntentKeys.LOCAL_READING);
    } else {
      mLocalReading = savedInstanceState.getParcelable(IntentKeys.LOCAL_READING);
      final String currentClosingRemark = savedInstanceState.getString(IntentKeys.CLOSING_REMARK);
      mEditClosingRemark.setText(currentClosingRemark);
      final boolean isRecommended = savedInstanceState.getBoolean(IntentKeys.RECOMMENDED);
      mSwitchRecommended.setChecked(isRecommended);
    }

    Drawable drawable = DrawableGenerator.generateEditTextOutline(mLocalReading.getColor(), getPixels(1), getPixels(3));
    mEditClosingRemark.setBackgroundDrawable(drawable);

    mButtonFinish.setBackgroundDrawable(DrawableGenerator.generateButtonBackground(mLocalReading.getColor()));

    setReading(mLocalReading);
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    outState.putParcelable(IntentKeys.LOCAL_READING, mLocalReading);
    outState.putString(IntentKeys.CLOSING_REMARK, mEditClosingRemark.getText().toString());
    outState.putBoolean(IntentKeys.RECOMMENDED, mSwitchRecommended.isChecked());
    super.onSaveInstanceState(outState);
  }

  private void bindViews() {
    mEditClosingRemark = (EditText) findViewById(R.id.editClosingRemark);
    mButtonFinish = (Button) findViewById(R.id.buttonFinishBook);
    mSwitchRecommended = (Switch) findViewById(R.id.switchRecommended);
  }

  private void bindEvents() {
    mButtonFinish.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        final String closingRemark = mEditClosingRemark.getText().toString();
        final boolean isRecommended = mSwitchRecommended.isChecked();
        finishReading(mLocalReading,closingRemark, isRecommended);
      }
    });
  }

  private void finishReading(LocalReading localReading, String closingRemark, boolean isRecommended) {
    localReading.readmillClosingRemark = closingRemark;
    localReading.readmillState = LocalReading.ReadingState.FINISHED;
    localReading.setClosedAt(new Date());
    localReading.readmillRecommended = isRecommended;
    (new SaveLocalReadingTask(new SaveLocalReadingListener() {
      @Override
      public void onLocalReadingSaved(LocalReading localReading) {
        finishWithUpdatedReading(localReading);
      }
    })).execute(localReading);
  }

  private void finishWithUpdatedReading(LocalReading localReading) {
    // Pass the local reading as part of the result, since it has been updated
    Intent data = new Intent();
    data.putExtra(IntentKeys.LOCAL_READING, localReading);
    setResult(ActivityCodes.RESULT_OK, data);
    finish();
  }
}
