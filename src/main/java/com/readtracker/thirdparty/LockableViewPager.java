package com.readtracker.thirdparty;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;

public class LockableViewPager extends ViewPager {

  private boolean mIsLocked;

  public LockableViewPager(Context context, AttributeSet attrs) {
    super(context, attrs);
    this.mIsLocked = false;
  }

  @Override
  public boolean onTouchEvent(MotionEvent event) {
    return this.mIsLocked ? false : super.onTouchEvent(event);
  }

  @Override
  public boolean onInterceptTouchEvent(MotionEvent event) {
    return this.mIsLocked ? false : super.onInterceptTouchEvent(event);
  }

  public void setLocked(boolean locked) {
    this.mIsLocked = locked;
  }
}
