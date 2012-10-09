package com.readtracker;

import android.app.Activity;
import android.content.Context;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.Comparator;
import java.util.List;

/**
 * Shows a list of highlights
 */
public class ListAdapterHighlight extends ArrayAdapter<ListItemHighlight> {

  private LayoutInflater mInflater;

  private Comparator<ListItemHighlight> mReadingHighlightComparator = new Comparator<ListItemHighlight>() {
    @Override
    public int compare(ListItemHighlight rdA, ListItemHighlight rdB) {
      if(rdA.getHighlightedAt().after(rdB.getHighlightedAt()))
        return -1;
      else
        return 1;
    }
  };

  public ListAdapterHighlight(Context context, int textViewResourceId, List<ListItemHighlight> highlights) {
    super(context, textViewResourceId, highlights);
    sort(mReadingHighlightComparator);
    mInflater = (LayoutInflater) context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
  }

  @Override
  public View getView(int position, View convertView, ViewGroup parent) {
    ListItemHighlight item = getItem(position);

    if(convertView == null) {
      convertView = mInflater.inflate(R.layout.highlight_list_item, null);
    }

    TextView textContent = (TextView) convertView.findViewById(R.id.textContent);
    textContent.setText(item.getContent());
    textContent.setTextSize(textSizeForContent(item.getContent()));
    return convertView;
  }

  private float textSizeForContent(String content) {
    if(content == null || content.length() < 100) {
      return textSizeFromDP(18);
    }
    if(content.length() < 500) {
      return textSizeFromDP(14);
    }
    return textSizeFromDP(10);
  }

  private float textSizeFromDP(int dp) {
    return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getContext().getResources().getDisplayMetrics());
  }

  @Override
  public void add(ListItemHighlight object) {
    super.add(object);
    sort(mReadingHighlightComparator);
  }
}
