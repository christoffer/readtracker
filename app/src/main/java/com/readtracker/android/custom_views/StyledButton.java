package com.readtracker.android.custom_views;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.Button;

import com.readtracker.android.support.UiUtils;

public class StyledButton extends Button {
  public StyledButton(Context context) {
    this(context, null);
  }

  public StyledButton(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public StyledButton(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
    UiUtils.applyFontStyle(this, attrs);
  }
}
