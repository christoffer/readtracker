package com.readtracker;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.readtracker.customviews.SessionView;
import com.readtracker.db.LocalReading;
import com.readtracker.db.LocalSession;

import java.util.ArrayList;

/**
 * Fragment for showing a reading history of a book
 */
public class FragmentSessions extends Fragment {
  private static final String TAG = FragmentSessions.class.getName();

  // Cache lookup of the inflater
  private LayoutInflater mInflater;

  private LocalReading mLocalReading;
  private ArrayList<LocalSession> mLocalSessions;

  private static SessionView mSessionView;
  private static TextView mTextSummary;
  private static TextView mTextSessionCount;

  // Flag for forcing reinitialization (ignore frozen state)
  private boolean mForceReInitialize;

  public static Fragment newInstance(LocalReading localReading, ArrayList<LocalSession> localSessions) {
    Log.d(TAG, "newInstance()");
    FragmentSessions instance = new FragmentSessions();
    instance.setLocalReading(localReading);
    instance.setReadingSessions(localSessions);
    instance.setForceReinitialize(true);
    return instance;
  }

  private void setForceReinitialize(boolean forceReinitialize) {
    mForceReInitialize = forceReinitialize;
  }

  private void setReadingSessions(ArrayList<LocalSession> localSessions) {
    mLocalSessions = localSessions;
  }

  private void setLocalReading(LocalReading localReading) {
    mLocalReading = localReading;
  }

  @Override
  public void onCreate(Bundle in) {
    super.onCreate(in);
    if(in != null && !mForceReInitialize) {
      Log.d(TAG, "unfreezing state");
      mLocalReading = in.getParcelable(IntentKeys.LOCAL_READING);
      mLocalSessions = in.getParcelableArrayList(IntentKeys.READING_SESSIONS);
    }
  }

  @Override
  public void onSaveInstanceState(Bundle out) {
    super.onSaveInstanceState(out);
    Log.d(TAG, "freezing state");
    out.putParcelable(IntentKeys.LOCAL_READING, mLocalReading);
    out.putParcelableArrayList(IntentKeys.READING_SESSIONS, mLocalSessions);
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    Log.d(TAG, "onCreateView()");
    View view = inflater.inflate(R.layout.fragment_sessions, container, false);
    mSessionView = (SessionView) view.findViewById(R.id.sessionView);

    mTextSummary = (TextView) view.findViewById(R.id.textSummary);
    mTextSessionCount = (TextView) view.findViewById(R.id.textSessionCount);

    if(mLocalSessions != null) {
      mSessionView.setSessions(mLocalSessions);

      final String timeSpent = String.format("%s / %d sessions",
        Utils.hoursAndMinutesFromMillis(mLocalReading.timeSpentMillis),
        mLocalSessions.size()
      );

      mTextSummary.setText(timeSpent);
    } else {
      mTextSummary.setVisibility(View.GONE);
    }

    return view;
  }

  private LayoutInflater getInflater() {
    if(mInflater == null) {
      mInflater = (LayoutInflater) getActivity().getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
    }
    return mInflater;
  }
}
