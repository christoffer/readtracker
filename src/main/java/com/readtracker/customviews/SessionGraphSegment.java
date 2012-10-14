package com.readtracker.customviews;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.drawable.BitmapDrawable;
import android.util.AttributeSet;
import android.view.View;

public class SessionGraphSegment extends View {
  private BitmapDrawable bitmapDrawable;

  private float mInPosition = 0.0f, mOutPosition = 0.0f;
  private float mHeightScale = 1.0f;

  @SuppressWarnings("UnusedDeclaration")
  public SessionGraphSegment(Context context) {
    super(context);
  }

  @SuppressWarnings("UnusedDeclaration")
  public SessionGraphSegment(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  @SuppressWarnings("UnusedDeclaration")
  public SessionGraphSegment(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
  }

  @Override protected void onSizeChanged(int w, int h, int oldw, int oldh) {
    super.onSizeChanged(w, h, oldw, oldh);
    refreshBackground();
  }

  @Override protected int getSuggestedMinimumHeight() {
    return (int) (200 * mHeightScale);
  }

  public void setPoints(float inPos, float outPos) {
    mInPosition = inPos;
    mOutPosition = outPos;
    refreshBackground();
  }

  public void setHeightScale(float heightScale) {
    mHeightScale = heightScale;
  }

  private void refreshBackground() {
    if(getWidth() == 0 || getHeight() == 0) {
      return;
    }
    setBackgroundDrawable(generateBackgroundDrawable(getWidth(), getHeight()));
    invalidate();
  }

  private BitmapDrawable generateBackgroundDrawable(final int width, final int height) {
    Paint paint = new Paint() {{
      setColor(0x22 << 24 | 0xffffff);
      setStyle(Style.FILL);
      setStrokeWidth(4.0f);
      setAntiAlias(true);
    }};

    Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_4444);

    Canvas bitmapCanvas = new Canvas(bitmap);

    // Draw the primary wave
    Path path = new Path();


    float startX = mInPosition * width;
    float endX = mOutPosition * width;

    if(isInEditMode()) {
      startX = 0.33f * width;
      endX = 0.65f * width;
    }

    path.moveTo(startX, 0);
    path.lineTo(endX, height);
    path.lineTo(0, height);
    path.lineTo(0, 0);
    path.close();

    bitmapCanvas.drawPath(path, paint);

    return new BitmapDrawable(bitmap);
  }
}
