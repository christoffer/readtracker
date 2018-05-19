package com.readtracker.android.support;

import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.util.Log;
import android.widget.NumberPicker;

import com.readtracker.android.db.Book;

import java.lang.reflect.Field;

/**
 * UI utilities for dealing with colors.
 */
public class ColorUtils {
  private static String TAG = ColorUtils.class.getName();

  /**
   * Set the colors of divides on a NumberPicker.
   *
   * Unfortunately Android API doesn't seem to offer a proper API for changing the color of
   * the dividers on the NumberPicker widget. After some Googling, it seems like the solution
   * below -- while super hacky -- is the common suggestion for solving this.
   *
   * See https://stackoverflow.com/questions/24233556/changing-numberpicker-divider-color
   *
   * @param numberPicker NumberPicker instance to set colors for
   * @param color The color to use for the NumberPicker dividers
   */
  static public void setNumberPickerDividerColorUsingHack(NumberPicker numberPicker, int color) {
    final String fieldToHack = "mSelectionDivider";
    final Field[] declaredFields = NumberPicker.class.getDeclaredFields();
    for(Field field : declaredFields) {
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

  /**
   * Returns a color for the book instance.
   */
  public static int getColorForBook(Book book) {
    final String colorKey = book.getTitle() + book.getAuthor();
    float color = 360 * (Math.abs(colorKey.hashCode()) / (float) Integer.MAX_VALUE);
    return Color.HSVToColor(new float[]{color, 0.8f, 1.0f});
  }
}
