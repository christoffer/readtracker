package com.readtracker.customviews;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.widget.EditText;

public class UnderlinedEditText extends EditText {
  private Paint linePaint;

  private static final int HEIGHT = 3;
  private static final int COLOR_FOCUS = 0xffffffff;
  private static final int COLOR_INACTIVE = 0x44ffffff;

  public UnderlinedEditText(Context context) {
    super(context);
    initResources();
  }

  public UnderlinedEditText(Context context, AttributeSet attrs) {
    super(context, attrs);
    initResources();
  }

  public UnderlinedEditText(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
    initResources();
  }

  private void initResources() {
    linePaint = new Paint();
    linePaint.setStyle(Paint.Style.FILL);
  }

  @Override protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);
    if(isFocused() && isEnabled()) {
      linePaint.setColor(COLOR_FOCUS);
    } else {
      linePaint.setColor(COLOR_INACTIVE);
    }

    final int bottom = getHeight() - 1;
    final int right = getWidth() - 1;
    canvas.drawLine(0, bottom - HEIGHT, 0, bottom, linePaint);
    canvas.drawLine(right, bottom - HEIGHT, right, bottom, linePaint);
    canvas.drawLine(0, bottom, right + 1, bottom, linePaint);
  }
}
