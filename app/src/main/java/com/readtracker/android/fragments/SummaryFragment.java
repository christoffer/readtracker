package com.readtracker.android.fragments;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.readtracker.R;
import com.readtracker.android.activities.BookActivity;
import com.readtracker.android.activities.BookBaseActivity;
import com.readtracker.android.activities.SessionEditFragment;
import com.readtracker.android.adapters.ReadingSessionAdapter;
import com.readtracker.android.custom_views.SessionHeaderView;
import com.readtracker.android.db.Book;
import com.readtracker.android.db.Session;
import com.squareup.otto.Subscribe;

import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;

/**
 * Fragment for showing summary of a book
 */
public class SummaryFragment extends BaseFragment implements SessionEditFragment.OnSessionEditListener {
  private static final String TAG = SummaryFragment.class.getName();

  private Book mBook;

  @InjectView(R.id.sessionList) ListView mSessionList;
  @InjectView(R.id.blank_text) TextView mBlankText;

  private ReadingSessionAdapter mSessionAdapter;
  private SessionHeaderView mSessionListHeader;

  public static Fragment newInstance() {
    return new SummaryFragment();
  }

  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View rootView = inflater.inflate(R.layout.fragment_sessions, container, false);
    ButterKnife.inject(this, rootView);

    mSessionListHeader = new SessionHeaderView(getContext());
    mSessionAdapter = new ReadingSessionAdapter(getContext(), R.layout.session_list_item);

    mSessionList.addHeaderView(mSessionListHeader, null, false);

    mSessionList.setDividerHeight(0);
    mSessionList.setAdapter(mSessionAdapter);
    mSessionList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
      @Override
      public void onItemClick(AdapterView<?> adapterView, View view, int pos, long itemId) {
        final FragmentManager fragMgr = getFragmentManager();
        if(fragMgr != null) {
          int sessionId = (int) itemId;
          SessionEditFragment sessionEditFragment = SessionEditFragment.create(SummaryFragment.this, sessionId);
          sessionEditFragment.show(fragMgr, "edit-session");
        }
      }
    });

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

    mSessionListHeader.populateForBook(mBook);
    final List<Session> sessions = mBook.getSessions();

    if(sessions.size() > 0) {
      mBlankText.setVisibility(View.GONE);
      mSessionList.setVisibility(View.VISIBLE);

      mSessionAdapter.setSessions(sessions);

    } else {
      mBlankText.setVisibility(View.VISIBLE);
      mSessionList.setVisibility(View.GONE);
    }
  }

  @Override public void onSessionUpdated(Session session) {
    Log.d(TAG, "Got update in summary fragment (finally?)");
    BookActivity activity = (BookActivity) getActivity();
    if(activity != null) {
      activity.loadBookFromIntent();
    }
  }

  @Override public void onSessionDeleted(long sessionId) {
    Log.d(TAG, "Got delete in summary fragment (finally?)");
  }
}
