package com.readtracker.custom_views;

import android.app.Activity;
import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import com.readtracker.R;
import com.readtracker.db.LocalReading;
import com.readtracker.thirdparty.widget.OnWheelChangedListener;
import com.readtracker.thirdparty.widget.WheelView;
import com.readtracker.thirdparty.widget.adapters.AbstractWheelTextAdapter;
import com.readtracker.thirdparty.widget.adapters.NumericWheelAdapter;
import com.readtracker.thirdparty.widget.adapters.PercentWheelAdapter;

public class ProgressPicker extends LinearLayout {
  private static final int PAGES_MODE = 0;
  private static final int PERCENT_MODE = 1;

  private int mMode = PAGES_MODE;
  private WheelView mWheelEndingPage;

  private OnProgressChangeListener mListener;
  private View mRootView;

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
  }

  public void setupForLocalReading(LocalReading localReading) {
    if(mRootView == null) {
      LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
      mRootView = inflater.inflate(R.layout._progress_picker, this);
      mWheelEndingPage = (WheelView) mRootView.findViewById(R.id.wheelEndingPage);
    }

    if(localReading.isMeasuredInPercent()) {
      setupPercentMode((int) localReading.currentPage);
    } else {
      setupPagesMode((int) localReading.currentPage, (int) localReading.totalPages);
    }
  }

  public int getCurrentPage() {
    return mWheelEndingPage.getCurrentItem();
  }

  public void setCurrentPage(int page) {
    mWheelEndingPage.setCurrentItem(page);
  }

  /**
   * Return the current page as a ratio of how close it is to the final page.
   *
   * @return the progress ratio [0..1]
   */
  public float getProgress() {
    final int totalPages = mWheelEndingPage.getViewAdapter().getItemsCount();
    if(totalPages == 0) {
      return 0.0f;
    }
    return (float) getCurrentPage() / (float) totalPages;
  }

  public void setOnProgressChangeListener(OnProgressChangeListener listener) {
    mListener = listener;
  }

  protected void setupPercentMode(int currentPage) {
    mMode = PERCENT_MODE;
    setupWheelView(mWheelEndingPage, 1000);
    setCurrentPage(currentPage);
  }

  protected void setupPagesMode(int currentPage, int totalPages) {
    mMode = PAGES_MODE;
    setupWheelView(mWheelEndingPage, totalPages);
    setCurrentPage(currentPage);
  }

  /**
   * Setup a wheel view with a numeric wheel adapter and the default style
   */
  private void setupWheelView(WheelView wheelView, int maxPages) {
    AbstractWheelTextAdapter adapter;
    if(mMode == PAGES_MODE) {
      adapter = new NumericWheelAdapter(getContext(), 0, maxPages);
    } else {
      adapter = new PercentWheelAdapter(getContext());
    }

    configureWheelAdapterStyle(adapter);
    wheelView.setViewAdapter(adapter);
    configureWheelView(wheelView);
  }

  private void configureWheelView(WheelView wheelView) {
    wheelView.setVisibleItems(3);
    wheelView.setCalliperMode(WheelView.CalliperMode.NO_CALLIPERS);

    wheelView.addChangingListener(new OnWheelChangedListener() {
      @Override public void onChanged(WheelView wheel, int oldValue, int newValue) {
        if(mListener != null) mListener.onChangeProgress(newValue);
      }
    });
  }

  private void configureWheelAdapterStyle(AbstractWheelTextAdapter wheelAdapter) {
    wheelAdapter.setTextColor(getResources().getColor(R.color.text_color_primary));
    wheelAdapter.setTypeFace(Typeface.DEFAULT);
    wheelAdapter.setTypeStyle(Typeface.NORMAL);
  }
}
