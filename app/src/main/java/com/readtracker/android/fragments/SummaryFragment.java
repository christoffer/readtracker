package com.readtracker.android.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.readtracker.android.activities.BookActivity;
import com.readtracker.android.activities.BookBaseActivity;
import com.readtracker.android.activities.FinishBookActivity;
import com.readtracker.android.activities.SessionEditFragment;
import com.readtracker.android.adapters.ReadingSessionAdapter;
import com.readtracker.android.custom_views.SessionHeaderViewHandler;
import com.readtracker.android.db.Book;
import com.readtracker.android.db.Session;
import com.readtracker.databinding.FragmentSessionsBinding;
import com.readtracker.databinding.SessionListHeaderBinding;
import com.squareup.otto.Subscribe;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import static com.readtracker.android.activities.BookActivity.REQUEST_FINISH_BOOK;

/**
 * Fragment for showing summary of a book
 */
public class SummaryFragment extends BaseFragment implements SessionEditFragment.OnSessionEditListener {
  private static final String TAG = SummaryFragment.class.getName();

  private Book mBook;

  private ListView mSessionList;
  private TextView mBlankText;

  private ReadingSessionAdapter mSessionAdapter;
  private SessionHeaderViewHandler mSessionHeaderViewHandler;

  public static Fragment newInstance() {
    return new SummaryFragment();
  }

  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    @NonNull FragmentSessionsBinding binding = FragmentSessionsBinding.inflate(inflater, container, false);
    mSessionList = binding.sessionList;
    mBlankText = binding.blankText;

    @NonNull SessionListHeaderBinding headerBinding = SessionListHeaderBinding.inflate(inflater, container, false);
    mSessionHeaderViewHandler = new SessionHeaderViewHandler(headerBinding, new SessionHeaderViewHandler.OnFinishBookListener() {
      @Override public void onFinishBook(final int bookId) {
        Log.d(TAG, "onFinishBook() called, showing finish book screen");
        Intent intent = new Intent(getActivity(), FinishBookActivity.class);
        intent.putExtra(BookBaseActivity.KEY_BOOK_ID, bookId);
        getActivity().startActivityForResult(intent, REQUEST_FINISH_BOOK);
      }
    });
    mSessionList.addHeaderView(headerBinding.getRoot(), null, false);

    mSessionAdapter = new ReadingSessionAdapter(getContext());

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

    return binding.getRoot();
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

    mSessionHeaderViewHandler.populateForBook(mBook);
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
    Log.d(TAG, "Got update in summary fragment, reloading book");
    BookActivity activity = (BookActivity) getActivity();
    if(activity != null) {
      activity.loadBookFromIntent();
    }
  }

  @Override public void onSessionDeleted(long sessionId) {
    Log.d(TAG, "Got delete in summary fragment, reloading book");
    BookActivity activity = (BookActivity) getActivity();
    if(activity != null) {
      activity.loadBookFromIntent();
    }
  }
}
