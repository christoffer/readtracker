package com.readtracker.android.custom_views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import java.util.Arrays;

public class SegmentBar extends View {
  private Paint mSegmentPaint;
  private Paint mBoundaryPaint;
  private Paint mBackgroundPaint;

  private int mColor = 0xffffffff;
  private float[] mStops;
  private static final int DIVIDER_WIDTH = 2; // px

  @SuppressWarnings("UnusedDeclaration")
  public SegmentBar(Context context) {
    this(context, null);
  }

  @SuppressWarnings("UnusedDeclaration")
  public SegmentBar(Context context, AttributeSet attrs) {
    super(context, attrs);
    initResources();
    if(isInEditMode()) {
      if(Math.random() > 0.25) {
        // Throw in some sample segments
        int numSegments = 43 + (int) (Math.random() * 10);
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
   * Sets the stops to use for drawing segments as an array of floats with values ranged 0.0 to 1.0.
   *
   * @param stops stops to draw
   */
  public void setStops(float[] stops) {
    if(stops == null) {
      mStops = null;
    } else {
      mStops = stops.clone();
    }
    invalidate();
  }

  // Private

  private void initResources() {
    mSegmentPaint = new Paint();
    mSegmentPaint.setColor(mColor);
    mSegmentPaint.setStyle(Paint.Style.FILL);

    mBoundaryPaint = new Paint();
    int transparentColor = (0x22 << 24) + (mColor & 0xffffff);
    mBoundaryPaint.setColor(transparentColor);
    mBoundaryPaint.setStyle(Paint.Style.STROKE);
    mBoundaryPaint.setStrokeWidth(1);

    mBackgroundPaint = new Paint();
    mBackgroundPaint.setColor(0xff000000);
    mBackgroundPaint.setStyle(Paint.Style.FILL);
  }

  private void drawSegment(Canvas canvas, float previousEnd, float segmentEnd) {
    final RectF segmentRect = new RectF(0, 0, segmentEnd, getHeight());
    canvas.drawRect(segmentRect, mSegmentPaint);
    final float segmentWidth = previousEnd - segmentEnd; // Since we are going backwards through the segments
    if(segmentWidth >= (DIVIDER_WIDTH + 1)) {
      canvas.drawRect(segmentRect.right - DIVIDER_WIDTH, segmentRect.top, segmentRect.right, segmentRect.bottom, mBackgroundPaint);
    }
  }

  private void drawOutline(Canvas canvas) {
    int color = mSegmentPaint.getColor();

    final int width = getWidth() -1;
    final int height = getHeight() - 1;
    canvas.drawRect(0, 0, width, height, mBoundaryPaint);
    mSegmentPaint.setColor(color);
  }

  @Override protected void onDraw(Canvas canvas) {
    if(mStops == null) {
      return;
    }

    final int width = getWidth();
    drawOutline(canvas);
    if(mStops.length > 0) {
      float[] drawStops = mStops.clone();
      Arrays.sort(drawStops);
      float previousEnd = 0.0f;
      for(int i = drawStops.length - 1; i >= 0; i--) {
        final float progress = drawStops[i];
        final float segmentEnd = progress * width;
        drawSegment(canvas, previousEnd, segmentEnd);
        previousEnd = segmentEnd;
      }
    }
  }
}
