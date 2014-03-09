package com.readtracker.android.activities;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.readtracker.android.R;
import com.readtracker.android.db.Book;
import com.readtracker.android.support.DrawableGenerator;
import com.readtracker.android.support.Utils;

public class FinishBookActivity extends BookBaseActivity implements View.OnClickListener {
  public static final String KEY_CLOSING_REMARK = "CLOSING_REMARK";

  private static final String STATE_CLOSING_REMARK = "CLOSING_REMARK";

  private EditText mEditClosingRemark;
  private Button mButtonFinish;

  private Book mBook;
  private boolean mDidLayout;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setContentView(R.layout.activity_finish_book);
    bindViews();
    mButtonFinish.setOnClickListener(this);

    loadBookFromIntent();

    if(savedInstanceState != null) {
      final String currentClosingRemark = savedInstanceState.getString(STATE_CLOSING_REMARK);
      mEditClosingRemark.setText(currentClosingRemark);
    }
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    outState.putString(STATE_CLOSING_REMARK, String.valueOf(mEditClosingRemark.getText()));
    super.onSaveInstanceState(outState);
  }

  @Override protected void onBookLoaded(Book book) {
    populateFieldsDeferred();
  }

  private void populateFieldsDeferred() {
    if(!mDidLayout || mBook == null) {
      return;
    }

    final int color = Utils.calculateBookColor(mBook);
    Drawable drawable = DrawableGenerator.generateEditTextOutline(color, getPixels(1), getPixels(3));
    mEditClosingRemark.setBackgroundDrawable(drawable);

    mButtonFinish.setBackgroundDrawable(DrawableGenerator.generateButtonBackground(color));
  }

  private void bindViews() {
    mEditClosingRemark = (EditText) findViewById(R.id.editClosingRemark);
    mButtonFinish = (Button) findViewById(R.id.finish_button);
    mDidLayout = true;
    populateFieldsDeferred();
  }

  @Override public void onClick(View view) {
    if(view == mButtonFinish) {
      final String closingRemark = String.valueOf(mEditClosingRemark.getText());
      Intent data = new Intent();
      data.putExtra(KEY_CLOSING_REMARK, closingRemark);
      setResult(RESULT_OK, data);
      finish();
    }
  }
}
