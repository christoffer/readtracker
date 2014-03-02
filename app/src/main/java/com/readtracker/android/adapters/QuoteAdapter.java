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
import com.readtracker.android.db.Quote;
import com.readtracker.android.support.DrawableGenerator;
import com.readtracker.android.support.Utils;

import java.util.Comparator;
import java.util.List;

/**
 * Shows a list of quotes
 */
public class QuoteAdapter extends ArrayAdapter<Quote> {
  private int mColor;
  private LayoutInflater mInflater;

  private Comparator<Quote> mQuoteComparator = new Comparator<Quote>() {
    @Override
    public int compare(Quote a, Quote b) {
      return a.getAddTimestamp() > b.getAddTimestamp() ? -1 : 1;
    }
  };

  public QuoteAdapter(Context context, int textViewResourceId, List<Quote> quotes) {
    super(context, textViewResourceId, quotes);
    sort(mQuoteComparator);
    mInflater = (LayoutInflater) context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
  }

  public void setColor(int color) {
    mColor = color;
    notifyDataSetChanged();
  }

  @Override
  public View getView(int position, View convertView, ViewGroup parent) {
    Quote quote = getItem(position);

    if(convertView == null) {
      convertView = mInflater.inflate(R.layout.highlight_list_item, null);
    }

    TextView textContent = (TextView) convertView.findViewById(R.id.textContent);
    textContent.setText(quote.getContent().trim());
    textContent.setTextSize(textSizeForContent(quote.getContent()));

    TextView textDate = (TextView) convertView.findViewById(R.id.textDate);

    String metadata = Utils.humanPastDate(quote.getAddTimestamp());

    textDate.setText(metadata);
    final int backgroundColor = getContext().getResources().getColor(R.color.background);
    final int itemColor = mColor;
    convertView.setBackgroundDrawable(DrawableGenerator.generateListItemBackground(itemColor, backgroundColor));
    return convertView;
  }

  public void removeById(int idOfItemToRemove) {
    for(int i = 0; i < getCount(); i++) {
      Quote quote = getItem(i);
      if(quote.getId() == idOfItemToRemove) {
        remove(quote);
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
