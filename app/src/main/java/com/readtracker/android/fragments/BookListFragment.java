package com.readtracker.android.fragments;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.readtracker.android.R;
import com.readtracker.android.ReadTrackerApp;
import com.readtracker.android.activities.HomeActivity;
import com.readtracker.android.adapters.BookAdapter;
import com.readtracker.android.db.Book;
import com.readtracker.android.db.LocalReading;
import com.readtracker.android.interfaces.LocalReadingInteractionListener;
import com.squareup.otto.Bus;

import java.util.ArrayList;

/** List fragments for displaying a list of Books. */
public class BookListFragment extends ListFragment {
  private static final String TAG = BookListFragment.class.getName();

  private Bus mBus;

  private static enum Argument { ItemResId, BookStateFilter }

  private BookAdapter mBookAdapter;

  // Which resources to render list items with
  private int mItemLayoutResId;

  // What filter to apply to the local reading list
  private Book.State mBookStateFilter;

  public static BookListFragment newInstance(int itemLayoutResId, Book.State bookStateFilter) {
    Bundle args = new Bundle();
    args.putInt(Argument.ItemResId.toString(), itemLayoutResId);
    args.putString(Argument.BookStateFilter.toString(), bookStateFilter.toString());

    BookListFragment fragment = new BookListFragment();
    fragment.setArguments(args);

    return fragment;
  }

  @Override public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    Bundle args = getArguments();
    mItemLayoutResId = args.getInt(Argument.ItemResId.toString());
    mBookStateFilter = Book.State.valueOf(args.getString(Argument.BookStateFilter.toString()));
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_reading_list, container, false);
  }

  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);

    mBookAdapter = new BookAdapter(getActivity(), mItemLayoutResId, mBookStateFilter);
    setListAdapter(mBookAdapter);

    mBus = ReadTrackerApp.from(getActivity()).getBus();
  }

  @Override public void onResume() {
    super.onResume();
    if(mBus != null) mBus.register(mBookAdapter);
  }

  @Override public void onPause() {
    super.onPause();
    if(mBus != null) mBus.unregister(mBookAdapter);
  }

  @Override
  public void onListItemClick(ListView listView, View clickedView, int position, long id) {
    LocalReading clickedReading = (LocalReading) listView.getItemAtPosition(position);
    ((LocalReadingInteractionListener) getActivity()).onLocalReadingClicked(clickedReading);
  }
}
