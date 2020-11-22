package com.readtracker.android.adapters;

import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;

import com.readtracker.R;
import com.readtracker.android.activities.HomeActivity;
import com.readtracker.android.custom_views.SegmentBar;
import com.readtracker.android.db.Book;
import com.readtracker.android.support.ColorUtils;
import com.readtracker.android.support.StringUtils;
import com.readtracker.android.support.Utils;
import com.readtracker.databinding.BookListItemFinishedBinding;
import com.readtracker.databinding.BookListItemReadingBinding;
import com.squareup.otto.Subscribe;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import androidx.viewbinding.ViewBinding;

/** Adapter for displaying a filtered list of books. */
public class BookAdapter extends BaseAdapter implements ListAdapter {
  private static final String TAG = BookAdapter.class.getName();

  private final Context mContext;

  private static final Comparator<Book> sBookComparator = new Comparator<Book>() {
    @Override public int compare(Book a, Book b) {
      final long keyA = a.getCurrentPositionTimestampMs() == null ? 0 : a.getCurrentPositionTimestampMs();
      final long keyB = b.getCurrentPositionTimestampMs() == null ? 0 : b.getCurrentPositionTimestampMs();

      //noinspection UseCompareMethod
      return keyB < keyA ? -1 : (keyB > keyA ? 1 : 0);
    }
  };

  // Books in this list
  private final List<Book> mBooks = new ArrayList<>();
  private final Book.State mStateFilter;
  private final boolean mUseCompactReadingLists;
  private final boolean mUseFullDates;

  public BookAdapter(Context context, Book.State stateFilter, boolean useCompactReadingLists, boolean useFullDates) {
    super();
    mContext = context;
    mStateFilter = stateFilter;
    mUseCompactReadingLists = useCompactReadingLists;
    mUseFullDates = useFullDates;
  }

  private void sortBooks() {
    Collections.sort(mBooks, sBookComparator);

    notifyDataSetChanged();
  }

  @Subscribe public void onCatalogueLoadedEvent(HomeActivity.CatalogueLoadedEvent event) {
    Log.d(TAG, String.format("Adapter with filter %s got number of books: %d", mStateFilter, event.getBooks().size()));
    setBooks(event.getBooks());
  }

  @Override public int getCount() {
    return mBooks.size();
  }

  @Override public Book getItem(int position) {
    return mBooks.get(position);
  }

  @Override public long getItemId(int position) {
    return getItem(position).getId();
  }

  @Override
  public View getView(int position, View convertView, ViewGroup parent) {
    final ViewHolder viewHolder;
    if(convertView == null) {
      LayoutInflater inflater = LayoutInflater.from(mContext);
      ViewBinding binding;
      if (mStateFilter == Book.State.Finished) {
        binding = BookListItemFinishedBinding.inflate(inflater);
      } else {
        binding = BookListItemReadingBinding.inflate(inflater);
      }
      convertView = binding.getRoot();
      viewHolder = new ViewHolder(convertView, mStateFilter);
      convertView.setTag(viewHolder);
    } else {
      viewHolder = (ViewHolder) convertView.getTag();
    }

    Book book = getItem(position);
    viewHolder.populate(convertView, book, mUseCompactReadingLists, mUseFullDates);

    return convertView;
  }

  @Override public boolean areAllItemsEnabled() {
    return true;
  }

  @Override public boolean isEnabled(int position) {
    return true;
  }

  /** Sets the list of books to display. */
  public void setBooks(List<Book> books) {
    addOrUpdateExistingEntries(books);
    removeDeletedEntries(books);
    sortBooks();
  }

  private void addOrUpdateExistingEntries(List<Book> updatedCatalogue) {
    for(Book book : updatedCatalogue) {
      int position = mBooks.indexOf(book);
      if(mStateFilter == null || book.getState() == mStateFilter) {
        if(position < 0) { // Not in adapter
          mBooks.add(book);
        } else { // Already in adapter
          mBooks.remove(position);
          mBooks.add(position, book);
        }
      } else if(position >= 0) {
        Log.v(TAG, String.format("Removing existing filtered entry: %s", book));
        mBooks.remove(position);
      }
    }
  }

