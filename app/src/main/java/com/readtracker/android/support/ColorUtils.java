package com.readtracker.android.support;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.ColorDrawable;
import android.util.Log;
import android.util.TypedValue;
import android.widget.NumberPicker;

public class ColorUtils {
  private static String TAG = ColorUtils.class.getName();
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

  static public void setNumberPickerDividerColorHack(NumberPicker numberPicker, int color) {
    // Super hacky introspection magic based on code from
    // https://stackoverflow.com/questions/24233556/changing-numberpicker-divider-color
    final String fieldToHack = "mSelectionDivider";
    java.lang.reflect.Field[] declaredFields = NumberPicker.class.getDeclaredFields();
    for(java.lang.reflect.Field field : declaredFields) {
      if(field.getName().equals(fieldToHack)) {
        field.setAccessible(true);
        try {
          ColorDrawable colorDrawable = new ColorDrawable(color);
          field.set(numberPicker, colorDrawable);
        } catch(IllegalArgumentException e) {
          Log.e(TAG, Log.getStackTraceString(e));
        } catch(Resources.NotFoundException e) {
          Log.e(TAG, Log.getStackTraceString(e));
        } catch(IllegalAccessException e) {
          Log.e(TAG, Log.getStackTraceString(e));
        }
        break;
      }
    }
  }
}
