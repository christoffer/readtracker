package com.readtracker.android.custom_views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.NumberPicker;

import com.readtracker.R;

import java.util.Locale;

import butterknife.ButterKnife;
import butterknife.InjectView;

/**
 * ProgressPicker is a view that lest the user select a page or percent indicating how much progress
 * they made in a reading session.
 */
public class ProgressPicker extends LinearLayout {

  @InjectView(R.id.position_picker) NumberPicker mPositionPicker;
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

    final int currentPage = mPositionPicker.getValue();
    final int totalPages = mPositionPicker.getMaxValue() - 1;

    return totalPages > 0 ? (currentPage / (float) totalPages) : 0f;
  }

  /**
   * Returns true if the position is on the last position possible (i.e. end of the book)
   */
  public boolean isOnLastPosition() {
    return mPositionPicker.getValue() == mPositionPicker.getMaxValue();
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
    if (pageCount == null) {
      // Make the more than questionable assumption that passing `null` for pageCount
      // means we're tracking progress in percent.
      // TODO(christoffer) Refactor this to avoid hacks like this
      setupPercentTrackingMode();
    } else {
      setupPageTrackingMode(pageCount.intValue());
    }

    mPositionPicker.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
      @Override public void onValueChange(NumberPicker picker, int oldValue, int newValue) {
        if(mListener != null) mListener.onChangeProgress(newValue);
      }
    });

    updatePosition(position);
  }

  private void initializeView() {
    final View root = LayoutInflater.from(getContext()).inflate(R.layout._progress_picker, this);
    ButterKnife.inject(this, root);
  }

  private void setupPageTrackingMode(int pageCount) {
    mPositionPicker.setMinValue(0);
    mPositionPicker.setMaxValue(pageCount);
  }

  private void setupPercentTrackingMode() {
    // Use percent mode, let the user select progress based on 0.1% increments
    String[] values = new String[1001];
    final Locale locale = Locale.getDefault();
    for (int i  = 0; i < 1001; i++) {
      values[i] = String.format(locale, "%.2f%%", (float)i / 100.0f);
    }
    mPositionPicker.setMinValue(0);
    mPositionPicker.setMaxValue(1000);
    mPositionPicker.setDisplayedValues(values);
  }

  /**
   * Update the position based on a ratio between 0 - 1.0.
   * This assumes that the position picker range has been initialized.
   */
  private void updatePosition(Float position) {
    int value = 0;
    if(position != null) {
      value = (int) (mPositionPicker.getMaxValue() * position);
    }
    mPositionPicker.setValue(value);
  }

  /**
   * Callback for listening to position change on the wheel view.
   */
  public interface OnPositionChangeListener {
    void onChangeProgress(int newPage);
  }
}
