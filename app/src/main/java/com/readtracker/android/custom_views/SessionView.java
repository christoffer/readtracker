package com.readtracker.android.custom_views;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import com.readtracker.R;
import com.readtracker.android.db.Session;
import com.readtracker.android.support.Utils;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Draws a history of reading sessions on a time line.
 */
public class SessionView extends View {
  private int mColor = 0xff449966;
  private Paint mNodePaint;
  private Paint mPrimaryTextPaint;
  private Paint mSecondaryTextPaint;
  private Paint mBackgroundPaint;

  private Node[] mNodes;
  private static final int SEGMENT_HEIGHT = 96; // Height between each node
  private static final int TEXT_PADDING = 5; // Distance from text to node

  @SuppressWarnings("UnusedDeclaration")
  public SessionView(Context context) {
    this(context, null, 0);
  }

  @SuppressWarnings("UnusedDeclaration")
  public SessionView(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public SessionView(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
    initializePaints();
    setDrawingCacheEnabled(true);
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
      return;
    }

    final float innerWidth = getWidth() - (getPaddingLeft() + getPaddingRight());

    // Draw lines in a separate pass since they are in the background of everything else
    drawLines(canvas);

    final long now = System.currentTimeMillis();

    for(int i = 0, mNodesLength = mNodes.length; i < mNodesLength; i++) {
      Node node = mNodes[i];

      final float radius = calcRadius(node);
      final float x = Math.min(1.0f, node.progress) * innerWidth + getPaddingLeft();
      final float y = i == 0 ? SEGMENT_HEIGHT * 0.5f : i * SEGMENT_HEIGHT + getPaddingTop();

      String primaryText = Utils.hoursAndMinutesFromMillis(node.durationSeconds * 1000);
      String secondaryText = Utils.humanPastTimeFromTimestamp(node.sessionTimestampMs, now);

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

  /**
   * Take a list of localSessions and convert them to session nodes.
   *
   * @param sessions list of sessions to convert
   */
  public void setSessions(List<Session> sessions) {
    mNodes = new Node[sessions.size()];
    int index = 0;

    for(Session session : sessions) {
      mNodes[index++] = new Node(session);
    }

    Arrays.sort(mNodes, new LocalSessionComparator());
    invalidate();
  }

  private void drawText(Canvas canvas, float x, float y, float radius, String primaryText, String secondaryText, boolean drawFlipped) {
    final float textPadding = TEXT_PADDING; // Padding between the text and the box
    final float primaryTextHeight = mPrimaryTextPaint.measureText("X");
    final float secondaryTextHeight = mSecondaryTextPaint.measureText("X");
    final float lineHeight = 0.8f;

    // Calculate dimensions of the text box
    final float largestTextWidth = Math.max(mPrimaryTextPaint.measureText(primaryText), mSecondaryTextPaint.measureText(secondaryText));
    final float textBoxHeight = primaryTextHeight + secondaryTextHeight + 2 * textPadding + lineHeight * primaryTextHeight;
    RectF textBox = new RectF(0, 0, textPadding * 2 + largestTextWidth, textBoxHeight);

    // Let the x, y position be the rightmost edge if drawFlipped is set
    if(drawFlipped) {
      textBox.offset(x - radius - textPadding - textBox.width(), y);
    } else {
      textBox.offset(x + radius + textPadding, y);
    }

    // Center box vertically
    textBox.offset(0, -textBox.height() * 0.5f);

    final float primaryTextTop = textBox.top + textPadding + primaryTextHeight;

    float textOffset;

    if(drawFlipped) {
      textOffset = textBox.right - textPadding;
      mPrimaryTextPaint.setTextAlign(Paint.Align.RIGHT);
      mSecondaryTextPaint.setTextAlign(Paint.Align.RIGHT);
    } else {
      textOffset = textBox.left + textPadding;
      mPrimaryTextPaint.setTextAlign(Paint.Align.LEFT);
      mSecondaryTextPaint.setTextAlign(Paint.Align.LEFT);
    }

    canvas.drawText(primaryText, textOffset, primaryTextTop, mPrimaryTextPaint);
    canvas.drawText(secondaryText, textOffset, primaryTextTop + lineHeight * primaryTextHeight + secondaryTextHeight, mSecondaryTextPaint);
  }

  private void drawLines(Canvas canvas) {
    float lastX = getPaddingLeft();
    float lastY = getPaddingTop();
    final float innerWidth = getWidth() - (getPaddingLeft() + getPaddingRight());

    int nodeColor = mNodePaint.getColor();
    int lineColor = (0x00ffffff & nodeColor) + (0x88 << 24);
    mNodePaint.setColor(lineColor);
    for(int i = 0, mNodesLength = mNodes.length; i < mNodesLength; i++) {
      Node node = mNodes[i];

      final float x = Math.min(1.0f, node.progress) * innerWidth + getPaddingLeft();
      final float y = i == 0 ? SEGMENT_HEIGHT * 0.5f : i * SEGMENT_HEIGHT + getPaddingTop();

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

    int primaryColor = ContextCompat.getColor(getContext(), R.color.textColorPrimary);
    mPrimaryTextPaint = new Paint();
    mPrimaryTextPaint.setTypeface(Typeface.DEFAULT);
    mPrimaryTextPaint.setTextSize(convertDPtoPX(14));
    mPrimaryTextPaint.setColor(primaryColor);
    mPrimaryTextPaint.setSubpixelText(true);
    mPrimaryTextPaint.setAntiAlias(true);

    int secondaryColor = ContextCompat.getColor(getContext(), R.color.textColorSecondary);
    mSecondaryTextPaint = new Paint();
    mSecondaryTextPaint.setTypeface(Typeface.DEFAULT);
    mSecondaryTextPaint.setTextSize(convertDPtoPX(12));
    mSecondaryTextPaint.setColor(secondaryColor);
    mSecondaryTextPaint.setSubpixelText(true);
    mPrimaryTextPaint.setAntiAlias(true);


    final int themeBackgroundColor = ContextCompat.getColor(getContext(), R.color.windowBackground);
    int backgroundColor = 0xff000000 | themeBackgroundColor; // Strip transparency

    mBackgroundPaint = new Paint();
    mBackgroundPaint.setStyle(Paint.Style.FILL);
    mBackgroundPaint.setColor(backgroundColor);
  }

  private float convertDPtoPX(int dp) {
    Resources r = getResources();
    return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, r.getDisplayMetrics());
  }

  private float calcRadius(Node node) {
    final float min = 5.0f;
    final float max = 50.0f;
    final float hours = (float) node.durationSeconds / (60.0f * 60.0f);
    final float durationRelativeSize = 15.0f * hours;
    return Math.max(min, Math.min(max, durationRelativeSize));
  }

  private class Node {
    public final long durationSeconds;
    public final float progress; // 0..1
    public final long sessionTimestampMs;

    public Node(Session session) {
      this.durationSeconds = session.getDurationSeconds();
      this.progress = session.getEndPosition();
      this.sessionTimestampMs = session.getTimestampMs();
    }
  }

  /**
   * Sorts nodes by progress
   */
  private class LocalSessionComparator implements Comparator<Node> {
    @Override public int compare(Node a, Node b) {
      // Multiply by 10000 to get a percent sensitivity with two decimals
      return (int)(10000 * (a.progress - b.progress));
    }
  }
}

