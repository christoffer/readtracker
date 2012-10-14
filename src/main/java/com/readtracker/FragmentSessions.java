package com.readtracker;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
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
  private ListAdapterLocalSession mLocalSessionsAdapter;

  // Closing remark, recommend (should be moved)
  private static ViewGroup mLayoutClosingButtons;
  private static TextView mDividerClosingButtons;
  private static Button mButtonFinishReading;
  private static Button mButtonAbandonReading;

  // List of reading sessions
  private static ListView mListSessions;

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
    bindViews(view);
    bindEvents();

    setupEndButtons();

    if(mLocalReading.hasClosingRemark()) {
      View header = inflateClosingRemark();
      mListSessions.addHeaderView(header, null, false);
    }

    View footer = inflateSummary();
    mListSessions.addFooterView(footer, null, false);

    mLocalSessionsAdapter = new ListAdapterLocalSession(getActivity(), 0, mLocalSessions);
    mListSessions.setAdapter(mLocalSessionsAdapter);

    return view;
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    Log.d(TAG, "Fragment handling activity result");
    if(requestCode == ActivityCodes.CLOSE_BOOK && resultCode == ActivityCodes.RESULT_OK) {
      mLocalReading = (LocalReading) data.getParcelableExtra(IntentKeys.LOCAL_READING);
      setupEndButtons();
      ((ActivityBook) getActivity()).finishWithResult(ActivityCodes.RESULT_OK);
    }
  }

  private void bindViews(View view) {
    mListSessions = (ListView) view.findViewById(R.id.listSessions);

    mDividerClosingButtons = (TextView) view.findViewById(R.id.dividerClosingButtons);
    mButtonFinishReading = (Button) view.findViewById(R.id.buttonFinishReading);
    mButtonAbandonReading = (Button) view.findViewById(R.id.buttonAbandonReading);
    mLayoutClosingButtons = (ViewGroup) view.findViewById(R.id.layoutClosingButtons);
  }

  public void bindEvents() {
    mButtonAbandonReading.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        exitToFinishBook(true);
      }
    });

    mButtonFinishReading.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        exitToFinishBook(false);
      }
    });
  }

  private LayoutInflater getInflater() {
    if(mInflater == null) {
      mInflater = (LayoutInflater) getActivity().getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
    }
    return mInflater;
  }

  private View inflateClosingRemark() {
    final TextView textClosingRemark = (TextView) getInflater().inflate(R.layout._closing_remark, null);
    final String closingRemark = mLocalReading.readmillClosingRemark;
    textClosingRemark.setText(closingRemark);
    return textClosingRemark;
  }

  private View inflateSummary() {
    final ViewGroup layoutSessionSummary = (ViewGroup) getInflater().inflate(R.layout._reading_session_summary, null);

    final TextView textSessionCount = (TextView) layoutSessionSummary.findViewById(R.id.textSessionCount);
    final TextView textTotalTime = (TextView) layoutSessionSummary.findViewById(R.id.textTotalTime);

    final String timesRead = String.format("%d times", mLocalSessions.size());
    textSessionCount.setText(timesRead);

    final String timeSpent = Utils.hoursAndMinutesFromMillis(mLocalReading.timeSpentMillis);
    textTotalTime.setText(timeSpent);

    return layoutSessionSummary;
  }

  private void setupEndButtons() {
    mLayoutClosingButtons.setVisibility(View.GONE);
    mDividerClosingButtons.setVisibility(View.GONE);

    if(mLocalReading.isActive()) {
      mLayoutClosingButtons.setVisibility(View.VISIBLE);
      mDividerClosingButtons.setVisibility(View.VISIBLE);
    }
  }

  private void exitToFinishBook(boolean shouldAbandon) {
    Intent intentClose = new Intent(getActivity(), ActivityClose.class);
    intentClose.putExtra(IntentKeys.LOCAL_READING, mLocalReading);
    intentClose.putExtra(IntentKeys.SHOULD_ABANDON, shouldAbandon);
    startActivityForResult(intentClose, ActivityCodes.CLOSE_BOOK);
  }
}
