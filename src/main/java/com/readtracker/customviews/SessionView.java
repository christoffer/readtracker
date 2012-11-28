package com.readtracker.customviews;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import com.readtracker.R;
import com.readtracker.Utils;

public class SessionView extends View {
  private int mColor = 0xff449966;
  private Paint mNodePaint;
  private Paint mPrimaryTextPaint;
  private Paint mSecondaryTextPaint;

  private int mBackgroundColor;

  private Node[] mNodes;
  private static final int SEGMENT_HEIGHT = 96; // Height between each node
  private static final int TEXT_PADDING = 5; // Distance from text to node

  public SessionView(Context context) {
    this(context, null, 0);
  }

  public SessionView(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public SessionView(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
    initialize();
  }

  @Override
  protected void onMeasure(int widthSpec, int heightSpec) {
    super.onMeasure(widthSpec, heightSpec);
    final int width = getMeasuredWidth(); // Let the super calculate the width

    int specHeightSize = MeasureSpec.getSize(heightSpec);
    int specHeightMode = MeasureSpec.getMode(heightSpec);

    int height = mNodes == null ? 0 : SEGMENT_HEIGHT * mNodes.length;

    final boolean isOverflow = specHeightMode == MeasureSpec.AT_MOST &&
        height > specHeightSize;

    if(specHeightMode == MeasureSpec.EXACTLY || isOverflow) {
      height = specHeightSize;
    }

    setMeasuredDimension(width, height);
  }

  @Override protected void onDraw(Canvas canvas) {
    if(mNodes == null) return;

    float lastX = getPaddingLeft();
    float lastY = getPaddingTop();
    float lastRadius = 0;
    float textHeight = mPrimaryTextPaint.measureText("X");

    for(int i = 0, mNodesLength = mNodes.length; i < mNodesLength; i++) {
      Node node = mNodes[i];

      final float radius = calcRadius(node);
      final float textPadding = radius + TEXT_PADDING;

      final float x = Math.min(1.0f, node.progress) * getWidth() + getPaddingLeft();
      final float y = (i + 1) * SEGMENT_HEIGHT + getPaddingTop();

      canvas.drawLine(lastX, lastY, x, y, mNodePaint);

      // Draw trailing nodes to allow it to cover the trailing line
      if(i > 0) {
        // Draw twice for the last node
        if(i == mNodesLength - 1) {
          drawNode(canvas, x, y, radius);
        }
        drawNode(canvas, lastX, lastY, lastRadius);
      }

      String primaryText = Utils.hoursAndMinutesFromMillis(node.durationSeconds * 1000);
      canvas.drawText(primaryText, x + textPadding, y, mPrimaryTextPaint);
      canvas.drawText(node.timeAgo, x + textPadding, y + textHeight * 2.0f, mSecondaryTextPaint);

      lastX = x;
      lastY = y;
      lastRadius = radius;
    }
  }

  private void drawNode(Canvas canvas, float x, float y, float radius) {
    mNodePaint.setColor(mColor);
    canvas.drawCircle(x, y, radius, mNodePaint);
    mNodePaint.setColor(0xff000000);
    canvas.drawCircle(x, y, radius- mNodePaint.getStrokeWidth(), mNodePaint);
    mNodePaint.setColor(mColor);
  }

  private void initialize() {
    mNodePaint = new Paint();
    mNodePaint.setStyle(Paint.Style.FILL_AND_STROKE);
    mNodePaint.setStrokeWidth(3);
    mNodePaint.setColor(mColor);

    mPrimaryTextPaint = new Paint();
    mPrimaryTextPaint.setTypeface(Typeface.DEFAULT);
    mPrimaryTextPaint.setTextSize(convertDPtoPX(14));
    mPrimaryTextPaint.setColor(getResources().getColor(R.color.text_color_primary));

    mSecondaryTextPaint = new Paint();
    mSecondaryTextPaint.setTypeface(Typeface.DEFAULT);
    mSecondaryTextPaint.setTextSize(convertDPtoPX(12));
    mSecondaryTextPaint.setColor(getResources().getColor(R.color.text_color_secondary));

    TypedArray array = getContext().getTheme().obtainStyledAttributes(new int[] {
        android.R.attr.colorBackground,
    });
    mBackgroundColor = 0x00ffffff & array.getColor(0, Color.BLACK);
    array.recycle();

    //    if(isInEditMode()) {
    generatePreviewNodes();
    //    }
  }

  private float convertDPtoPX(int dp) {
    Resources r = getResources();
    return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, r.getDisplayMetrics());
  }

  private void generatePreviewNodes() {
    Node n1 = new Node(45 * 60, 0.2f, "2 weeks ago");
    Node n2 = new Node(95 * 60, 0.5f, "1 year ago");
    Node n3 = new Node(1245 * 60, 0.35f, "2 days ago");
    mNodes = new Node[] { n1, n2, n3 };
  }

  private float calcRadius(Node node) {
    final int min = 10;
    final int max = 50;
    final int three_hours = 60 * 60 * 5;
    return Math.max(min, Math.min(max, node.durationSeconds / three_hours));
  }
}

class Node {
  public Node(long durationSeconds, float progress, String timeAgo) {
    this.durationSeconds = durationSeconds;
    this.progress = progress;
    this.timeAgo = timeAgo;
  }

  public long durationSeconds;
  public float progress; // 0..1
  public String timeAgo;
}
