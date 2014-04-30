package com.readtracker.android.custom_views;

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

  private int mMaxSize = 0;

  private Paint mTickPaint;
  private Paint mHighlightPaint;

  @SuppressWarnings("UnusedDeclaration")
  public TimeSpinner(Context context) {
    super(context, null);
    initialize();
  }

  @SuppressWarnings("UnusedDeclaration")
  public TimeSpinner(Context context, AttributeSet attrs) {
    super(context, attrs);
    initialize();
  }

  @Override protected void onDraw(Canvas canvas) {
    mHighlightPaint.setColor(mIsHighlighted ? mFillColorHighlighted : mFillColor);

    float midX = getWidth() / 2.0f;
    float midY = getHeight() / 2.0f;
    canvas.drawCircle(midX, midY, Math.min(midX, midY) - 35.0f, mHighlightPaint);
    drawTicks(canvas, 60, 15, 2, 10, mSecondaryColor);
    drawTicks(canvas, 12, 25, 4, 0, mPrimaryColor);
  }

  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    int measuredWidth = MeasureSpec.getSize(widthMeasureSpec);
    if(mMaxSize > 0 && mMaxSize < measuredWidth) {
      int measureMode = MeasureSpec.getMode(widthMeasureSpec);
      widthMeasureSpec = MeasureSpec.makeMeasureSpec(mMaxSize, measureMode);
    }
    int measuredHeight = MeasureSpec.getSize(heightMeasureSpec);
    if(mMaxSize > 0 && mMaxSize < measuredHeight) {
      int measureMode = MeasureSpec.getMode(heightMeasureSpec);
      heightMeasureSpec = MeasureSpec.makeMeasureSpec(mMaxSize, measureMode);
    }
    super.onMeasure(widthMeasureSpec, heightMeasureSpec);
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

  /**
   * Adjust the maximum size the timeSpinner can be
   *
   * @param maxSize the maximum size in pixels
   */
  public void setMaxSize(int maxSize) {
    mMaxSize = maxSize;
  }

  private void initialize() {
    setColor(DEFAULT_COLOR);
    mTickPaint = new Paint();
    mTickPaint.setStyle(Paint.Style.FILL);
    mTickPaint.setAntiAlias(true);

    mHighlightPaint = new Paint();
    mHighlightPaint.setStyle(Paint.Style.FILL);

    setDrawingCacheEnabled(true);
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

    final int radius = Math.min(width, height) / 2 - 20;

    canvas.save();

    mTickPaint.setColor(color);
    canvas.translate(width / 2, height / 2);

    final float rotation = 360 / tickCount;

    for(int i = 0; i < tickCount; i++) {
      canvas.rotate(rotation);
      canvas.drawRect(0 - halfTickWidth, radius - tickHeight - tickOffset, halfTickWidth, radius - tickOffset, mTickPaint);
    }

    mTickPaint.setColor(prevColor);
    canvas.restore();
  }
}
