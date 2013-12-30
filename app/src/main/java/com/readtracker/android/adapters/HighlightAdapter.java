package com.readtracker.android.adapters;

import android.app.Activity;
import android.content.Context;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.readtracker.android.R;
import com.readtracker.android.support.DrawableGenerator;
import com.readtracker.android.support.Utils;

import java.util.Comparator;
import java.util.List;

/**
 * Shows a list of highlights
 */
public class HighlightAdapter extends ArrayAdapter<HighlightItem> {
  private int mColor;
  private LayoutInflater mInflater;

  private Comparator<HighlightItem> mReadingHighlightComparator = new Comparator<HighlightItem>() {
    @Override
    public int compare(HighlightItem highlightA, HighlightItem highlightB) {
      return highlightA.getHighlightedAt().after(highlightB.getHighlightedAt()) ? -1 : 1;
    }
  };

  public HighlightAdapter(Context context, int textViewResourceId, List<HighlightItem> highlights) {
    super(context, textViewResourceId, highlights);
    sort(mReadingHighlightComparator);
    mInflater = (LayoutInflater) context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
  }

  public void setColor(int color) {
    mColor = color;
    notifyDataSetChanged();
  }

  @Override
  public View getView(int position, View convertView, ViewGroup parent) {
    HighlightItem item = getItem(position);

    if(convertView == null) {
      convertView = mInflater.inflate(R.layout.highlight_list_item, null);
    }

    TextView textContent = (TextView) convertView.findViewById(R.id.textContent);
    textContent.setText(item.getContent().trim());
    textContent.setTextSize(textSizeForContent(item.getContent()));

    TextView textDate = (TextView) convertView.findViewById(R.id.textDate);

    String metadata = Utils.humanPastDate(item.getHighlightedAt());
    int likeCount = item.getLikeCount();
    int commentCount = item.getCommentCount();

    if(likeCount > 0) {
      metadata += String.format(" ・ Liked by %d %s", likeCount, (likeCount == 1 ? "person" : "people"));
    }

    if(commentCount > 0) {
      metadata += String.format(" ・ %d %s", commentCount, Utils.pluralize(commentCount, "comment"));
    }

    textDate.setText(metadata);
    final int backgroundColor = getContext().getResources().getColor(R.color.background);
    final int itemColor = mColor;
    convertView.setBackgroundDrawable(DrawableGenerator.generateListItemBackground(itemColor, backgroundColor));
    return convertView;
  }

  @Override
  public void add(HighlightItem object) {
    super.add(object);
    sort(mReadingHighlightComparator);
  }

  public void remove(int localHighlightId) {
    for(int i = 0; i < getCount(); i++) {
      HighlightItem item = getItem(i);
      if(item.getLocalHighlight().id == localHighlightId) {
        remove(item);
        notifyDataSetChanged();
        return;
      }
    }
  }

  private float textSizeForContent(String content) {
    if(content == null || content.length() < 100) {
      return textSizeFromDP(18);
    }
    if(content.length() < 350) {
      return textSizeFromDP(14);
    }
    return textSizeFromDP(10);
  }

  private float textSizeFromDP(int dp) {
    return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getContext().getResources().getDisplayMetrics());
  }
}
