package com.readtracker.android.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.readtracker.R;
import com.readtracker.android.ReadTrackerApp;
import com.readtracker.android.activities.ActivityCodes;
import com.readtracker.android.activities.BookSearchActivity;
import com.readtracker.android.activities.HomeActivity;
import com.readtracker.android.adapters.BookAdapter;
import com.readtracker.android.db.Book;
import com.readtracker.android.support.ReadTrackerDataImportHandler;
import com.squareup.otto.Bus;

/** List fragments for displaying a list of Books. */
public class BookListFragment extends ListFragment {
  private static final String TAG = BookListFragment.class.getName();

  private Bus mBus;

  private enum Argument {ItemResId, BookStateFilter, CompactReadingLists, UseFullDates}

  private BookAdapter mBookAdapter;

  // Which resources to render list items with
  private int mItemLayoutResId;

  // What filter to apply to the local reading list
  private Book.State mBookStateFilter;

  // Whether or not to display compact reading lists
  private boolean mUseCompactReadingLists;

  // Whether or not to use only full dates or human-friendly relative dates
  private boolean mUseFullDates;

  public static BookListFragment newInstance(int itemLayoutResId, Book.State bookStateFilter, boolean useCompactReadingLists, boolean useFullDates) {
    Log.d(TAG, String.format("BookListFragment being created with itemLayoutResId: %d amd bookStateFilter: %s", itemLayoutResId, bookStateFilter));
    Bundle args = new Bundle();
    args.putInt(Argument.ItemResId.toString(), itemLayoutResId);
    args.putString(Argument.BookStateFilter.toString(), bookStateFilter.toString());
    args.putBoolean(Argument.CompactReadingLists.toString(), useCompactReadingLists);
    args.putBoolean(Argument.UseFullDates.toString(), useFullDates);

    BookListFragment fragment = new BookListFragment();
    fragment.setArguments(args);

    return fragment;
  }

  @Override public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    Bundle args = getArguments();
    if (args != null) {
      mItemLayoutResId = args.getInt(Argument.ItemResId.toString());
      mBookStateFilter = Book.State.valueOf(args.getString(Argument.BookStateFilter.toString()));
      mUseCompactReadingLists = args.getBoolean(Argument.CompactReadingLists.toString());
      mUseFullDates = args.getBoolean(Argument.UseFullDates.toString());
    } else {
      Log.e(TAG, "Got null arguments");
    }
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    final View fragmentLayout = inflater.inflate(R.layout.fragment_reading_list, container, false);
    final Button addBookButton = fragmentLayout.findViewById(R.id.button_add_first_book);
    final Button importButton = fragmentLayout.findViewById(R.id.button_import_data);

    // The fragments of the different pages (currently reading, finished) both use the same layout
    // but the add and import button only really makes sense on the currently reading tab.
    // We also want a different text on the finished books page.
    if(mBookStateFilter == Book.State.Finished) {
      if(addBookButton != null) {
        addBookButton.setVisibility(View.INVISIBLE);
      }
      if(importButton != null) {
        importButton.setVisibility(View.INVISIBLE);
      }
      final TextView blankText = fragmentLayout.findViewById(R.id.text_blank_reading_list);
      if(blankText != null) {
        blankText.setText(R.string.reading_list_blank_text_finished_books);
      }
    } else {
      // Hook up the event listeners for the buttons
      if(addBookButton != null) {
        addBookButton.setOnClickListener(new View.OnClickListener() {
          @Override public void onClick(View view) {
            final Intent intent = new Intent(getActivity(), BookSearchActivity.class);
            startActivityForResult(intent, ActivityCodes.REQUEST_ADD_BOOK);
          }
        });
      }
      if(importButton != null) {
        importButton.setOnClickListener(new View.OnClickListener() {
          @Override public void onClick(View view) {
            ReadTrackerDataImportHandler.confirmImport(getActivity());
          }
        });
      }
    }

    return fragmentLayout;
  }

  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);

    HomeActivity homeActivity = (HomeActivity) getActivity();

    if(homeActivity != null) {
      mBookAdapter = new BookAdapter(homeActivity, mItemLayoutResId, mBookStateFilter, mUseCompactReadingLists, mUseFullDates);
      Log.d(TAG, String.format("Created adapter with filter %s", mBookStateFilter));
      mBookAdapter.setBooks(homeActivity.getBooks());
      setListAdapter(mBookAdapter);
      mBus = ReadTrackerApp.from(homeActivity).getBus();
    }
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
