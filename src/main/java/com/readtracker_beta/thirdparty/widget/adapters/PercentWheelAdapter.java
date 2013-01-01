package com.readtracker_beta.thirdparty.widget.adapters;

import android.content.Context;

public class PercentWheelAdapter extends AbstractWheelTextAdapter {

  public PercentWheelAdapter(Context context) {
    super(context);
  }

  public PercentWheelAdapter(Context context, int itemResource) {
    super(context, itemResource);
  }

  public PercentWheelAdapter(Context context, int itemResource, int itemTextResource) {
    super(context, itemResource, itemTextResource);
  }

  @Override public CharSequence getItemText(int index) {
    if(index < 0 || index >= getItemsCount()) { return null; }

    final int integer = index / 100;
    final int fraction = index - (integer * 100);
    return String.format("%d.%02d%%", integer, fraction);
  }

  @Override
  public int getItemsCount() {
    return 10000; // 100.00%
  }
}
