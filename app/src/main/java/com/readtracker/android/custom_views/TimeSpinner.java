package com.readtracker.android.custom_views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.View;

import com.readtracker.R;

public class TimeSpinner extends View {
  private static final int DEFAULT_COLOR = 0xff457698;

  private int mPrimaryColor;
  private int mSecondaryColor;
  private int mFillColor;
  private int mFillColorHighlighted;

  private boolean mIsHighlighted = false;

  private Paint mTickPaint;
  private Paint mHighlightPaint;

  private float mPixelsPerDP = 1.0f;

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

    drawTicks(canvas, mSecondaryColor);
  }

  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    int measuredWidth = MeasureSpec.getSize(widthMeasureSpec);
    final int maxWidth = (int) getResources().getDimension(R.dimen.readingViewsMaxWidth);

    if(maxWidth > 0 && maxWidth < measuredWidth) {
      int measureMode = MeasureSpec.getMode(widthMeasureSpec);
      widthMeasureSpec = MeasureSpec.makeMeasureSpec(maxWidth, measureMode);
    }

    // NOTE(christoffer) Only respect the width to keep the spinner centered when the
    // keyboard is visible. Otherwise it'll adjust the size to the height and become smaller
    // and the centering logic in the layout seems to mess that up.

    //noinspection SuspiciousNameCombination
    super.onMeasure(widthMeasureSpec, widthMeasureSpec);
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

    mHighlightPaint = new Paint();
    mHighlightPaint.setStyle(Paint.Style.FILL);

    mPixelsPerDP = getResources().getDisplayMetrics().densityDpi / DisplayMetrics.DENSITY_DEFAULT;

    setDrawingCacheEnabled(true);
  }

  public void setHighlighted(boolean isHighlighted) {
    mIsHighlighted = isHighlighted;
    invalidate();
  }

  private void drawTicks(Canvas canvas, int color) {
    final int width = getWidth();
    final int height = getHeight();
    final int prevColor = mTickPaint.getColor();

    final float radius = Math.min(width, height) / 2.0f - 20.0f;

    final float tickCount = 45;
    final float tickHeightPx = mPixelsPerDP * 5;
    final float tickWidthPx = mPixelsPerDP * 7;
    final float halfTickWidthPx = tickWidthPx / 2.0f;
    final float tickOffset = mPixelsPerDP * 10;

    canvas.save();

    mTickPaint.setColor(color);
    canvas.translate(width / 2, height / 2);

    final float rotation = 360 / tickCount;

    for(int i = 0; i < tickCount; i++) {
      canvas.rotate(rotation);
      canvas.drawRect(
          0.0f - halfTickWidthPx,
          radius - tickHeightPx - tickOffset,
          tickWidthPx,
          radius - tickOffset,
          mTickPaint
      );
    }

    mTickPaint.setColor(prevColor);
    canvas.restore();
  }
}
