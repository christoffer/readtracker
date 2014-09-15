package com.readtracker.android.custom_views;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.TextView;

import com.readtracker.R;

public class ReadTrackerTextView extends TextView {
  private final FontWeight fontWeight;
  private final boolean fontAllCaps;

  public ReadTrackerTextView(Context context) {
    this(context, null);
  }

  public ReadTrackerTextView(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public ReadTrackerTextView(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);

    TypedArray typedArray = context.getTheme().obtainStyledAttributes(
        attrs, R.styleable.ReadTrackerTextView, 0, 0
    );

    try {
      if(typedArray.hasValue(R.styleable.ReadTrackerTextView_fontWeight)) {
        final int fontWeightId = typedArray.getInt(R.styleable.ReadTrackerTextView_fontWeight, -1);
        fontWeight = FontWeight.from(fontWeightId);
      } else {
        fontWeight = FontWeight.REGULAR;
      }
      // Compatibility for pre-14 devices, and allows us to tweak the font if we would like
      fontAllCaps = typedArray.getBoolean(R.styleable.ReadTrackerTextView_fontAllCaps, false);
    } finally {
      typedArray.recycle();
    }
  }

  public FontWeight getFontWeight() {
    return fontWeight;
  }

  public boolean isFontAllCaps() {
    return fontAllCaps;
  }

  public static enum FontWeight {
    THIN(0), REGULAR(1), MEDIUM(2);

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
