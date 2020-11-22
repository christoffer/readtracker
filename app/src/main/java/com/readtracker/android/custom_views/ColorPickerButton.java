package com.readtracker.android.custom_views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.RectF;
import android.util.AttributeSet;

import com.readtracker.R;
import com.readtracker.android.support.Utils;

public class ColorPickerButton extends androidx.appcompat.widget.AppCompatImageButton {
  private static int[] COLOR_WHEEL_COLORS;
  private static final int OUTLINE_WIDTH_DP = 4;
  private static final Paint mOutlinePaint = new Paint();
  private static final Paint mColorDotBackgroundPaint = new Paint();
  private int mOutlineStrokeWidthPx;

  private Canvas mColorCircleCanvas;

  private Integer mColor;
  private boolean mIsCurrentColor;
  private RectF mColorDotBounds;
  private Bitmap mColorDotBitmap;
  private int mImageSize;

  public ColorPickerButton(Context context) {
    super(context);
    initialize();
  }

  public ColorPickerButton(Context context, AttributeSet attrs) {
    super(context, attrs);
    initialize();
  }

  public ColorPickerButton(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    initialize();
  }

  public void setColor(Integer color) {
    mColor = color;
    refreshDrawable();
  }

  public Integer getColor() {
    return mColor;
  }

  public void setIsCurrentColor(boolean isCurrentColor) {
    mIsCurrentColor = isCurrentColor;
    refreshDrawable();
  }


  private void initialize() {
    mOutlineStrokeWidthPx = Utils.convertDPtoPixels(getContext(), OUTLINE_WIDTH_DP);

    // Can't use getWidth()/getHeight() here since the view hasn't been layout'd yet. Seems
    // reasonable to use a fixed image size anyway, like if it were a static drawable.
    mImageSize = getResources().getDimensionPixelSize(R.dimen.colorPickerButton_ImageSize);

    // Initialize paint for the outline of active color dots
    mOutlinePaint.setAntiAlias(true);
    mOutlinePaint.setStrokeWidth(4);
    mOutlinePaint.setStyle(Paint.Style.STROKE);

    // Initialize paint for rendering the filled backgrounds
    mColorDotBackgroundPaint.setAntiAlias(true);
    mColorDotBackgroundPaint.setStyle(Paint.Style.FILL);

    TypedArray colorArrayRes = this.getResources().obtainTypedArray(R.array.color_wheel_colors);
    COLOR_WHEEL_COLORS = new int[colorArrayRes.length()];
    for (int i = 0; i < colorArrayRes.length(); i++) {
      COLOR_WHEEL_COLORS[i] = colorArrayRes.getColor(i, 0);
    }
    colorArrayRes.recycle();

    // Initialize the bounds for where to draw the color dot. The bounds needs to be offset
    // a bit from the edge of the view, since drawing with stroke draws from the center and not
    // inside. Not doing this will lead to half the stroke being drawn outside the view.
    mColorDotBounds = new RectF(
        mOutlineStrokeWidthPx,
        mOutlineStrokeWidthPx,
        mImageSize - mOutlineStrokeWidthPx,
        mImageSize - mOutlineStrokeWidthPx
    );
    mColorDotBitmap = Bitmap.createBitmap(mImageSize, mImageSize, Bitmap.Config.ARGB_8888);
    mColorCircleCanvas = new Canvas(mColorDotBitmap);

    refreshDrawable();
    setBackgroundResource(R.drawable.bg_button_transparent);
  }

  private void refreshDrawable() {
    // Clear canvas
    mColorCircleCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
    renderButtonImage(mColorCircleCanvas);
    setImageBitmap(mColorDotBitmap);
  }

  private void renderButtonImage(Canvas canvas) {
    float midX = mImageSize / 2.0f;
    float midY = mImageSize / 2.0f;
    float radius = Math.min(midX, midY) - mOutlineStrokeWidthPx;

    if(mIsCurrentColor) {
      mOutlinePaint.setColor(mColor);
      canvas.drawCircle(midX, midY, radius + mOutlineStrokeWidthPx * 0.49f, mOutlinePaint);
    }

    if(mColor == null) {
      float angleStep = 360.f / COLOR_WHEEL_COLORS.length;
      for(int i = 0; i < COLOR_WHEEL_COLORS.length; i++) {
        mColorDotBackgroundPaint.setColor(COLOR_WHEEL_COLORS[i]);
        canvas.drawArc(mColorDotBounds, i * angleStep, angleStep, true, mColorDotBackgroundPaint);
      }
    } else {
      mColorDotBackgroundPaint .setColor(mColor);
      canvas.drawCircle(midX, midY, radius, mColorDotBackgroundPaint);
    }
  }
}
