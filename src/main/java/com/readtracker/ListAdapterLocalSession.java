package com.readtracker;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import com.readtracker.customviews.SessionGraphSegment;
import com.readtracker.db.LocalSession;

import java.util.List;

public class ListAdapterLocalSession extends ArrayAdapter<LocalSession> {
  private LayoutInflater mInflater;

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
    View view = convertView;
    LocalSession localSession = getItem(position);

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

    final float hours = localSession.durationSeconds / (60 * 60);

    holder.sessionGraphSegment.setHeightScale(Math.min(1.0f, 1.0f + hours));
    holder.textDuration.setText("Hours " + hours);
    holder.sessionGraphSegment.setPoints(inPos, outPos);

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
