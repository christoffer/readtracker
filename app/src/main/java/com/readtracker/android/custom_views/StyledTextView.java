package com.readtracker.android.custom_views;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;

import com.readtracker.android.support.UiUtils;

public class StyledTextView extends TextView {
  public StyledTextView(Context context) {
    this(context, null);
  }

  public StyledTextView(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public StyledTextView(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
    UiUtils.applyFontStyle(this, attrs);
  }
}
