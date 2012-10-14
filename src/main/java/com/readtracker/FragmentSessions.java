package com.readtracker;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import com.readtracker.db.LocalReading;
import com.readtracker.db.LocalSession;

import java.util.ArrayList;

/**
 * Fragment for showing a reading history of a book
 */
public class FragmentSessions extends Fragment {
  private LocalReading mLocalReading;
  private ArrayList<LocalSession> mLocalSessions;

  private static TextView mTextTimesReadCount;
  private static TextView mTextTimesReadText;
  private static TextView mTextTimeSpent;
  private static TextView mTextTimeSpentFooter;
  private static Button mButtonFinishReading;
  private static Button mButtonAbandonReading;

  private static TextView mDividerClosingButtons;
  private static ViewGroup mLayoutClosingButtons;

  private static TextView mDividerClosingRemark;
  private static TextView mTextClosingRemark;

  private static TextView mDividerSessionList;
  private static ViewGroup mLayoutReadingSessions;

  private static final String TAG = FragmentSessions.class.getName();

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
    bindViews(view);
    return view;
  }

  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    Log.d(TAG, "onActivityCreated()");

    mTextTimesReadCount.setText(String.format("%s", mLocalSessions.size()));
    mTextTimesReadText.setText(String.format("times read in \n%s", mLocalReading.title));
    mTextTimeSpent.setText(Utils.hoursAndMinutesFromMillis(mLocalReading.timeSpentMillis));
    mTextTimeSpentFooter.setText(String.format("and %s seconds spent reading", Utils.getSecondsFromMillis(mLocalReading.timeSpentMillis)));

    bindEvents();
    updateClosingRemarkState();
    generateSessionItems();
  }

  private void bindViews(View view) {
    mTextTimesReadCount = (TextView) view.findViewById(R.id.textTimesReadCount);
    mTextTimesReadText = (TextView) view.findViewById(R.id.textTimesReadText);
    mTextTimeSpent = (TextView) view.findViewById(R.id.textTimeSpent);
    mTextTimeSpentFooter = (TextView) view.findViewById(R.id.textTimeSpentFooter);
    mButtonFinishReading = (Button) view.findViewById(R.id.buttonFinishReading);
    mButtonAbandonReading = (Button) view.findViewById(R.id.buttonAbandonReading);
    mDividerSessionList = (TextView) view.findViewById(R.id.dividerSessionList);
    mLayoutReadingSessions = (ViewGroup) view.findViewById(R.id.layoutReadingSessions);

    mDividerClosingRemark = (TextView) view.findViewById(R.id.dividerClosingRemark);
    mTextClosingRemark = (TextView) view.findViewById(R.id.textClosingRemark);

    mDividerClosingButtons = (TextView) view.findViewById(R.id.dividerClosingButtons);
    mLayoutClosingButtons = (ViewGroup) view.findViewById(R.id.layoutClosingButtons);
  }

  public void bindEvents() {
    mButtonAbandonReading.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        startActivityForFinishBook(true);
      }
    });

    mButtonFinishReading.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        startActivityForFinishBook(false);
      }
    });
  }

  /* Inflate and render the list of session items */
  private void generateSessionItems() {
    if(mLocalSessions.size() == 0) {
      mDividerSessionList.setVisibility(View.GONE);
      mLayoutReadingSessions.setVisibility(View.GONE);
      return;
    } else {
      mDividerSessionList.setVisibility(View.VISIBLE);
      mLayoutReadingSessions.setVisibility(View.VISIBLE);
    }

    LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

    Drawable unsyncedDot = getResources().getDrawable(R.drawable.dot_unsynced);
    for(LocalSession session : mLocalSessions) {
      Log.d(TAG, "Adding list item for session: " + session.toString());
      View child = inflater.inflate(R.layout.session_list_item, null);
      String readingSummary = getReadingSummary(session);
      ((TextView) child.findViewById(R.id.textSessionSummary)).setText(Html.fromHtml(readingSummary));

      // Default is the synced dot
      if(!session.syncedWithReadmill) {
        ((ImageView) child.findViewById(R.id.imageSyncDot)).setImageDrawable(unsyncedDot);
      }

      mLayoutReadingSessions.addView(child);
    }
  }

  private String getReadingSummary(LocalSession session) {
    String readingDuration = Utils.longCoarseHumanTimeFromMillis(session.durationSeconds * 1000);
    String readingTime = Utils.humanTimeOfDay(session.occurredAt);
    String readingDate = Utils.humanPastDate(session.occurredAt);
    return String.format("Reading for <b>%s</b>%s %s %s", readingDuration, approxEndedOnPage(session), readingTime, readingDate);
  }

  private String approxEndedOnPage(LocalSession session) {
    String endedOnpage = ", and ended on page <b>%s</b>";

    if(session.endedOnPage >= 0) {
      return String.format(endedOnpage, session.endedOnPage);
    }

    if(mLocalReading.totalPages > 0) {
      return String.format(endedOnpage, (int) Math.floor(session.progress * mLocalReading.totalPages));
    }

    return "";
  }

  private void updateClosingRemarkState() {
    mLayoutClosingButtons.setVisibility(View.GONE);
    mDividerClosingButtons.setVisibility(View.GONE);
    mTextClosingRemark.setVisibility(View.GONE);
    mDividerClosingRemark.setVisibility(View.GONE);

    if(mLocalReading.isActive()) {
      mLayoutClosingButtons.setVisibility(View.VISIBLE);
      mDividerClosingButtons.setVisibility(View.VISIBLE);
    } else if(mLocalReading.hasClosingRemark()) {
      mTextClosingRemark.setText(mLocalReading.readmillClosingRemark);
      mTextClosingRemark.setVisibility(View.VISIBLE);
      mDividerClosingRemark.setVisibility(View.VISIBLE);
    }
  }

  private void startActivityForFinishBook(boolean shouldAbandon) {
    Intent intentClose = new Intent(getActivity(), ActivityClose.class);
    intentClose.putExtra(IntentKeys.LOCAL_READING, mLocalReading);
    intentClose.putExtra(IntentKeys.SHOULD_ABANDON, shouldAbandon);
    startActivityForResult(intentClose, ActivityCodes.CLOSE_BOOK);
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    Log.d(TAG, "Fragment handling activity result");
    if(requestCode == ActivityCodes.CLOSE_BOOK && resultCode == ActivityCodes.RESULT_OK) {
      mLocalReading = (LocalReading) data.getParcelableExtra(IntentKeys.LOCAL_READING);
      updateClosingRemarkState();
      ((ActivityBook) getActivity()).finishWithResult(ActivityCodes.RESULT_OK);
    }
  }
}
