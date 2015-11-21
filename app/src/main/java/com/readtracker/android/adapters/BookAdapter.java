package com.readtracker.android.adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.StateListDrawable;
import android.os.Build;
import android.support.v4.content.ContextCompat;
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
import com.readtracker.android.ReadTrackerApp;
import com.readtracker.android.activities.HomeActivity;
import com.readtracker.android.custom_views.SegmentBar;
import com.readtracker.android.db.Book;
import com.readtracker.android.support.BookPalette;
import com.readtracker.android.support.DrawableGenerator;
import com.readtracker.android.support.LoadBookCoverTask;
import com.readtracker.android.support.UiUtils;
import com.readtracker.android.support.Utils;
import com.squareup.otto.Subscribe;

import java.lang.ref.WeakReference;
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

    int backColor = ContextCompat.getColor(mContext, R.color.background);
    int activeColor = ContextCompat.getColor(mContext, R.color.default_button_color_pressed);

    StateListDrawable background = DrawableGenerator.generateListItemBackground(
        activeColor, backColor
    );

    if(Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
      //noinspection deprecation
      convertView.setBackgroundDrawable(background);
    } else {
      convertView.setBackground(background);
    }
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
    private final ReadTrackerApp app;

    @InjectView(R.id.textTitle) TextView titleText;
    @InjectView(R.id.textAuthor) TextView authorText;

    @Optional @InjectView(R.id.progressReadingProgress) SegmentBar segmentedProgressBar;
    @Optional @InjectView(R.id.imageCover) ImageView coverImage;
    @Optional @InjectView(R.id.textClosingRemark) TextView closingRemarkText;
    @Optional @InjectView(R.id.textFinishedAt) TextView finishedAtText;
    private int currentBookHash = 0;

    ViewHolder(View view) {
      ButterKnife.inject(this, view);
      app = (ReadTrackerApp) view.getContext().getApplicationContext();
    }

    void populate(final Book book) {
      currentBookHash = book.hashCode();

      // Required fields
      titleText.setText(book.getTitle());
      authorText.setText(book.getAuthor());

      BookPalette palette = book.getBookPalette();

      // Optional fields
      if(segmentedProgressBar != null) {
        segmentedProgressBar.setVisibility(View.VISIBLE);
        segmentedProgressBar.setStops(Utils.getSessionStops(book.getSessions()));
        segmentedProgressBar.setColor(palette.getMutedColor());
      }

      if(coverImage != null) {
        // Defer image visibility until the image has been loaded
        coverImage.setVisibility(View.INVISIBLE);
      }

      PopulateColoredFieldsCallback callback = new PopulateColoredFieldsCallback(
          book.hashCode(), this
      );
      ((ReadTrackerApp) app.getApplicationContext()).processBookCover(book, callback);

      if(closingRemarkText != null) {
        final TextView closingRemark = closingRemarkText;

        if(!TextUtils.isEmpty(book.getClosingRemark())) {
          closingRemark.setVisibility(View.VISIBLE);
          closingRemark.setText(book.getClosingRemark());
          UiUtils.applyQuoteBackgroundColor(closingRemark, palette.getMutedColor());
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

    void setPalette(BookPalette palette) {
      if(segmentedProgressBar != null) segmentedProgressBar.setColor(palette.getMutedColor());
      if(closingRemarkText != null) {
        UiUtils.applyQuoteBackgroundColor(closingRemarkText, palette.getMutedColor());
      }
    }

    private static class PopulateColoredFieldsCallback implements LoadBookCoverTask.Callback {
      private final int mBookHashOnStart;
      private final WeakReference<ViewHolder> mViewHolder;

      public PopulateColoredFieldsCallback(int bookHashOnStart, ViewHolder viewHolder) {
        mBookHashOnStart = bookHashOnStart;
        mViewHolder = new WeakReference<>(viewHolder);
      }

      @Override public void onBookCoverProcessed(Book book, Bitmap coverImageBitmap) {
        ViewHolder viewHolder = mViewHolder.get();
        if(viewHolder == null || viewHolder.currentBookHash != mBookHashOnStart) {
          // The view has been reused since the task started, let the task started for the new
          // book updated the UI instead.
          return;
        }

        if(viewHolder.coverImage != null) {
          viewHolder.coverImage.setVisibility(View.VISIBLE);
          viewHolder.coverImage.setImageBitmap(coverImageBitmap);
        }
        viewHolder.setPalette(book.getBookPalette());
      }
    }
  }
}
