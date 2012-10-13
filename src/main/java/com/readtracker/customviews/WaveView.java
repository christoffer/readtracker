package com.readtracker.customviews;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.*;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.View;
import com.readtracker.R;

public class WaveView extends View {
  private static final int DEFAULT_PRIMARY_COLOR = Color.argb(188, 67, 128, 198);
  private static final int DEFAULT_SECONDARY_COLOR = Color.argb(88, 137, 158, 208);
  private static final int DEFAULT_BACKGROUND_COLOR = Color.rgb(0, 0, 0);

  private static final long PIXELS_PER_SECOND = 20;
  private static final long FPS = 15;
  private static final long UPDATE_INTERVAL = 1000 / FPS;

  // How many views long the waveform should be
  private static final int WAVEFORM_VIEW_LENGTHS = 10;

  private float offset;

  private boolean isRunning = false;
  private Handler handler = new Handler();

  private Bitmap bitmap;

  private long timestamp;

  // Color of the primary wave
  private int primaryColor = DEFAULT_PRIMARY_COLOR;

  // Color of the secondary waves
  private int secondaryColor = DEFAULT_SECONDARY_COLOR;

  // Background color, used for masking the ends of the waves
  private int backgroundColor = DEFAULT_BACKGROUND_COLOR;

  @SuppressWarnings("UnusedDeclaration")
  public WaveView(Context context) {
    super(context);
  }

  @SuppressWarnings("UnusedDeclaration")
  public WaveView(Context context, AttributeSet attrs) {
    super(context, attrs);
    readStyles(attrs);
  }

  @SuppressWarnings("UnusedDeclaration")
  public WaveView(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
    readStyles(attrs);
  }

  @Override protected void onDraw(Canvas canvas) {
    if(bitmap == null) createBitmap();
    canvas.drawBitmap(bitmap, -offset, 0, null);
    if(!isInEditMode()) { // for some reason the gradient does not work in edit mode
      Paint gradientPaint = createGradientPaint();
      canvas.drawRect(0, 0, getWidth(), getHeight(), gradientPaint);
    }
  }

  /**
   * Sets the primary wave color
   *
   * @param color color for drawing the primary wave
   */
  @SuppressWarnings("UnusedDeclaration")
  public void setPrimaryColor(int color) {
    primaryColor = color;
  }

  /**
   * Sets the secondary wave color
   *
   * @param color color for drawing the secondary wave
   */
  @SuppressWarnings("UnusedDeclaration")
  public void setSecondaryColor(int color) {
    secondaryColor = color;
  }

  /**
   * Sets the background color. This is used to draw the fading edges of the
   * wave.
   *
   * @param color color of the background of the wave container
   */
  public void setBackgroundColor(int color) {
    backgroundColor = color;
  }

  private void readStyles(AttributeSet attrs) {
    TypedArray styles = getContext().obtainStyledAttributes(attrs, R.styleable.WaveView);

    final int numStyles = styles.getIndexCount();
    for(int i = 0; i < numStyles; i++) {
      int attr = styles.getIndex(i);
      switch(attr) {
        case R.styleable.WaveView_primaryColor:
          primaryColor = styles.getColor(i, DEFAULT_PRIMARY_COLOR);
          break;
        case R.styleable.WaveView_secondaryColor:
          secondaryColor = styles.getColor(i, DEFAULT_SECONDARY_COLOR);
          break;
        case R.styleable.WaveView_backgroundColor:
          backgroundColor = styles.getColor(i, DEFAULT_BACKGROUND_COLOR);
          break;
      }
    }
    styles.recycle();
  }

  private Paint createGradientPaint() {
    int fill = backgroundColor;
    int transparent = Color.argb(
        0,
        Color.red(backgroundColor),
        Color.green(backgroundColor),
        Color.blue(backgroundColor)
    );
    int[] colors = new int[] { fill, transparent, transparent, fill };
    float[] positions = new float[] { 0.1f, 0.6f, 0.9f, 0.98f };
    LinearGradient gradient = new LinearGradient(0, 0, getWidth(), 0, colors, positions, Shader.TileMode.CLAMP);
    Paint gradientPaint = new Paint();
    gradientPaint.setShader(gradient);
    return gradientPaint;
  }

  private Paint createPrimaryPaint() {
    return new Paint() {{
      setColor(primaryColor);
      setStyle(Paint.Style.STROKE);
      setStrokeWidth(4.0f);
      setAntiAlias(true);
      setPathEffect(new CornerPathEffect(45));
    }};
  }

