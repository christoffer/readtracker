package com.readtracker.android.adapters;

import android.content.Context;
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
import com.readtracker.android.support.DrawableGenerator;
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
  private final List<Book> mBooks = new ArrayList<Book>();

  private Book.State mStateFilter = null;

  public BookAdapter(Context context, int resource, Book.State stateFilter) {
    super();
    mContext = context;
    mLayoutResource = resource;
    mStateFilter = stateFilter;
  }

  public void sortBooks() {
    Collections.sort(mBooks, sBookComparator);

    notifyDataSetChanged();
  }

  @Subscribe public void onCatalogueLoadedEvent(HomeActivity.CatalogueLoadedEvent event) {
    Log.d(TAG, "Adapter got books: " + event.getBooks().size());
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
    viewHolder.populate(book);

    int backColor = mContext.getResources().getColor(R.color.background);
    int activeColor = mContext.getResources().getColor(R.color.default_button_color_pressed);

    convertView.setBackgroundDrawable(DrawableGenerator.generateListItemBackground(activeColor, backColor));
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
        Log.v(TAG, String.format("Adding entry: %s", book));
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

    void populate(Book book) {
      // Required fields
      titleText.setText(book.getTitle());
      authorText.setText(book.getAuthor());

      // Optional fields
      if(segmentedProgressBar != null) {
        segmentedProgressBar.setVisibility(View.VISIBLE);
        segmentedProgressBar.setStops(Utils.getSessionStops(book.getSessions()));
        segmentedProgressBar.setColor(Utils.calculateBookColor(book));
      }

      if(coverImage != null) {
        coverImage.setImageResource(R.drawable.bookmark);
        if(!TextUtils.isEmpty(book.getCoverImageUrl())) {
          coverImage.setVisibility(View.VISIBLE);
          Picasso.with(coverImage.getContext()).load(book.getCoverImageUrl()).into(coverImage);
        }
      }

      if(closingRemarkText != null) {
        final TextView closingRemark = closingRemarkText;
        if(!TextUtils.isEmpty(book.getClosingRemark())) {
          closingRemark.setVisibility(View.VISIBLE);
          closingRemark.setText(book.getClosingRemark());
        } else {
          closingRemark.setVisibility(View.GONE);
        }
      }

      if(finishedAtText != null) {
        if(book.getState() == Book.State.Finished) {
          final long now = System.currentTimeMillis();
          finishedAtText.setText(Utils.humanPastTimeFromTimestamp(book.getCurrentPositionTimestampMs(), now));
          finishedAtText.setVisibility(View.VISIBLE);
        } else {
          finishedAtText.setVisibility(View.GONE);
        }
      }
    }

    // Required fields
    @InjectView(R.id.textTitle) TextView titleText;
    @InjectView(R.id.textAuthor) TextView authorText;

    // Optional fields *
    @Optional @InjectView(R.id.progressReadingProgress) SegmentBar segmentedProgressBar;
    @Optional @InjectView(R.id.imageCover) ImageView coverImage;
    @Optional @InjectView(R.id.textClosingRemark) TextView closingRemarkText;
    @Optional @InjectView(R.id.textFinishedAt) TextView finishedAtText;
  }
}
