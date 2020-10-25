package com.readtracker.android.activities;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatButton;
import android.view.View;
import android.widget.EditText;

import com.readtracker.R;
import com.readtracker.android.db.Book;
import com.readtracker.android.support.ColorUtils;
import com.readtracker.databinding.ActivityFinishBookBinding;

public class FinishBookActivity extends BookBaseActivity implements View.OnClickListener {
  public static final String KEY_CLOSING_REMARK = "CLOSING_REMARK";

  private EditText mEditClosingRemark;
  private AppCompatButton mButtonFinish;

  private Book mBook;
  private boolean mDidLayout;

  // TODO Change button text to "Finish without note" when nothing entered

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    @NonNull ActivityFinishBookBinding binding = ActivityFinishBookBinding.inflate(getLayoutInflater());
    mEditClosingRemark = binding.editClosingRemark;
    mButtonFinish = binding.finishButton;
    setContentView(binding.getRoot());

    mDidLayout = true;
    populateFieldsDeferred();
    mButtonFinish.setOnClickListener(this);

    loadBookFromIntent();
  }

  @Override protected void onBookLoaded(Book book) {
    mBook = book;
    populateFieldsDeferred();
  }

  @Override protected String getActivityTitle(Book book) {
    return getString(R.string.finish_book_finished_reading);
  }

  @Override protected String getActivitySubTitle(Book book) {
    return book.getTitle();
  }

  private void populateFieldsDeferred() {
    if(!mDidLayout || mBook == null) {
      return;
    }

    final int color = ColorUtils.getColorForBook(mBook);
    ColorUtils.applyButtonColor(color, mButtonFinish);
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
