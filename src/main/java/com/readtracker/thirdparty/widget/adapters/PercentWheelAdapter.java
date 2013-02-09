package com.readtracker.thirdparty.widget.adapters;

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

    final int integer = index / 10;
    final int fraction = index - (integer * 10);
    return String.format("%d.%01d%%", integer, fraction);
  }

  @Override
  public int getItemsCount() {
    return 1000 + 1; // 0.0% => 100.0%
  }
}
