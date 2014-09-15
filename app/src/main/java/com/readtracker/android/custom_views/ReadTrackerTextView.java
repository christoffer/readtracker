package com.readtracker.android.custom_views;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.os.Build;
import android.util.AttributeSet;
import android.widget.TextView;

import com.readtracker.BuildConfig;
import com.readtracker.R;

public class ReadTrackerTextView extends TextView {
  private static Typeface FONT_REGULAR = null;
  private static Typeface FONT_MEDIUM = null;
  private static Typeface FONT_LIGHT = null;

  private FontWeight fontWeight;
  private boolean fontAllCaps;

  public ReadTrackerTextView(Context context) {
    this(context, null);
  }

  public ReadTrackerTextView(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public ReadTrackerTextView(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);

    initFontResources(getContext());

    TypedArray typedArray = context.getTheme().obtainStyledAttributes(
        attrs, R.styleable.ReadTrackerTextView, 0, 0
    );

    try {
      if(typedArray.hasValue(R.styleable.ReadTrackerTextView_fontWeight)) {
        final int fontWeightId = typedArray.getInt(R.styleable.ReadTrackerTextView_fontWeight, -1);
        setFontWeight(FontWeight.from(fontWeightId));
      } else {
        setFontWeight(FontWeight.REGULAR);
      }
      // Compatibility for pre-14 devices, and allows us to tweak the font if we would like
      setFontAllCaps(typedArray.getBoolean(R.styleable.ReadTrackerTextView_fontAllCaps, false));
    } finally {
      typedArray.recycle();
    }
  }

  private static void initFontResources(Context context) {
    if(FONT_REGULAR == null) {
      FONT_REGULAR = Typeface.createFromAsset(context.getAssets(), "fonts/Roboto-Regular.ttf");
    }

    if(FONT_LIGHT == null) {
      FONT_LIGHT = Typeface.createFromAsset(context.getAssets(), "fonts/Roboto-Light.ttf");
    }

    if(FONT_MEDIUM == null) {
      FONT_MEDIUM = Typeface.createFromAsset(context.getAssets(), "fonts/Roboto-Medium.ttf");
    }
  }

  public FontWeight getFontWeight() {
    return fontWeight;
  }

  public void setFontWeight(FontWeight fontWeight) {
    this.fontWeight = fontWeight;

    if(fontWeight == FontWeight.LIGHT) {
      setTypeface(FONT_LIGHT);
    } else if(fontWeight == FontWeight.REGULAR) {
      setTypeface(FONT_REGULAR);
    } else if (fontWeight == FontWeight.MEDIUM) {
      setTypeface(FONT_MEDIUM);
    }
  }

  public boolean isFontAllCaps() {
    return fontAllCaps;
  }

  public void setFontAllCaps(boolean fontAllCaps) {
    this.fontAllCaps = fontAllCaps;
    if(android.os.Build.VERSION.SDK_INT > android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
      setAllCaps(this.fontAllCaps);
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
