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
  private static final int SEGMENT_SPACING = 2; // px

  public SegmentBar(Context context) {
    this(context, null);
  }

  public SegmentBar(Context context, AttributeSet attrs) {
    super(context, attrs);
    initResources();
    if(isInEditMode()) {
      if(Math.random() > 0.25) {
        // Throw in some sample segments
        int numSegments = 3 + (int) (Math.random() * 10);
        float[] sampleSegments = new float[numSegments];

        for(int i = 0; i < numSegments; i++) {
          sampleSegments[i] = (float) Math.random();
        }
        Arrays.sort(sampleSegments);
        setStops(sampleSegments);
      } else {
        setStops(new float[0]);
      }
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

  private void drawSegment(Canvas canvas, int startX, int endX, boolean isFirst) {
    final RectF rect = new RectF(startX + (isFirst ? 0 : SEGMENT_SPACING), 0, endX, getHeight());
    canvas.drawRect(rect, mPaint);
  }

  private void drawTopLine(Canvas canvas) {
    int color = mPaint.getColor();
    int transparentColor = (0x22 << 24) + (color & 0xffffff);
    mPaint.setColor(transparentColor);
    canvas.drawLine(0, 0, getWidth(), 0, mPaint);
    mPaint.setColor(color);
  }

  @Override protected void onDraw(Canvas canvas) {
    if(mStops == null) {
      return;
    }

    final int width = getWidth();
    float prevSegment = 0.0f;
    drawTopLine(canvas);
    if(mStops.length > 0) {
      for(int i = 0, mStopsLength = mStops.length; i < mStopsLength; i++) {
        float segment = mStops[i];
        final int fromX = (int) (prevSegment * width);
        final int toX = (int) (segment * width);
        drawSegment(canvas, fromX, toX, (i == 0));
        prevSegment = segment;
      }
    }
  }
}
