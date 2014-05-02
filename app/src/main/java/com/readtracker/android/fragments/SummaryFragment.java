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

import com.readtracker.R;
import com.readtracker.android.activities.BookBaseActivity;
import com.readtracker.android.custom_views.SegmentBar;
import com.readtracker.android.custom_views.SessionView;
import com.readtracker.android.db.Book;
import com.readtracker.android.db.Session;
import com.readtracker.android.support.Utils;
import com.squareup.otto.Subscribe;

import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;

/**
 * Fragment for showing summary of a book
 */
public class SummaryFragment extends BaseFragment {
  private static final String TAG = SummaryFragment.class.getName();

  private Book mBook;

  @InjectView(R.id.sessionView) SessionView mSessionView;
  @InjectView(R.id.segmentBar) SegmentBar mSegmentBar;
  @InjectView(R.id.textSummary) TextView mTextSummary;
  @InjectView(R.id.textReadingState) TextView mTextReadingState;
  @InjectView(R.id.textClosingRemark) TextView mTextClosingRemark;
  @InjectView(R.id.textTimeLeft) TextView mTextTimeLeft;

  public static Fragment newInstance() {
    return new SummaryFragment();
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    Log.d(TAG, "onCreateView()");
    View rootView = inflater.inflate(R.layout.fragment_sessions, container, false);
    ButterKnife.inject(this, rootView);
    return rootView;
  }

  @Override public void onViewCreated(View view, Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    populateFieldsDeferred();
  }

  @Subscribe public void onBookLoadedEvent(BookBaseActivity.BookLoadedEvent event) {
    mBook = event.getBook();
    populateFieldsDeferred();
  }

  /** Defer populating the fields until both the UI and the data is available. */
  private void populateFieldsDeferred() {
    if(mBook == null || getView() == null) {
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
      populateClosingRemark(mBook.getClosingRemark());
      populateSummary();
      populateTimeLeft();
    } else {
      getView().findViewById(R.id.blank_text).setVisibility(View.VISIBLE);
      getView().findViewById(R.id.scrollView).setVisibility(View.GONE);
    }
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
    for(int i = 0; i < 8; i++) {
      radius[i] = 3;
    }
    RoundRectShape roundedRect = new RoundRectShape(radius, null, null);
    ShapeDrawable background = new ShapeDrawable(roundedRect);
    background.getPaint().setColor(color);
    mTextReadingState.setBackgroundDrawable(background);
    mTextReadingState.setText(readingState);
  }

  private void populateClosingRemark(String closingRemark) {
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
    // Hide the time left field when no relevant content
    String timeLeft = null;
    int visibility = View.GONE;

    if(!mBook.getState().equals(Book.State.Finished)) {
      final int estimatedSecondsLeft = mBook.calculateEstimatedSecondsLeft();
      if(estimatedSecondsLeft > 0) {
        visibility = View.VISIBLE;
        timeLeft = String.format("You have about %s left of reading, given you keep the same pace", Utils.longCoarseHumanTimeFromSeconds(estimatedSecondsLeft));
        final String pepTalk = getPepTalk(estimatedSecondsLeft);
        if(pepTalk != null) {
          timeLeft += ".\n\n" + pepTalk;
        }
      }
    }
    mTextTimeLeft.setVisibility(visibility);
    mTextTimeLeft.setText(timeLeft);
  }

  /**
   * Generate a short, encouraging, phrase on how long the user has to read.
   */
  private static String getPepTalk(float estimatedSecondsLeft) {
    String pepTalk = null;
    float hoursLeft = estimatedSecondsLeft / (60.0f * 60.0f);
    if(hoursLeft == 0){
      return null;
    } else if(hoursLeft < 1) {
      pepTalk = "Why not finish it today?";
    } else if(hoursLeft < 4) {
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
