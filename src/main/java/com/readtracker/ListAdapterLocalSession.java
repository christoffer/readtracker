package com.readtracker;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import com.readtracker.custom_views.SessionGraphSegment;
import com.readtracker.db.LocalSession;

import java.util.List;

public class ListAdapterLocalSession extends ArrayAdapter<LocalSession> {
  private LayoutInflater mInflater;

  private static final float SEGMENT_BASE_HEIGHT = 240; // How big a segment of 1 hour is
  private static final int MIN_SEGMENT_HEIGHT = 72;
  private static final int MAX_SEGMENT_HEIGHT = (int) (SEGMENT_BASE_HEIGHT * 4);

  private class ViewHolder {
    TextView textDuration;
    SessionGraphSegment sessionGraphSegment;

    public ViewHolder(View root) {
      textDuration = (TextView) root.findViewById(R.id.textDuration);
      sessionGraphSegment = (SessionGraphSegment) root.findViewById(R.id.sessionGraphSegment);
    }
  }

  public ListAdapterLocalSession(Context context, int textViewResourceId, List<LocalSession> localSessions) {
    super(context, textViewResourceId, localSessions);
  }

  @Override
  public View getView(int position, View convertView, ViewGroup parent) {
    LocalSession localSession = getItem(position);

    View view = convertView;
    ViewHolder holder;
    if(view == null) {
      view = getInflater().inflate(R.layout.local_session_item, null);
      holder = new ViewHolder(view);
      view.setTag(holder);
    } else {
      holder = (ViewHolder) view.getTag();
    }

    String duration = Utils.hoursAndMinutesFromMillis(localSession.durationSeconds * 1000);
    holder.textDuration.setText(duration);

    LocalSession previousItem = null;
    if(position > 0) {
      previousItem = getItem(position - 1);
    }

    final float inPos = previousItem == null ? 0.0f : (float) previousItem.progress;
    final float outPos = (float) localSession.progress;
    holder.sessionGraphSegment.setPoints(inPos, outPos);

    final float hours = localSession.durationSeconds / (float)(60 * 60);
    final int calculatedHeight = (int)((float) Math.log(1.0f + hours) * SEGMENT_BASE_HEIGHT);
    // clamp height to avoid crazy items
    final int height = Math.max(MIN_SEGMENT_HEIGHT, Math.min(MAX_SEGMENT_HEIGHT, calculatedHeight));

    holder.sessionGraphSegment.setHeight(height);

    return view;
  }

  @Override public boolean isEnabled(int position) {
    return false; // Prevent orange flash on click
  }

  private LayoutInflater getInflater() {
    if(mInflater == null) {
      mInflater = (LayoutInflater) getContext().getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
    }
    return mInflater;
  }
}