  private Paint createSecondaryPaint() {
    return new Paint() {{
      setColor(secondaryColor);
      setStyle(Paint.Style.STROKE);
      setStrokeWidth(1.0f);
      setAntiAlias(true);
      setPathEffect(new CornerPathEffect(20));
      setXfermode(new PorterDuffXfermode(PorterDuff.Mode.LIGHTEN));
    }};
  }

  private void createBitmap() {
    float[] waveform = new float[20];

    // make the waveform a lot wider than the view to allow some variations
    int waveformWidth = getWidth() * WAVEFORM_VIEW_LENGTHS;
    int waveformHeight = getHeight();

    Paint primaryPaint = createPrimaryPaint();
    Paint secondaryPaint = createSecondaryPaint();

    // render two bitmaps to allow seamless scrolling, stick a full view width
    // at the end of the first one
    bitmap = Bitmap.createBitmap(waveformWidth + getWidth(), getHeight(), Bitmap.Config.ARGB_4444);

    Canvas bitmapCanvas = new Canvas(bitmap);

    // Draw the primary wave
    populateWaveform(waveform, 0.1f, 0.4f);
    Path path = createWaveformPath(waveform, waveformWidth, waveformHeight);
    drawWaveform(path, bitmapCanvas, 0.0f, primaryPaint);
    drawWaveform(path, bitmapCanvas, waveformWidth, primaryPaint);

    // Draw secondary waves
    for(int i = 0; i < 3; i++) {
      populateWaveform(waveform, 0.2f, 0.3f);
      path = createWaveformPath(waveform, waveformWidth, waveformHeight);
      drawWaveform(path, bitmapCanvas, 0.0f, secondaryPaint);
      drawWaveform(path, bitmapCanvas, waveformWidth, secondaryPaint);
    }
  }

  @SuppressWarnings("UnusedDeclaration")
  public void start() {
    isRunning = true;
    handler.removeCallbacks(animationTimer);
    handler.postDelayed(animationTimer, UPDATE_INTERVAL);
  }

  @SuppressWarnings("UnusedDeclaration")
  public void start(long timestamp) {
    this.timestamp = timestamp;
    isRunning = true;
    handler.removeCallbacks(animationTimer);
    handler.postDelayed(animationTimer, UPDATE_INTERVAL);
  }

  @SuppressWarnings("UnusedDeclaration")
  public void stop() {
    isRunning = false;
    handler.removeCallbacks(animationTimer);
  }

  @SuppressWarnings("UnusedDeclaration")
  public boolean isRunning() {
    return isRunning;
  }

private Runnable animationTimer = new Runnable() {
  @Override public void run() {
    if(bitmap != null) {
      long now = System.currentTimeMillis();
      long elapsed = now - timestamp;
      float delta = PIXELS_PER_SECOND * ((float) elapsed / 1000.0f);
      timestamp = now;
      offset = (offset + delta) % (bitmap.getWidth() - getWidth());
      invalidate();
    }

    handler.postDelayed(animationTimer, UPDATE_INTERVAL);
  }
};

  private void drawWaveform(Path waveformPath, Canvas canvas, final float offset, Paint paint) {
    canvas.save();
    canvas.translate(offset, 0);
    canvas.drawPath(waveformPath, paint);
    canvas.restore();
  }

  private Path createWaveformPath(float[] waveform, int width, int height) {
    Path path = new Path();

    final int centerY = height / 2;
    final int numPoints = waveform.length;

    final float halfHeight = 0.5f * height;

    final float stepSize = width / numPoints;
    final float halfStepSize = stepSize * 0.5f;

    int prevY = 0;
    for(int i = 0; i < numPoints + 1; i++) {
      final int x = (int) stepSize * i;
      final int handleX = (int) (x - halfStepSize);

      final int index = i < numPoints ? i : 0;
      final float waveformValue = waveform[index];
      final int y = (int) (centerY + halfHeight * waveformValue);

      if(i == 0) {
        path.moveTo(0, y);
      } else {
        path.cubicTo(
            handleX, prevY,
            handleX, y,
            x, y);
      }
      prevY = y;
    }

    return path;
  }

  /**
   * Populates a waveform with randomized points.
   * Populates the waveforms points with values [-1.0, 1.0].
   *
   * @param waveform       waveform to populate
   * @param waveOffset     offset [0.0, 1.0] from center for points (lower values =
   *                       flatter curve)
   * @param randomizeScale randomizing scale (lower values = more uniform shape)
   */
  private void populateWaveform(float[] waveform, float waveOffset, float randomizeScale) {
    for(int i = 0; i < waveform.length; i++) {
      final float offsetFromCenter = waveOffset + (float) Math.random() * randomizeScale;
      waveform[i] = i % 2 == 0 ? offsetFromCenter : -offsetFromCenter;
    }
  }

}
