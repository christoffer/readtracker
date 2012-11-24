package com.readtracker.customviews;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import java.util.Arrays;

public class SegmentBar extends View {
  private Paint mPaint;
  private int mColor = 0xffffffff;
  private float[] mStops;
  private static final int MIN_GAP_SIZE = 4; // px

  public SegmentBar(Context context) {
    this(context, null);
  }

  public SegmentBar(Context context, AttributeSet attrs) {
    super(context, attrs);
    initResources();
    if(isInEditMode()) {
      // Throw in some sample segments
      int numSegments = 3 + (int) (Math.random() * 10);
      float[] sampleSegments = new float[numSegments];

      for(int i = 0; i < numSegments; i++) {
        sampleSegments[i] = (float) Math.random();
      }
      Arrays.sort(sampleSegments);
      setStops(sampleSegments);
    }
  }

  /**
   * Sets the color of the progress bar segments.
   *
   * @param color Color to use for progress bar segments
   */
  public void setColor(int color) {
    mColor = color;
    initResources();
    invalidate();
  }

  /**
   * Gets the color of the progress bar segments
   *
   * @return the color of the progress bar segments
   */
  public int getColor() {
    return mColor;
  }

  /**
   * Sets the stops to use for drawing segments as an array of floats with values ranged 0.0 to 1.0.
   *
   * @param stops stops to draw
   */
  public void setStops(float[] stops) {
    mStops = stops.clone();
  }

  // Private

  private void initResources() {
    mPaint = new Paint();
    mPaint.setColor(mColor);
    mPaint.setStyle(Paint.Style.FILL);
  }

  private void drawSegment(Canvas canvas, int startX, int endX) {
    if((endX - startX) < MIN_GAP_SIZE) {
      endX = startX;
    }
    final RectF rect = new RectF(startX + MIN_GAP_SIZE, 0, endX, getHeight());
    canvas.drawRect(rect, mPaint);
  }

  @Override protected void onDraw(Canvas canvas) {
    if(mStops == null || mStops.length == 0) {
      return;
    }

    final int width = getWidth();
    float prevSegment = 0.0f;
    for(final float segment : mStops) {
      final int fromX = (int) (prevSegment * width);
      final int toX = (int) (segment * width);
      drawSegment(canvas, fromX, toX);
      prevSegment = segment;
    }
  }
}
