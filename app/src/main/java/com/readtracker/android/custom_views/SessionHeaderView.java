package com.readtracker.android.custom_views;

import android.content.Context;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RoundRectShape;
import androidx.core.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.readtracker.R;
import com.readtracker.android.db.Book;
import com.readtracker.android.db.Session;
import com.readtracker.android.support.ColorUtils;
import com.readtracker.android.support.Utils;

import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;

import static com.readtracker.android.support.StringUtils.longCoarseHumanTimeFromMillis;
import static com.readtracker.android.support.StringUtils.longCoarseHumanTimeFromSeconds;

public class SessionHeaderView extends LinearLayout {
  private static final String TAG = SessionHeaderView.class.getSimpleName();

  @InjectView(R.id.segmentBar) SegmentBar mSegmentBar;
  @InjectView(R.id.textSummary) public TextView mTextSummary;
  @InjectView(R.id.textReadingState) TextView mTextReadingState;
  @InjectView(R.id.textClosingRemarkContent) TextView mTextClosingRemark;
  @InjectView(R.id.textTimeLeft) TextView mTextTimeLeft;

  public SessionHeaderView(Context context) {
    super(context);
    Log.d(TAG, "SessionHeaderView()");
    View rootView = inflate(getContext(), R.layout.session_list_header, this);
    ButterKnife.inject(this, rootView);
  }

  /** Defer populating the fields until both the UI and the data is available. */
  public void populateForBook(Book book) {
    Log.d(TAG, "SessionHeaderView populateForBook()");
    final int color = ColorUtils.getColorForBook(book);
    final List<Session> sessions = book.getSessions();

    mSegmentBar.setColor(color);
    mSegmentBar.setStops(Utils.getSessionStops(sessions));

    if(sessions.size() > 0) {
      populateReadingState(book.getState(), color);
      populateClosingRemark(book.getClosingRemark());
      populateSummary(book);
      populateTimeLeft(book);
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

    final float[] radius = new float[8];
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

  private void populateSummary(Book book) {
    final long secondsSpent = book.calculateSecondsSpent();
    final int sessionCount = book.getSessions().size();
    final Context context = mTextSummary.getContext();

    if(book.getState() == Book.State.Reading) {
      final String sessionDesc = getResources().getQuantityString(R.plurals.plural_session, sessionCount, sessionCount);
      final String timeSpentDesc = longCoarseHumanTimeFromSeconds(secondsSpent, getContext());
      final String summary = context.getString(R.string.summary_fragment_summary, timeSpentDesc, sessionDesc);
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

  private void populateTimeLeft(Book book) {
    // Hide the time left field when no relevant content
    String timeLeft = null;
    int visibility = View.GONE;
    final Context context = mTextTimeLeft.getContext();

    if(!book.getState().equals(Book.State.Finished)) {
      final int estimatedSecondsLeft = book.calculateEstimatedSecondsLeft();
      if(estimatedSecondsLeft > 0) {
        visibility = View.VISIBLE;
        final String etaTime = longCoarseHumanTimeFromSeconds(estimatedSecondsLeft, context);
        final String pepTalk = getPepTalkString(context, estimatedSecondsLeft);
        final String sessionsEtaSentence = context.getString(R.string.sessions_eta, etaTime);
        if(pepTalk == null) {
          timeLeft = sessionsEtaSentence;
        } else {
          // NOTE(christoffer) I assume that inserting two newlines will be the same in all languages
          timeLeft = sessionsEtaSentence + "\n\n" + pepTalk;
        }
      }
    }
    mTextTimeLeft.setVisibility(visibility);
    mTextTimeLeft.setText(timeLeft);
  }

  /** Generate a short, encouraging, phrase on how long the user has to read. */
  public static String getPepTalkString(Context context, float estimatedSecondsLeft) {
    float hoursLeft = estimatedSecondsLeft / (60.0f * 60.0f);
    if(hoursLeft == 0) {
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
      return (context.getString(R.string.summary_eta_for_one_week, etaOneWeek));
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
