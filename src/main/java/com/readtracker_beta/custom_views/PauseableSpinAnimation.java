package com.readtracker_beta.custom_views;

import android.view.animation.RotateAnimation;
import android.view.animation.Transformation;

public class PauseableSpinAnimation extends RotateAnimation {
  private long mElapsedAtPause = 0;
  private boolean mPaused = false;

  public PauseableSpinAnimation(float fromDegrees, float toDegrees, float pivotX, float pivotY) {
    super(fromDegrees, toDegrees, pivotX, pivotY);
  }

  @Override public boolean getTransformation(long currentTime, Transformation outTransformation) {
    if(mPaused && mElapsedAtPause == 0) {
      mElapsedAtPause = currentTime - getStartTime();
    } else if(mPaused) {
      setStartTime(currentTime - mElapsedAtPause);
    }
    return super.getTransformation(currentTime, outTransformation);
  }

  public void pause() {
    mElapsedAtPause = 0;
    mPaused = true;
  }

  public void resume() {
    mPaused = false;
  }
}
