package com.readtracker.android.support;

import android.content.res.AssetManager;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import com.readtracker.R;

/**
 * Utilities for maintaining a consistent UI style.
 */
public class UiUtils {
  private static Typeface FONT_REGULAR = null;
  private static Typeface FONT_MEDIUM = null;
  private static Typeface FONT_LIGHT = null;

  public static void applyFontStyle(TextView view, AttributeSet attrs) {
    if(!view.isInEditMode()) {
      initFontResources(view);
    }

    TypedArray typedArray = view.getContext().getTheme().obtainStyledAttributes(
        attrs, R.styleable.FontStyle, 0, 0
    );

    try {
      if(typedArray.hasValue(R.styleable.FontStyle_fontWeight)) {
        final int fontWeightId = typedArray.getInt(R.styleable.FontStyle_fontWeight, -1);
        applyFontWeight(view, FontWeight.from(fontWeightId));
      } else {
        applyFontWeight(view, FontWeight.REGULAR);
      }

      final boolean allCaps = typedArray.getBoolean(R.styleable.FontStyle_fontAllCaps, false);
      if(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
        view.setAllCaps(allCaps);
      }

    } finally {
      typedArray.recycle();
    }
  }

  private static void applyFontWeight(TextView view, FontWeight fontWeight) {
    if(view.isInEditMode()) {
      // Edit mode doesn't support custom typefaces
      return;
    }

    if(fontWeight == FontWeight.LIGHT) {
      view.setTypeface(FONT_LIGHT);
    } else if(fontWeight == FontWeight.REGULAR) {
      view.setTypeface(FONT_REGULAR);
    } else if(fontWeight == FontWeight.MEDIUM) {
      view.setTypeface(FONT_MEDIUM);
    }
  }

  private static void initFontResources(View view) {
    if(view.isInEditMode()) {
      return;
    }

    final AssetManager assets = view.getContext().getAssets();

    if(FONT_REGULAR == null) {
      FONT_REGULAR = Typeface.createFromAsset(assets, "fonts/Roboto-Regular.ttf");
    }

    if(FONT_LIGHT == null) {
      FONT_LIGHT = Typeface.createFromAsset(assets, "fonts/Roboto-Light.ttf");
    }

    if(FONT_MEDIUM == null) {
      FONT_MEDIUM = Typeface.createFromAsset(assets, "fonts/Roboto-Medium.ttf");
    }
  }

  public static enum FontWeight {
    LIGHT(0), REGULAR(1), MEDIUM(2);

    private final int id;

    FontWeight(int id) {
      this.id = id;
    }

    public static FontWeight from(int id) {
      for(FontWeight fw : values()) {
        if(fw.id == id) {
          return fw;
        }
      }

      throw new IllegalArgumentException("Invalid identifier: " + id);
    }
  }
}
