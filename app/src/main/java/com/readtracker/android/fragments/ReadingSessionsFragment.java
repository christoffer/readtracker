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

import com.readtracker.android.R;
import com.readtracker.android.custom_views.SegmentBar;
import com.readtracker.android.custom_views.SessionView;
import com.readtracker.android.db.Book;
import com.readtracker.android.db.Session;
import com.readtracker.android.support.Utils;

import java.util.List;

/**
 * Fragment for showing a reading history of a book
 */
public class ReadingSessionsFragment extends Fragment {
  private static final String TAG = ReadingSessionsFragment.class.getName();

  private Book mBook;
  private View mRootView = null;

  private static SessionView mSessionView;
  private static SegmentBar mSegmentBar;

  private static TextView mTextSummary;
  private static TextView mTextReadingState;
  private static TextView mTextClosingRemark;
  private static TextView mTextTimeLeft;

  public static Fragment newInstance() {
    Log.d(TAG, "newInstance()");
    return new ReadingSessionsFragment();
  }

  @Override
  public void onCreate(Bundle in) {
    super.onCreate(in);
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    Log.d(TAG, "onCreateView()");
    View view = inflater.inflate(R.layout.fragment_sessions, container, false);

    setRootView(view);

    return view;
  }

  /** Deferr populating the fields until both the UI and the data is available. */
  private void populateFieldsDeferred() {
    if(mBook == null || mRootView == null) {
      return;
    }

    final int color = Utils.calculateBookColor(mBook);
    final List<Session> sessions = mBook.getSessions();

    mSegmentBar.setColor(color);
    mSegmentBar.setStops(Utils.getSessionStops(sessions));

    if(sessions.size() > 0) {
      mSessionView.setColor(color);
      mSessionView.setSessions(sessions);

      populateReadingState(mBook.getState(), color);
      popluateClosingRemark(mBook.getClosingRemark());
      populateSummary();
      populateTimeLeft();
    } else {
      mRootView.findViewById(R.id.blank_text).setVisibility(View.VISIBLE);
      mRootView.findViewById(R.id.scrollView).setVisibility(View.GONE);
    }
  }

  private void setRootView(View view) {
    mSessionView = (SessionView) view.findViewById(R.id.sessionView);
    mTextReadingState = (TextView) view.findViewById(R.id.textReadingState);
    mTextClosingRemark = (TextView) view.findViewById(R.id.textClosingRemark);
    mTextSummary = (TextView) view.findViewById(R.id.textSummary);
    mTextTimeLeft = (TextView) view.findViewById(R.id.textTimeLeft);
    mSegmentBar = (SegmentBar) view.findViewById(R.id.segmentBar);
    mRootView = view;

    populateFieldsDeferred();
  }

  private void populateReadingState(Book.State state, int color) {
    if(state == Book.State.Reading || state == Book.State.Unknown) {
      mTextReadingState.setVisibility(View.GONE);
      return;
    }

    String readingState = "";

    if(state == Book.State.Finished) {
      readingState = "Finished";
    }

    final float radius[] = new float[8];
    for(int i = 0; i < 8; i++) { radius[i] = 3; }
    RoundRectShape roundedRect = new RoundRectShape(radius, null, null);
    ShapeDrawable background = new ShapeDrawable(roundedRect);
    background.getPaint().setColor(color);
    mTextReadingState.setBackgroundDrawable(background);
    mTextReadingState.setText(readingState);
  }

  private void popluateClosingRemark(String closingRemark) {
    if(closingRemark != null) {
      mTextClosingRemark.setText(closingRemark);
    } else {
      mTextClosingRemark.setVisibility(View.GONE);
    }
  }

  private void populateSummary() {
    final long secondsSpent = mBook.calculateSecondsSpent();
    final int sessionCount = mBook.getSessions().size();

    if(mBook.getState() == Book.State.Reading) {
      final String summary = String.format("%s / %d sessions", Utils.longCoarseHumanTimeFromSeconds(secondsSpent), sessionCount);
      mTextSummary.setText(summary);
      mSegmentBar.setVisibility(View.GONE);
    } else {
      mTextSummary.setTextColor(getResources().getColor(R.color.text_color_primary));
      final String summary = String.format("Reading for %s over %d sessions.", Utils.longCoarseHumanTimeFromSeconds(secondsSpent), sessionCount);
      mTextSummary.setText(summary);
    }
  }

  private void populateTimeLeft() {
    final int estimatedSecondsLeft = mBook.calculateEstimatedSecondsLeft();
    String timeLeft = String.format("You have about %s left of reading, given you keep the same pace", Utils.longCoarseHumanTimeFromSeconds(estimatedSecondsLeft));

    final String pepTalk = getPepTalk(estimatedSecondsLeft);
    if(pepTalk != null) {
      timeLeft += ".\n\n" + pepTalk;
    }

    mTextTimeLeft.setText(timeLeft);
  }

  /** Generate a short, encouraging, phrase on how long the user has to read. */
  private String getPepTalk(float estimatedSecondsLeft) {
    String pepTalk = null;
    float hoursLeft = estimatedSecondsLeft / (60.0f * 60.0f);
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
    return pepTalk;
  }
}