  private void removeDeletedEntries(List<Book> updatedCatalogue) {
    for(Iterator<Book> iterator = mBooks.iterator(); iterator.hasNext(); ) {
      final Book book = iterator.next();
      if(!updatedCatalogue.contains(book)) {
        Log.v(TAG, String.format("Removing entry: %s", book));
        iterator.remove();
      }
    }
  }

  static class ViewHolder {
    // Required fields
    private final TextView titleText;
    private final TextView authorText;
    private final ImageView coverImage;

    // Only for "reading"
    private SegmentBar segmentedProgressBar;

    // Only for "finished"
    private TextView closingRemarkText;
    private TextView finishedAtText;

    ViewHolder(View view, Book.State stateFilter) {
      if (stateFilter == Book.State.Finished) {
        BookListItemFinishedBinding binding;
        binding = BookListItemFinishedBinding.bind(view);
        titleText = binding.textTitle;
        authorText = binding.textAuthor;
        coverImage = binding.imageCover;
        closingRemarkText = binding.textClosingRemark;
        finishedAtText = binding.textFinishedAt;
      } else {
        BookListItemReadingBinding binding;
        binding = BookListItemReadingBinding.bind(view);
        titleText = binding.textTitle;
        authorText = binding.textAuthor;
        segmentedProgressBar = binding.progressReadingProgress;
        coverImage = binding.imageCover;
      }
    }

    void populate(View view, Book book, boolean useCompactReadingLists, boolean useFullDates) {
      // Required fields
      titleText.setText(book.getTitle());
      authorText.setText(book.getAuthor());

      // Optional fields
      if(segmentedProgressBar != null) {
        segmentedProgressBar.setVisibility(View.VISIBLE);
        segmentedProgressBar.setStops(Utils.getSessionStops(book.getSessions()));
        segmentedProgressBar.setColor(ColorUtils.getColorForBook(book));
      }

      if(coverImage != null) {
        coverImage.setImageResource(R.drawable.icon_book);
        if(!TextUtils.isEmpty(book.getCoverImageUrl())) {
          Picasso.with(coverImage.getContext())
              .load(book.getCoverImageUrl())
              .placeholder(R.drawable.icon_book)
              .into(coverImage);
        }
      }

      if(closingRemarkText != null) {
        if(!useCompactReadingLists && !TextUtils.isEmpty(book.getClosingRemark())) {
          closingRemarkText.setVisibility(View.VISIBLE);
          closingRemarkText.setText(book.getClosingRemark());
        } else {
          closingRemarkText.setVisibility(View.GONE);
        }
      }

      if(finishedAtText != null) {
        // Colorize the dot icon next to the finished at label
        final int bookColor = ColorUtils.getColorForBook(book);
        Drawable[] compoundDrawables = finishedAtText.getCompoundDrawables();
        Drawable dot = compoundDrawables.length > 0 ? compoundDrawables[0] : null;
        if (dot != null) {
          dot.mutate().setColorFilter(new PorterDuffColorFilter(bookColor, PorterDuff.Mode.SRC_IN));
        }

        if(book.getState() == Book.State.Finished) {
          final long now = System.currentTimeMillis();
          String finishedOn;
          if (useFullDates) {
            finishedOn = StringUtils.getDateString(book.getCurrentPositionTimestampMs(), view.getContext());
          } else {
            finishedOn = StringUtils.humanPastTimeFromTimestamp(book.getCurrentPositionTimestampMs(), now, view.getContext());
          }

          String finishedOnWithPrefix = view.getContext().getString(R.string.book_list_finished_on, finishedOn);
          finishedAtText.setText(finishedOnWithPrefix);
          finishedAtText.setVisibility(View.VISIBLE);
        } else {
          finishedAtText.setVisibility(View.GONE);
        }
      }
    }
  }
}
