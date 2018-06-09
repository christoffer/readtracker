package com.readtracker.android.fragments;

import android.content.Context;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RoundRectShape;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
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
import com.readtracker.android.support.ColorUtils;
import com.readtracker.android.support.Utils;
import com.squareup.otto.Subscribe;

import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;

import static com.readtracker.android.support.StringUtils.longCoarseHumanTimeFromMillis;
import static com.readtracker.android.support.StringUtils.longCoarseHumanTimeFromSeconds;

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
  @InjectView(R.id.textClosingRemarkContent) TextView mTextClosingRemark;
  @InjectView(R.id.textTimeLeft) TextView mTextTimeLeft;

  public static Fragment newInstance() {
    return new SummaryFragment();
  }

  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    Log.d(TAG, "onCreateView()");
    View rootView = inflater.inflate(R.layout.fragment_sessions, container, false);
    ButterKnife.inject(this, rootView);
    return rootView;
  }

  @Override public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
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

    final int color = ColorUtils.getColorForBook(mBook);
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
      readingState = mTextReadingState.getContext().getString(R.string.general_finished);
    }

    final float radius[] = new float[8];
    for(int i = 0; i < 8; i++) {
      radius[i] = 3;
    }
    RoundRectShape roundedRect = new RoundRectShape(radius, null, null);
    ShapeDrawable background = new ShapeDrawable(roundedRect);
    background.getPaint().setColor(color);

    mTextReadingState.setBackground(background);
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
    final Context context = mTextSummary.getContext();

    if(mBook.getState() == Book.State.Reading) {
      final String sessionDesc = getResources().getQuantityString(R.plurals.plural_session, sessionCount, sessionCount);
      final String timeSpentDesc = longCoarseHumanTimeFromSeconds(secondsSpent, getContext());
      final String summary = getString(R.string.summary_fragment_summary, timeSpentDesc, sessionDesc);
      mTextSummary.setText(summary);
      mSegmentBar.setVisibility(View.GONE);
    } else {
      final int primaryTextColor = ContextCompat.getColor(context, R.color.textColorPrimary);
      mTextSummary.setTextColor(primaryTextColor);
      final String readingTimeString = longCoarseHumanTimeFromSeconds(secondsSpent, getContext());
      final String sessionCountString = getResources().getQuantityString(R.plurals.plural_session, sessionCount, sessionCount);
      final String summary = context.getString(R.string.summary_fragment_summary, readingTimeString, sessionCountString);
      mTextSummary.setText(summary);
    }
  }

  private void populateTimeLeft() {
    // Hide the time left field when no relevant content
    String timeLeft = null;
    int visibility = View.GONE;
    final Context context = mTextTimeLeft.getContext();

    if(!mBook.getState().equals(Book.State.Finished)) {
      final int estimatedSecondsLeft = mBook.calculateEstimatedSecondsLeft();
      if(estimatedSecondsLeft > 0) {
        visibility = View.VISIBLE;
        final String timeLeftString = longCoarseHumanTimeFromSeconds(estimatedSecondsLeft, context);
        timeLeft = context.getString(R.string.sessions_eta, timeLeftString);
        final String pepTalk = getPepTalkString(context, estimatedSecondsLeft);
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
   *
   * TODO(christoffer) translation
   */
  public static String getPepTalkString(Context context, float estimatedSecondsLeft) {
    float hoursLeft = estimatedSecondsLeft / (60.0f * 60.0f);
    if(hoursLeft == 0){
      return null;
    }

    if(hoursLeft < 1) {
      return context.getString(R.string.summary_finish_today);
    }

    if(hoursLeft < 4) {
      // hours per day to finish in 3 days
      final int secondsPerDayForGoal = (int) ((hoursLeft / 3.0f) * 3600);
      final String etaThreeDays = longCoarseHumanTimeFromMillis(secondsPerDayForGoal * 1000, context);
      return context.getString(R.string.summary_eta_for_three_days, etaThreeDays);
    }

    if(hoursLeft < 10) {
      // hours per day to finish in a week
      final int secondsPerDayForGoal = (int) ((hoursLeft / 7.0f) * 3600);
      final String etaOneWeek = longCoarseHumanTimeFromMillis(secondsPerDayForGoal * 1000, context);
      return(context.getString(R.string.summary_eta_for_one_week, etaOneWeek));
    }

    if(hoursLeft < 20) {
      // hours per day to finish in two weeks
      final int secondsPerDayForGoal = (int) ((hoursLeft / 14.0f) * 3600);
      final String etaTwoWeeks = longCoarseHumanTimeFromMillis(secondsPerDayForGoal * 1000, context);
      return context.getString(R.string.summary_eta_for_two_weeks, etaTwoWeeks);
    }

    return null;
  }
}
