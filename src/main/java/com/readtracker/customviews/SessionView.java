package com.readtracker.customviews;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.*;
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
  private Paint mBackgroundPaint;

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
    initializePaints();
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
    if(mNodes == null) {
      if(isInEditMode()) {
        generatePreviewNodes();
      } else {
        return;
      }
    }

    final float innerWidth = getWidth() - (getPaddingLeft() + getPaddingRight());

    // Draw lines in a separate pass since they are in the background of everything else
    drawLines(canvas);

    for(int i = 0, mNodesLength = mNodes.length; i < mNodesLength; i++) {
      Node node = mNodes[i];

      final float radius = calcRadius(node);
      final float x = Math.min(1.0f, node.progress) * innerWidth + getPaddingLeft();
      final float y = (i + 1) * SEGMENT_HEIGHT + getPaddingTop();

      String primaryText = Utils.hoursAndMinutesFromMillis(node.durationSeconds * 1000);
      String secondaryText = node.timeAgo;

      boolean drawFlipped = node.progress > 0.5;
      drawText(canvas, x, y, radius, primaryText, secondaryText, drawFlipped);
      drawNode(canvas, x, y, radius);
    }
  }

  public void setColor(int color) {
    mColor = color;
    initializePaints();
    invalidate();
  }

  private void drawText(Canvas canvas, float x, float y, float radius, String primaryText, String secondaryText, boolean drawFlipped) {
    final float textPadding = TEXT_PADDING; // Padding between the text and the box
    final float textPaddingNode = 2 * radius;

    final float primaryTextHeight = mPrimaryTextPaint.measureText("X");
    final float secondaryTextHeight = mSecondaryTextPaint.measureText("X");

    final float lineHeight = 1.5f;

    // Calculate dimensions of the text box
    final float largestTextWidth = Math.max(mPrimaryTextPaint.measureText(primaryText), mSecondaryTextPaint.measureText(secondaryText));
    final float textBoxHeight = primaryTextHeight + secondaryTextHeight + 2 * textPadding + lineHeight * primaryTextHeight; // use line height of 1.5
    RectF textBox = new RectF(0, 0, textPadding * 2 + largestTextWidth, textBoxHeight);

    textBox.offset(x, y);

    // Let the x, y position be the rightmost edge if drawFlipped is set
    if(drawFlipped) {
      textBox.offset(-textBox.width() - (2 * textPaddingNode), 0);
    }

    textBox.offset(0, -textBox.height() * 0.5f);

    final float primaryTextTop = textBox.top + textPadding + primaryTextHeight;
    canvas.drawText(primaryText, textBox.left + textPaddingNode + textPadding, primaryTextTop, mPrimaryTextPaint);
    canvas.drawText(secondaryText, textBox.left + textPaddingNode + textPadding, primaryTextTop + lineHeight * primaryTextHeight + secondaryTextHeight, mSecondaryTextPaint);
  }

  private void drawLines(Canvas canvas) {
    float lastX = getPaddingLeft();
    float lastY = getPaddingTop();
    final float innerWidth = getWidth() - (getPaddingLeft() + getPaddingRight());

    int nodeColor = mNodePaint.getColor();
    int lineColor = (0x00ffffff & nodeColor) + (0x44 << 24);
    mNodePaint.setColor(lineColor);
    for(int i = 0, mNodesLength = mNodes.length; i < mNodesLength; i++) {
      Node node = mNodes[i];

      final float x = Math.min(1.0f, node.progress) * innerWidth + getPaddingLeft();
      final float y = (i + 1) * SEGMENT_HEIGHT + getPaddingTop();


      canvas.drawLine(lastX, lastY, x, y, mNodePaint);

      lastX = x;
      lastY = y;
    }
    mNodePaint.setColor(nodeColor);
  }

  private void drawNode(Canvas canvas, float x, float y, float radius) {
    canvas.drawCircle(x, y, radius, mBackgroundPaint);
    canvas.drawCircle(x, y, radius, mNodePaint);
  }

  private void initializePaints() {
    mNodePaint = new Paint();
    mNodePaint.setStyle(Paint.Style.STROKE);
    mNodePaint.setStrokeWidth(3);
    mNodePaint.setColor(mColor);
    mNodePaint.setAntiAlias(true);

    mPrimaryTextPaint = new Paint();
    mPrimaryTextPaint.setTypeface(Typeface.DEFAULT);
    mPrimaryTextPaint.setTextSize(convertDPtoPX(14));
    mPrimaryTextPaint.setColor(getResources().getColor(R.color.text_color_primary));
    mPrimaryTextPaint.setSubpixelText(true);

    mSecondaryTextPaint = new Paint();
    mSecondaryTextPaint.setTypeface(Typeface.DEFAULT);
    mSecondaryTextPaint.setTextSize(convertDPtoPX(12));
    mSecondaryTextPaint.setColor(getResources().getColor(R.color.text_color_secondary));
    mSecondaryTextPaint.setSubpixelText(true);

    TypedArray array = getContext().getTheme().obtainStyledAttributes(new int[]{
      android.R.attr.colorBackground,
    });

    int backgroundColor = 0xff000000 | array.getColor(0, Color.BLACK);
    array.recycle();

    mBackgroundPaint = new Paint();
    mBackgroundPaint.setStyle(Paint.Style.FILL);
    mBackgroundPaint.setColor(backgroundColor);
  }

  private float convertDPtoPX(int dp) {
    Resources r = getResources();
    return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, r.getDisplayMetrics());
  }

  private void generatePreviewNodes() {
    mNodes = new Node[]{
      new Node(45 * 60, 0.2f, "2 weeks ago"),
      new Node(95 * 60, 0.3f, "Yesterday"),
      new Node(1245 * 60, 0.45f, "2 days ago"),
      new Node(95 * 60, 1.0f, "3 months ago")
    };
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
