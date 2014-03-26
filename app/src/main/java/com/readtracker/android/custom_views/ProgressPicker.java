package com.readtracker.android.custom_views;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import com.readtracker.android.R;
import com.readtracker.android.thirdparty.widget.OnWheelChangedListener;
import com.readtracker.android.thirdparty.widget.WheelView;
import com.readtracker.android.thirdparty.widget.adapters.AbstractWheelTextAdapter;
import com.readtracker.android.thirdparty.widget.adapters.NumericWheelAdapter;
import com.readtracker.android.thirdparty.widget.adapters.PercentWheelAdapter;

public class ProgressPicker extends LinearLayout {
  private WheelView mPositionWheel;
  private OnPositionChangeListener mListener;

  @SuppressWarnings("UnusedDeclaration")
  public ProgressPicker(Context context) {
    super(context);
    initializeView();
  }

  @SuppressWarnings("UnusedDeclaration")
  public ProgressPicker(Context context, AttributeSet attrs) {
    super(context, attrs);
    initializeView();
  }

  /**
   * Returns the current page as a ratio of how close it is to the final page.
   */
  public float getPosition() {

    final int currentPage = mPositionWheel.getCurrentItem();
    final int totalPages = mPositionWheel.getViewAdapter().getItemsCount() - 1;

    return totalPages > 0 ? (currentPage / (float) totalPages) : 0f;
  }

  /**
   * Sets the listener to get notified about position changes.
   */
  public void setOnPositionChangeListener(OnPositionChangeListener listener) {
    mListener = listener;
  }

  /**
   * Initializes the progress picker with an initial position
   * and a page count (may be null, in which case percentage mode is used).
   */
  public void setPositionAndPageCount(Float position, Float pageCount) {
    setPageCount(pageCount);
    setPosition(position);
  }

  private void initializeView() {
    final View root = LayoutInflater.from(getContext()).inflate(R.layout._progress_picker, this);
    mPositionWheel = (WheelView) root.findViewById(R.id.position_wheel);
  }

  /**
   * Sets the page count of the wheel. If pageCount is null, percentage mode is used.
   */
  private void setPageCount(Float pageCount) {
    final AbstractWheelTextAdapter adapter;
    if(pageCount == null) {
      adapter = new PercentWheelAdapter(getContext());
    } else {
      adapter = new NumericWheelAdapter(getContext(), 0, pageCount.intValue());
    }

    setupWheelView(mPositionWheel, adapter);
  }

  private void setPosition(Float position) {
    int itemIndex = 0;
    if(position != null) {
      itemIndex = (int) (mPositionWheel.getViewAdapter().getItemsCount() * position);
    }
    mPositionWheel.setCurrentItem(itemIndex);
  }

  /**
   * Setup a wheel view with a numeric wheel adapter and the default style
   */
  private void setupWheelView(WheelView wheelView, AbstractWheelTextAdapter adapter) {
    configureWheelAdapterStyle(adapter);
    wheelView.setViewAdapter(adapter);

    wheelView.setVisibleItems(3);
    wheelView.setCalliperMode(WheelView.CalliperMode.NO_CALLIPERS);

    wheelView.addChangingListener(new OnWheelChangedListener() {
      @Override
      public void onChanged(WheelView wheel, int oldValue, int newValue) {
        if(mListener != null) mListener.onChangeProgress(newValue);
      }
    });
  }

  private void configureWheelAdapterStyle(AbstractWheelTextAdapter wheelAdapter) {
    wheelAdapter.setTextColor(getResources().getColor(R.color.text_color_primary));
    wheelAdapter.setTypeFace(Typeface.DEFAULT);
    wheelAdapter.setTypeStyle(Typeface.NORMAL);
  }

  public boolean isOnLastPosition() {
    return !(getPosition() < 1f);
  }

  /**
   * Callback for listening to position change on the wheel view.
   */
  public static interface OnPositionChangeListener {
    public void onChangeProgress(int newPage);
  }
}
