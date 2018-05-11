package com.readtracker.android.fragments;

import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.readtracker.R;
import com.readtracker.android.ReadTrackerApp;
import com.readtracker.android.activities.HomeActivity;
import com.readtracker.android.adapters.BookAdapter;
import com.readtracker.android.db.Book;
import com.squareup.otto.Bus;

/** List fragments for displaying a list of Books. */
public class BookListFragment extends ListFragment {
  private static final String TAG = BookListFragment.class.getName();

  private Bus mBus;

  private static enum Argument {ItemResId, BookStateFilter}

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
    // TODO(christoffer) Add button and blurb about importing data from previous install
    return inflater.inflate(R.layout.fragment_reading_list, container, false);
  }

  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);

    HomeActivity homeActivity = (HomeActivity) getActivity();

    mBookAdapter = new BookAdapter(homeActivity, mItemLayoutResId, mBookStateFilter);
    Log.d(TAG, "Created adapter");
    mBookAdapter.setBooks(homeActivity.getBooks());
    setListAdapter(mBookAdapter);

    mBus = ReadTrackerApp.from(homeActivity).getBus();
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
    Book book = (Book) listView.getItemAtPosition(position);
    mBus.post(new BookClickedEvent(book));
  }

  /** Emitted when a book is clicked. */
  public static class BookClickedEvent {
    private final Book mBook;

    public BookClickedEvent(Book book) {
      mBook = book;
    }

    public Book getBook() {
      return mBook;
    }
  }
}
