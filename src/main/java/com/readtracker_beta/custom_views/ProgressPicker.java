package com.readtracker_beta.custom_views;

import android.app.Activity;
import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.readtracker_beta.R;
import com.readtracker_beta.thirdparty.widget.OnWheelChangedListener;
import com.readtracker_beta.thirdparty.widget.WheelView;
import com.readtracker_beta.thirdparty.widget.adapters.NumericWheelAdapter;

public class ProgressPicker extends LinearLayout {
  private WheelView mWheelEndingPage;
  private WheelView mWheelEndPercentInteger;
  private WheelView mWheelEndPercentFraction;
  private TextView mTextLabel;

  private boolean mPercentMode;
  private int mTotalPageCount = 0;

  private OnProgressChangeListener mListener;

  public interface OnProgressChangeListener {
    /**
     * Triggered when the ProgressPicker's number has changed.
     *
     * @param newPage the new page number
     */
    public void onChangeProgress(int newPage);
  }

  @SuppressWarnings("UnusedDeclaration")
  public ProgressPicker(Context context) {
    this(context, null);
  }

  public ProgressPicker(Context context, AttributeSet attrs) {
    super(context, attrs);
    LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
    View root = inflater.inflate(R.layout._progress_picker, this);
    bindViews(root);
  }

  public void setOnProgressChangeListener(OnProgressChangeListener listener) {
    mListener = listener;
  }

  public void setText(String text) {
    mTextLabel.setText(text);
  }

  public void setupPercentMode(int currentPage) {
    mPercentMode = true;
    mTotalPageCount = 0;
    initializeWheelViews();
    setPage(currentPage);
  }

  public void setupPagesMode(int currentPage, int totalPages) {
    mPercentMode = false;
    mTotalPageCount = totalPages;
    initializeWheelViews();
    setPage(currentPage);
  }

  public int getPage() {
    if(mPercentMode) {
      int percentInteger = mWheelEndPercentInteger.getCurrentItem();
      int percentFraction = mWheelEndPercentFraction.getCurrentItem();
      // Concatenate 45 and 17 => 4517
      return percentInteger * 100 + percentFraction;
    }
    return mWheelEndingPage.getCurrentItem();
  }

  public void setPage(int page) {
    if(mPercentMode) {
      // Split 578 => 5 and 78
      int currentInteger = page / 100;
      int currentFraction = page - currentInteger * 100;

      mWheelEndPercentInteger.setCurrentItem(Math.min(99, currentInteger));
      mWheelEndPercentFraction.setCurrentItem(Math.min(99, currentFraction));
    } else {
      mWheelEndingPage.setCurrentItem(page);
    }
  }

  public float getProgress() {
    if(mTotalPageCount == 0) {
      return 0.0f;
    }
    return (float) getPage() / (float) mTotalPageCount;
  }

  private void bindViews(View root) {
    // Wheel for normal pages
    mWheelEndingPage = (WheelView) root.findViewById(R.id.wheelEndingPage);

    // Wheel for percents
    mWheelEndPercentInteger = (WheelView) root.findViewById(R.id.wheelEndPercentInteger);
    mWheelEndPercentFraction = (WheelView) root.findViewById(R.id.wheelEndPercentFraction);

    mTextLabel = (TextView) root.findViewById(R.id.textLabel);
  }

  private void initializeWheelViews() {
    if(mPercentMode) {
      setupWheelView(mWheelEndPercentInteger, 99);
      setupWheelView(mWheelEndPercentFraction, 99);

      mWheelEndPercentInteger.setCalliperMode(WheelView.CalliperMode.LEFT_CALLIPER);
      mWheelEndPercentFraction.setCalliperMode(WheelView.CalliperMode.RIGHT_CALLIPER);

      mWheelEndingPage.setVisibility(View.GONE);
    } else {
      setupWheelView(mWheelEndingPage, mTotalPageCount);
      mWheelEndPercentInteger.setVisibility(View.GONE);
      mWheelEndPercentFraction.setVisibility(View.GONE);
      findViewById(R.id.textDot).setVisibility(View.GONE);
    }
  }

  /**
   * Setup a wheel view with a numeric wheel adapter and the default style
   */
  private void setupWheelView(WheelView wheelView, int maxNumber) {
    NumericWheelAdapter adapter = new NumericWheelAdapter(getContext(), 0, maxNumber);
    configureWheelAdapterStyle(adapter);
    wheelView.setViewAdapter(adapter);
    configureWheelView(wheelView);
  }

  private void configureWheelView(WheelView wheelView) {
    wheelView.setVisibleItems(3);

    wheelView.addChangingListener(new OnWheelChangedListener() {
      @Override public void onChanged(WheelView wheel, int oldValue, int newValue) {
        if(mListener != null) mListener.onChangeProgress(newValue);
      }
    });
  }

  private void configureWheelAdapterStyle(NumericWheelAdapter wheelAdapter) {
    wheelAdapter.setTextColor(getResources().getColor(R.color.text_color_primary));
    wheelAdapter.setTypeFace(Typeface.DEFAULT);
    wheelAdapter.setTypeStyle(Typeface.NORMAL);
  }
}
