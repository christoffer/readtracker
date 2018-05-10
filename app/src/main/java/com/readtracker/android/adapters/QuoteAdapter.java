package com.readtracker.android.adapters;

import android.content.Context;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.readtracker.R;
import com.readtracker.android.db.Quote;
import com.readtracker.android.support.ColorUtils;
import com.readtracker.android.support.DrawableGenerator;
import com.readtracker.android.support.Utils;

import java.util.Comparator;
import java.util.List;

/** Shows a list of quotes */
public class QuoteAdapter extends ArrayAdapter<Quote> {
  private int mColor;

  private final Comparator<Quote> mQuoteComparator = new Comparator<Quote>() {
    @Override
    public int compare(Quote a, Quote b) {
      return a.getAddTimestampMs() > b.getAddTimestampMs() ? -1 : 1;
    }
  };

  public QuoteAdapter(Context context, List<Quote> quotes) {
    super(context, R.layout.quote_list_item, quotes);
    sortQuotes();
  }

  public void sortQuotes() {
    sort(mQuoteComparator);
  }

  public void setColor(int color) {
    mColor = color;
    notifyDataSetChanged();
  }

  @Override
  public View getView(int position, View convertView, ViewGroup parent) {
    final Quote quote = getItem(position);

    if(convertView == null) {
      convertView = LayoutInflater.from(getContext()).inflate(R.layout.quote_list_item, parent, false);
    }

    final TextView textContent = (TextView) convertView.findViewById(R.id.textContent);
    final TextView textDate = (TextView) convertView.findViewById(R.id.textDate);

    if (quote == null) {
      textContent.setText("N/A");
      textContent.setTextSize(getTextSizeForContentLength(0));
      textDate.setText("N/A");
    } else {
      final String content = quote.getContent();
      if (content == null) {
        textContent.setTextSize(getTextSizeForContentLength(0));
        textContent.setText("N/A");
      } else {
        textContent.setTextSize(getTextSizeForContentLength(content.length()));
        textContent.setText(content);
      }
      final long now = System.currentTimeMillis();
      final String metadata = Utils.humanPastTimeFromTimestamp(quote.getAddTimestampMs(), now);
      textDate.setText(metadata);
    }

    final int backgroundColor = ColorUtils.getBackgroundColor(convertView.getContext());
    final int itemColor = mColor;
    convertView.setBackgroundDrawable(DrawableGenerator.generateListItemBackground(itemColor, backgroundColor));
    return convertView;
  }

  private float getTextSizeForContentLength(long length) {
    if(length < 100) {
      return textSizeFromSP(14);
    }
    if(length < 350) {
      return textSizeFromSP(12);
    }
    return textSizeFromSP(10);
  }

  private float textSizeFromSP(int dp) {
    return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, dp, getContext().getResources().getDisplayMetrics());
  }
}
