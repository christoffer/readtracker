package com.readtracker.android.support;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Shader;

public class CircleImageTransformation implements com.squareup.picasso.Transformation {
  private final Paint mPaint;

  public CircleImageTransformation() {
    mPaint = new Paint();
    mPaint.setAntiAlias(true);
  }

  @Override
  public Bitmap transform(final Bitmap source) {
    final BitmapShader shader = new BitmapShader(source, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
    mPaint.setShader(shader);

    final float sourceWidth = source.getWidth();
    final float sourceHeight = source.getHeight();
    final float dimension = Math.min(sourceWidth, sourceHeight);
    final float halfDimension = dimension / 2.0f;

    Bitmap output = Bitmap.createBitmap((int) dimension, (int) dimension, Config.ARGB_8888);
    Canvas canvas = new Canvas(output);
    canvas.drawCircle(halfDimension, halfDimension, halfDimension, mPaint);
    source.recycle();

    return output;
  }

  @Override
  public String key() {
    return "circle-mask";
  }
}
