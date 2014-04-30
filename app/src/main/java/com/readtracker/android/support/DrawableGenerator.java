package com.readtracker.android.support;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.widget.Button;

public class DrawableGenerator {
  // Reuse this temporary container for color conversions
  private static final float[] hsv = new float[3];

  public static StateListDrawable generateButtonBackground(int baseColor) {
    StateListDrawable states = new StateListDrawable();

    baseColor = multiplyHSV(baseColor, 1.0f, 0.8f, 0.5f);

    int fillColor = baseColor;

    // Focused
    int focusedOutlineColor = multiplyHSV(fillColor, 1.0f, 1.05f, 1.3f);
    states.addState(new int[]{ android.R.attr.state_focused }, createButtonDrawable(fillColor, focusedOutlineColor));

    // Pressed
    fillColor = multiplyHSV(baseColor, 1.0f, 1.05f, 1.2f);
    states.addState(new int[]{ android.R.attr.state_pressed }, createButtonDrawable(fillColor, outlineColorFor(fillColor)));

    // Disabled
    fillColor = multiplyHSV(baseColor, 1.0f, 0.5f, 0.5f);
    states.addState(new int[]{ -android.R.attr.state_enabled }, createButtonDrawable(fillColor, outlineColorFor(fillColor)));

    // Default
    fillColor = baseColor;
    states.addState(new int[]{ }, createButtonDrawable(fillColor, outlineColorFor(fillColor)));

    return states;
  }

  public static Drawable generateEditTextOutline(int color, int pixelBorder, int pixelRadius) {
    GradientDrawable gradientDrawable = new GradientDrawable();
    gradientDrawable.setCornerRadius(pixelRadius);
    gradientDrawable.setStroke(pixelBorder, color);
    gradientDrawable.setColor(Color.BLACK);
    return gradientDrawable;
  }

  public static StateListDrawable generateListItemBackground(int activeColor, int baseColor) {
    StateListDrawable states = new StateListDrawable();

    activeColor = multiplyHSV(activeColor, 1.0f, 0.75f, 0.75f);

    // Focused
    GradientDrawable fillDrawable = new GradientDrawable();
    fillDrawable.setStroke(3, activeColor);
    states.addState(new int[]{ android.R.attr.state_focused }, fillDrawable);

    // Pressed
    ColorDrawable pressedDrawable = new ColorDrawable(activeColor);
    fillDrawable.setAlpha(64);
    states.addState(new int[]{ android.R.attr.state_pressed }, pressedDrawable);

    // Default
    states.addState(new int[]{ }, new ColorDrawable(baseColor));

    return states;
  }

  private static int outlineColorFor(int baseColor) {
    return multiplyHSV(baseColor, 1.0f, 1.05f, 1.15f);
  }

  private static GradientDrawable createButtonDrawable(int fillColor, int outlineColor) {
    GradientDrawable drawable = new GradientDrawable();
    drawable.setColor(fillColor);
    drawable.setCornerRadius(3);
    drawable.setStroke(2, outlineColor);
    return drawable;
  }

  private static int multiplyHSV(int color, float adjustH, float adjustS, float adjustV) {
    Color.colorToHSV(color, hsv);
    hsv[0] *= adjustH;
    hsv[1] *= adjustS;
    hsv[2] *= adjustV;
    return Color.HSVToColor(hsv);
  }

  /** Applies a button background drawable to each of the buttons. */
  public static void applyButtonBackground(int color, Button... buttons) {
    for(Button button: buttons) {
      button.setBackgroundDrawable(generateButtonBackground(color));
    }
  }
}
