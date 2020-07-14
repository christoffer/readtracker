package com.readtracker.android.adapters;

import android.content.Context;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;
import android.widget.TextView;

import com.readtracker.R;
import com.readtracker.android.custom_views.SessionItemBackground;
import com.readtracker.android.db.Session;
import com.readtracker.android.support.StringUtils;
import com.readtracker.android.support.Utils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import butterknife.ButterKnife;
import butterknife.InjectView;

/** Adapter for displaying a filtered list of books. */
public class ReadingSessionAdapter extends BaseAdapter implements ListAdapter {
  private static final String TAG = ReadingSessionAdapter.class.getName();

  private final Context mContext;
  private final int mLayoutResource;
  private final List<Session> mSessions = new ArrayList<>();
  private Comparator<Session> mSessionComparator = new ComparatorSessionByStartDate();

  public ReadingSessionAdapter(Context context, int resource) {
    super();
    mContext = context;
    mLayoutResource = resource;
  }

  @Override public int getCount() {
    return mSessions.size();
  }

  @Override public Session getItem(int position) {
    return mSessions.get(position);
  }

  @Override public long getItemId(int position) {
    return getItem(position).getId();
  }

  @Override
  public View getView(int position, View convertView, ViewGroup parent) {
    final ViewHolder viewHolder;
    if(convertView == null) {
      convertView = LayoutInflater.from(mContext).inflate(mLayoutResource, null);
      viewHolder = new ViewHolder(convertView);
      convertView.setTag(viewHolder);
    } else {
      viewHolder = (ViewHolder) convertView.getTag();
    }

    Session book = getItem(position);
    viewHolder.populate(convertView, book);

    return convertView;
  }

  @Override public boolean areAllItemsEnabled() {
    return true;
  }

  @Override public boolean isEnabled(int position) {
    return true;
  }

  public void setSessions(List<Session> sessions) {
    mSessions.clear();
    mSessions.addAll(sessions);
    Collections.sort(mSessions, mSessionComparator);
    notifyDataSetChanged();
  }

  static class ViewHolder {
    private final SimpleDateFormat mDateFormatter;

    @InjectView(R.id.primaryInfoText) TextView primaryInfoText;
    @InjectView(R.id.secondaryInfoText) TextView secondaryInfoText;
    @InjectView(R.id.progressBackgroundView) SessionItemBackground progressBackgroundView;

    ViewHolder(View view) {
      ButterKnife.inject(this, view);
      final Context context = view.getContext();
      final String dateFormat = context.getString(R.string.ReadingSessionAdapter_date_format);
      final Locale locale = Utils.getLocale(context);
      mDateFormatter = new SimpleDateFormat(dateFormat, locale);
    }

    void populate(View view, Session session) {
      final Context context = view.getContext();

      // Calculate left side text
      final String formattedLength = StringUtils.formatSessionReadAmountHtml(context, session);
      final String formattedDuration = StringUtils.formatSessionDurationHtml(context, session);
      final String primaryInfo = String.format("%s <br/> %s", formattedLength, formattedDuration);
      primaryInfoText.setText(Html.fromHtml(primaryInfo));

      // Calculate right side text
      final String formattedInterval = StringUtils.formatSessionFromTo(context, session);
      final Date sessionStart = new Date(session.getTimestampMs());
      String formattedDate = mDateFormatter.format(sessionStart);
      // Doesn't seem to be a way to format AM/PM to lowercase, so force it
      formattedDate = formattedDate.replace("AM", "am").replace("PM", "pm");
      final String secondaryInfo = String.format("%s <br /> %s", formattedInterval, formattedDate);
      secondaryInfoText.setText(Html.fromHtml(secondaryInfo));

      progressBackgroundView.initForSession(session);
    }
  }

  private static class ComparatorSessionByStartDate implements Comparator<Session> {
    @Override public int compare(Session sessionA, Session sessionB) {
      final long tsA = sessionA.getTimestampMs();
      final long tsB = sessionB.getTimestampMs();

      //noinspection UseCompareMethod
      return tsA < tsB ? -1 : (tsA > tsB ? 1 : 0);
    }
  }
}
