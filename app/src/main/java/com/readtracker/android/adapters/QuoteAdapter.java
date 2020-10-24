package com.readtracker.android.adapters;

import android.content.Context;
import androidx.annotation.NonNull;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.readtracker.R;
import com.readtracker.android.db.Quote;
import com.readtracker.android.support.StringUtils;

import java.util.Comparator;
import java.util.List;

/** Shows a list of quotes */
public class QuoteAdapter extends ArrayAdapter<Quote> {
  private final boolean mUseFullDates;
  private int mColor;

  private final Comparator<Quote> mQuoteComparator = new Comparator<Quote>() {
    @Override
    public int compare(Quote a, Quote b) {
      final long timestampA = a.getAddTimestampMs();
      final long timestampB = b.getAddTimestampMs();
      //noinspection UseCompareMethod -- Can't use this with our current min sdk target
      return timestampA == timestampB ? 0 : timestampA > timestampB ? -1 : 1;
    }
  };

  public QuoteAdapter(Context context, List<Quote> quotes, boolean useFullDates) {
    super(context, R.layout.quote_list_item, quotes);
    mUseFullDates = useFullDates;
    sortQuotes();
  }

  public void sortQuotes() {
    sort(mQuoteComparator);
  }

  public void setColor(int color) {
    mColor = color;
    notifyDataSetChanged();
  }

  @NonNull @Override
  public View getView(int position, View convertView, @NonNull ViewGroup parent) {
    final Quote quote = getItem(position);

    if(convertView == null) {
      convertView = LayoutInflater.from(getContext()).inflate(R.layout.quote_list_item, parent, false);
    }

    final TextView textContent = convertView.findViewById(R.id.textContent);
    final TextView textDate = convertView.findViewById(R.id.textDate);
    final String NA = getContext().getString(R.string.general_not_available_short);

    if (quote == null) {
      textContent.setText(NA);
      applyTextSizeForContentLength(textContent, 0);
      textDate.setText(NA);
    } else {
      final String content = quote.getContent();
      if (content == null) {
        applyTextSizeForContentLength(textContent, 0);
        textContent.setText(NA);
      } else {
        applyTextSizeForContentLength(textContent, content.length());
        textContent.setText(content);
      }
      final long now = System.currentTimeMillis();
      final String metadata;
      if (mUseFullDates) {
        metadata = StringUtils.getDateString(quote.getAddTimestampMs(), convertView.getContext());
      } else {
        metadata = StringUtils.humanPastTimeFromTimestamp(quote.getAddTimestampMs(), now, getContext());
      }
      textDate.setText(metadata);
    }

    return convertView;
  }

  private void applyTextSizeForContentLength(TextView textView, long contentLength) {
    float textSizeSp = 18.0f;
    if(contentLength > 100) {
      textSizeSp = 16.0f;
    }
    if(contentLength > 350) {
      textSizeSp = 14.0f;
    }
    textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSp);
  }
}
