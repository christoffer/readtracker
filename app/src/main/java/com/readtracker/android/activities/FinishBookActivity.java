package com.readtracker.android.activities;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.readtracker.R;
import com.readtracker.android.db.Book;
import com.readtracker.android.support.DrawableGenerator;
import com.readtracker.android.support.Utils;

import butterknife.ButterKnife;
import butterknife.InjectView;

public class FinishBookActivity extends BookBaseActivity implements View.OnClickListener {
  public static final String KEY_CLOSING_REMARK = "CLOSING_REMARK";

  @InjectView(R.id.editClosingRemark) EditText mEditClosingRemark;
  @InjectView(R.id.finish_button) Button mButtonFinish;

  private Book mBook;
  private boolean mDidLayout;

  // TODO Change button text to "Finish without note" when nothing entered

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setContentView(R.layout.activity_finish_book);
    ButterKnife.inject(this);
    mDidLayout = true;
    populateFieldsDeferred();
    mButtonFinish.setOnClickListener(this);

    loadBookFromIntent();
  }

  @Override protected void onBookLoaded(Book book) {
    mBook = book;
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
