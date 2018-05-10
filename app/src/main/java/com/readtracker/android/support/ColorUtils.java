package com.readtracker.android.support;

import android.content.Context;
import android.util.TypedValue;

public class ColorUtils {
  private static TypedValue mTemporaryTypedValue = new TypedValue();

  static private int resolveAttrColorFromContext(Context context, int attr) {
    context.getTheme().resolveAttribute(attr, mTemporaryTypedValue, true);
    return mTemporaryTypedValue.data;
  }

  static public int getPrimaryTextColor(Context context) {
    return resolveAttrColorFromContext(context, android.R.attr.textColorPrimary);
  }

  static public int getSecondaryTextColor(Context context) {
    return resolveAttrColorFromContext(context, android.R.attr.textColorSecondary);
  }

  static public int getDisabledTextColor(Context context) {
    return resolveAttrColorFromContext(context, android.R.attr.textColorPrimaryDisableOnly);
  }

  static public int getBackgroundColor(Context context) {
    return resolveAttrColorFromContext(context, android.R.attr.background);
  }

  static public int getPressedColor(Context context) {
    return resolveAttrColorFromContext(context, android.R.attr.colorPressedHighlight);
  }
}
