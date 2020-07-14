package com.readtracker.android.custom_views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import com.readtracker.android.db.Book;
import com.readtracker.android.db.Session;
import com.readtracker.android.support.ColorUtils;

import java.lang.ref.WeakReference;

public class SessionItemBackground extends View {
  private static final int DEFAULT_COLOR = 0x99000000;

  private Paint mPaint;
  private int mParentHeight;
  private int mParentWidth;
  private float mIntervalStart = -1f;
  private float mIntervalEnd = -1f;

  public SessionItemBackground(Context context) {
    super(context);
    initializePaint();
  }

  public SessionItemBackground(Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
    initializePaint();
  }

  public SessionItemBackground(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    initializePaint();
  }

  @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
  public SessionItemBackground(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
    super(context, attrs, defStyleAttr, defStyleRes);
    initializePaint();
  }

  @Override protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);
    if(mIntervalStart >= 0 && mIntervalEnd > 0) {
      float startOffset = mParentWidth * mIntervalStart;
      float intervalWidth = mParentWidth * (mIntervalEnd - mIntervalStart);
      canvas.drawRect(startOffset, 0, startOffset + intervalWidth, mParentHeight, mPaint);
    }
  }

  @Override protected void onSizeChanged(int w, int h, int oldw, int oldh) {
    super.onSizeChanged(w, h, oldw, oldh);
    final View parent = ((View) getParent());
    final boolean hasParentDimensions = mParentHeight != 0 || mParentWidth != 0;
    if(parent instanceof ViewGroup && !hasParentDimensions) {
      mParentHeight = parent.getHeight() - getPaddingTop() - getPaddingBottom();
      mParentWidth = parent.getWidth() - getPaddingLeft() - getPaddingRight();
      // Can't request re-layout here since onSizeChanged is part of the layout
      post(new RequestLayoutRunnable(this));
    }
  }

  @Override protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    setMeasuredDimension(
        getMeasureSpec(mParentWidth, widthMeasureSpec),
        getMeasureSpec(mParentHeight, heightMeasureSpec)
    );
  }

  public void initForSession(Session session) {
    int color = DEFAULT_COLOR;

    final Book book = session.getBook();
    if(book != null) {
      color = ColorUtils.getColorForBook(book);
    }

    // Mute the color for the background
    int r = Color.red(color);
    int g = Color.green(color);
    int b = Color.blue(color);
    color = Color.argb(64, r, g, b);

    mIntervalStart = Math.min(1.0f, session.getStartPosition());
    mIntervalEnd = Math.min(1.0f, session.getEndPosition());
    mPaint.setColor(color);
    invalidate();
  }

  private int getMeasureSpec(int desiredSize, int measureSpec) {
    final int specMode = MeasureSpec.getMode(measureSpec);
    final int specSize = MeasureSpec.getSize(measureSpec);
    if(specMode == MeasureSpec.EXACTLY) {
      return specSize;
    }
    if(specMode == MeasureSpec.AT_MOST) {
      return Math.min(desiredSize, specSize);
    }
    return desiredSize;
  }

  private void initializePaint() {
    setLayerType(View.LAYER_TYPE_SOFTWARE, null);

    mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    mPaint.setStyle(Paint.Style.FILL);
    mPaint.setColor(DEFAULT_COLOR);
  }

  private static class RequestLayoutRunnable implements Runnable {
    private final WeakReference<SessionItemBackground> mRef;

    public RequestLayoutRunnable(SessionItemBackground view) {
      mRef = new WeakReference<>(view);
    }

    @Override public void run() {
      SessionItemBackground view = mRef.get();
      if(view != null) {
        view.requestLayout();
        view.invalidate();
      }
    }
  }
}
