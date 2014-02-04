package com.readtracker.android.fragments;

import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RoundRectShape;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.readtracker.android.IntentKeys;
import com.readtracker.android.R;
import com.readtracker.android.custom_views.SegmentBar;
import com.readtracker.android.custom_views.SessionView;
import com.readtracker.android.db.LocalReading;
import com.readtracker.android.db.LocalSession;
import com.readtracker.android.support.ReadmillApiHelper;
import com.readtracker.android.support.Utils;

import java.util.ArrayList;

/**
 * Fragment for showing a reading history of a book
 */
public class ReadingSessionsFragment extends Fragment {
  private static final String TAG = ReadingSessionsFragment.class.getName();

  private LocalReading mLocalReading;
  private ArrayList<LocalSession> mLocalSessions;

  private static SessionView mSessionView;
  private static SegmentBar mSegmentBar;

  private static TextView mTextSummary;
  private static TextView mTextReadingState;
  private static TextView mTextClosingRemark;
  private static TextView mTextTimeLeft;

  // Flag for forcing reinitialization (ignore frozen state)
  private boolean mForceReInitialize;

  public static Fragment newInstance(LocalReading localReading, ArrayList<LocalSession> localSessions) {
    Log.d(TAG, "newInstance()");
    ReadingSessionsFragment instance = new ReadingSessionsFragment();
    instance.setLocalReading(localReading);
    instance.setReadingSessions(localSessions);
    instance.setForceReinitialize(true);
    return instance;
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

    bindViews(view);

    int color = mLocalReading.getColor();

    mSegmentBar.setColor(color);
    mSegmentBar.setStops(mLocalReading.getProgressStops());

    if(mLocalSessions != null && mLocalSessions.size() > 0) {
      mSessionView.setColor(color);
      mSessionView.setSessions(mLocalSessions);

      presentReadingState(mLocalReading.readmillState, color);
      presentClosingRemark(mLocalReading.getClosingRemark());
      presentSummary(mLocalReading.timeSpentMillis, mLocalReading.estimateTimeLeft(), mLocalSessions.size(), mLocalReading.readmillState);
    } else {
      view.findViewById(R.id.blank_text).setVisibility(View.VISIBLE);
      view.findViewById(R.id.scrollView).setVisibility(View.GONE);
    }

    return view;
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

  private void bindViews(View view) {
    mSessionView = (SessionView) view.findViewById(R.id.sessionView);
    mTextReadingState = (TextView) view.findViewById(R.id.textReadingState);
    mTextClosingRemark = (TextView) view.findViewById(R.id.textClosingRemark);
    mTextSummary = (TextView) view.findViewById(R.id.textSummary);
    mTextTimeLeft = (TextView) view.findViewById(R.id.textTimeLeft);
    mSegmentBar = (SegmentBar) view.findViewById(R.id.segmentBar);
  }

  private void presentReadingState(int readingStateValue, int color) {
    if(readingStateValue == ReadmillApiHelper.ReadingState.READING ||
      readingStateValue == ReadmillApiHelper.ReadingState.INTERESTING) {
      mTextReadingState.setVisibility(View.GONE);
      return;
    }

    String readingState = "";

    if(readingStateValue == ReadmillApiHelper.ReadingState.FINISHED) {
      readingState = "Finished";
    } else if(readingStateValue == ReadmillApiHelper.ReadingState.ABANDONED) {
      readingState = "Abandoned";
    }

    final float radius[] = new float[8];
    for(int i = 0; i < 8; i++) { radius[i] = 3; }
    RoundRectShape roundedRect = new RoundRectShape(radius, null, null);
    ShapeDrawable background = new ShapeDrawable(roundedRect);
    background.getPaint().setColor(color);
    mTextReadingState.setBackgroundDrawable(background);
    mTextReadingState.setText(readingState);
  }

  private void presentClosingRemark(String closingRemark) {
    if(closingRemark != null) {
      mTextClosingRemark.setText(closingRemark);
    } else {
      mTextClosingRemark.setVisibility(View.GONE);
    }
  }

  private void presentSummary(long timeSpentMillis, int estimatedSecondsLeft, int sessionCount, int readingState) {
    if(readingState != ReadmillApiHelper.ReadingState.READING) {
      final String summary = String.format("%s / %d sessions", Utils.longCoarseHumanTimeFromMillis(timeSpentMillis), sessionCount);
      mTextSummary.setText(summary);
      mSegmentBar.setVisibility(View.GONE);
      return;
    }

    // compensate visually for lacking the capsule that finish and abandon have
    mTextSummary.setTextColor(getResources().getColor(R.color.text_color_primary));
    final String summary = String.format("Reading for %s over %d sessions.", Utils.longCoarseHumanTimeFromMillis(timeSpentMillis), sessionCount);
    mTextSummary.setText(summary);

    String pepTalk = null;
    float hoursLeft = (float) estimatedSecondsLeft / (60.0f * 60.0f);
    if(hoursLeft < 1.0f) {
      pepTalk = "Why not finish it today?";
    } else if(hoursLeft < 4.0f) {
      // hours per day to finish in 3 days
      final int secondsPerDayForGoal = (int) ((hoursLeft / 3.0f) * 3600);
      pepTalk = String.format("That's about %s per day to finish it in three days.",
        Utils.longCoarseHumanTimeFromMillis(secondsPerDayForGoal * 1000));
    } else if(hoursLeft < 10) {
      // hours per day to finish in a week
      final int secondsPerDayForGoal = (int) ((hoursLeft / 7.0f) * 3600);
      pepTalk = String.format("That's about %s per day to finish it in a week.",
        Utils.longCoarseHumanTimeFromMillis(secondsPerDayForGoal * 1000));
    } else if(hoursLeft < 20) {
      // hours per day to finish in two weeks
      final int secondsPerDayForGoal = (int) ((hoursLeft / 14.0f) * 3600);
      pepTalk = String.format("That's about %s per day to finish it in two weeks.",
        Utils.longCoarseHumanTimeFromMillis(secondsPerDayForGoal * 1000));
    }

    String timeLeft = String.format("You have about %s left of reading, given you keep the same pace", Utils.longCoarseHumanTimeFromMillis(estimatedSecondsLeft * 1000));

    if(pepTalk != null) {
      timeLeft += ".\n\n" + pepTalk;
    }

    mTextTimeLeft.setText(timeLeft);
  }
}
