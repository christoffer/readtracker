package com.readtracker.custom_views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

public class TimeSpinner extends View {
  private static final int DEFAULT_COLOR = 0xff457698;

  private int mPrimaryColor;
  private int mSecondaryColor;
  private int mFillColor;
  private int mFillColorHighlighted;

  private boolean mIsHighlighted = false;

  private Paint mTickPaint;

  public TimeSpinner(Context context) {
    super(context, null);
  }

  public TimeSpinner(Context context, AttributeSet attrs) {
    super(context, attrs);
    initialize();
  }

  @Override protected void onDraw(Canvas canvas) {
    Paint p = new Paint();
    p.setStyle(Paint.Style.FILL);

    p.setColor(mIsHighlighted ? mFillColorHighlighted : mFillColor);

    float midX = getWidth() / 2.0f;
    float midY = getHeight() / 2.0f;
    canvas.drawCircle(midX, midY, Math.min(midX,  midY) - 35.0f, p);
    drawTicks(canvas, 60, 15, 2, 10, mSecondaryColor);
    drawTicks(canvas, 12, 25, 4, 0, mPrimaryColor);

    // Draw the index tick
    drawTicks(canvas, 1, 45, 4, -10, mPrimaryColor);
  }

  public void setColor(int primaryColor) {
    mPrimaryColor = primaryColor;
    float[] hsv = new float[3];

    Color.colorToHSV(mPrimaryColor, hsv);
    hsv[1] *= 0.7f;

    mSecondaryColor = Color.HSVToColor(hsv);

    mFillColor = Color.HSVToColor(30, hsv);
    mFillColorHighlighted = Color.HSVToColor(50, hsv);
    invalidate();
  }

  private void initialize() {
    setColor(DEFAULT_COLOR);
    mTickPaint = new Paint();
    mTickPaint.setStyle(Paint.Style.FILL);
    mTickPaint.setAntiAlias(true);
  }

  public void setHighlighted(boolean isHighlighted) {
    mIsHighlighted = isHighlighted;
    invalidate();
  }

  private void drawTicks(Canvas canvas, int tickCount, int tickHeight, int tickWidth, int tickOffset, int color) {
    final int width = getWidth();
    final int height = getHeight();
    final int prevColor = mTickPaint.getColor();
    final float halfTickWidth = tickWidth / 2.0f;

    final int radius = Math.min(width,  height) / 2 - 20;

    canvas.save();

    mTickPaint.setColor(color);
    canvas.translate(width / 2, height / 2);

    final float rotation = 360 / tickCount;

    // Start drawing ticks at the top
    canvas.rotate(-180);

    for(int i = 0; i < tickCount; i++) {
      canvas.rotate(rotation);
      canvas.drawRect(0 - halfTickWidth, radius - tickHeight - tickOffset, halfTickWidth, radius - tickOffset, mTickPaint);
    }

    mTickPaint.setColor(prevColor);
    canvas.restore();
  }
}
