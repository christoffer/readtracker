package com.readtracker.android.custom_views;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.EditText;

import com.readtracker.android.support.UiUtils;

public class StyledEditText extends EditText {
  public StyledEditText(Context context) {
    this(context, null);
  }

  public StyledEditText(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public StyledEditText(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
    UiUtils.applyFontStyle(this, attrs);
  }
}
