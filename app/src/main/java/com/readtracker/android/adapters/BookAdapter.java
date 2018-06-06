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
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.readtracker.R;
import com.readtracker.android.activities.HomeActivity;
import com.readtracker.android.custom_views.SegmentBar;
import com.readtracker.android.db.Book;
import com.readtracker.android.support.ColorUtils;
import com.readtracker.android.support.StringUtils;
import com.readtracker.android.support.Utils;
import com.squareup.otto.Subscribe;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.Optional;

/** Adapter for displaying a filtered list of books. */
public class BookAdapter extends BaseAdapter implements ListAdapter {
  private static final String TAG = BookAdapter.class.getName();

  private final Context mContext;

  // Layout to inflate when rendering items
  private final int mLayoutResource;

  private static final Comparator<Book> sBookComparator = new Comparator<Book>() {
    @Override public int compare(Book a, Book b) {
      final long keyA = a.getCurrentPositionTimestampMs() == null ? 0 : a.getCurrentPositionTimestampMs();
      final long keyB = b.getCurrentPositionTimestampMs() == null ? 0 : b.getCurrentPositionTimestampMs();

      return keyB < keyA ? -1 : (keyA > keyB ? 1 : 0);
    }
  };

  // Books in this list
  private final List<Book> mBooks = new ArrayList<>();
  private Book.State mStateFilter;
  private final boolean mUseCompactReadingLists;
  private final boolean mUseFullDates;

  public BookAdapter(Context context, int resource, Book.State stateFilter, boolean useCompactReadingLists, boolean useFullDates) {
    super();
    mContext = context;
    mLayoutResource = resource;
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
      convertView = LayoutInflater.from(mContext).inflate(mLayoutResource, null);
      viewHolder = new ViewHolder(convertView);
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
    ViewHolder(View view) {
      ButterKnife.inject(this, view);
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
        coverImage.setImageResource(android.R.drawable.ic_menu_gallery);
        if(!TextUtils.isEmpty(book.getCoverImageUrl())) {
          Picasso.with(coverImage.getContext())
              .load(book.getCoverImageUrl())
              .placeholder(android.R.drawable.ic_menu_gallery)
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
        // Colorize the dot icon next to the finished at label by appl
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

    // Required fields
    @InjectView(R.id.textTitle) TextView titleText;
    @InjectView(R.id.textAuthor) TextView authorText;
    @InjectView(R.id.layout) RelativeLayout layout;

    // Optional fields *
    @Optional @InjectView(R.id.progressReadingProgress) SegmentBar segmentedProgressBar;
    @Optional @InjectView(R.id.imageCover) ImageView coverImage;
    @Optional @InjectView(R.id.textClosingRemark) TextView closingRemarkText;
    @Optional @InjectView(R.id.textFinishedAt) TextView finishedAtText;
  }
}
