package com.readtracker.android.adapters;

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

/** Shows a list of quotes */
public class QuoteAdapter extends ArrayAdapter<Quote> {
  private int mColor;

  private Comparator<Quote> mQuoteComparator = new Comparator<Quote>() {
    @Override
    public int compare(Quote a, Quote b) {
      return a.getAddTimestamp() > b.getAddTimestamp() ? -1 : 1;
    }
  };

  public QuoteAdapter(Context context, int textViewResourceId, List<Quote> quotes) {
    super(context, textViewResourceId, quotes);
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
    Quote quote = getItem(position);

    if(convertView == null) {
      convertView = LayoutInflater.from(getContext()).inflate(R.layout.quote_list_item, null);
    }

    TextView textContent = (TextView) convertView.findViewById(R.id.textContent);
    textContent.setText(quote.getContent().trim());
    final long contentLength = quote.getContent() == null ? 0 : quote.getContent().length();
    textContent.setTextSize(getTextSizeForContentLength(contentLength));

    TextView textDate = (TextView) convertView.findViewById(R.id.textDate);

    String metadata = Utils.humanPastDate(quote.getAddTimestamp());

    textDate.setText(metadata);
    final int backgroundColor = getContext().getResources().getColor(R.color.background);
    final int itemColor = mColor;
    convertView.setBackgroundDrawable(DrawableGenerator.generateListItemBackground(itemColor, backgroundColor));
    return convertView;
  }

  private float getTextSizeForContentLength(long length) {
    if(length < 100) {
      return textSizeFromDP(18);
    }
    if(length < 350) {
      return textSizeFromDP(14);
    }
    return textSizeFromDP(10);
  }

  private float textSizeFromDP(int dp) {
    return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getContext().getResources().getDisplayMetrics());
  }
}
