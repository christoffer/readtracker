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

import com.readtracker.android.R;
import com.readtracker.android.activities.HomeActivity;
import com.readtracker.android.custom_views.SegmentBar;
import com.readtracker.android.db.Book;
import com.readtracker.android.db.Session;
import com.readtracker.android.support.DrawableGenerator;
import com.readtracker.android.support.Utils;
import com.squareup.otto.Subscribe;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/** Adapter for displaying a filtered list of books. */
public class BookAdapter extends BaseAdapter implements ListAdapter {
  private static final String TAG = BookAdapter.class.getName();

  private final Context mContext;

  // Layout to inflate when rendering items
  private int mLayoutResource;

  private static Comparator<String> sBookComparator = new Comparator<String>() {
    @Override public int compare(String keyA, String keyB) {
      return 0;
    }
  };

  // Books in this list
  private List<Book> mBooks = new ArrayList<Book>();

  private Book.State mStateFilter = null;

  public BookAdapter(Context context, int resource, Book.State stateFilter) {
    super();
    mContext = context;
    mLayoutResource = resource;
    mStateFilter = stateFilter;
  }

  @Subscribe public void onBooksLoadedEvent(HomeActivity.BooksLoadedEvent event) {
    Log.d(TAG, "Adapter got books: " + event.getBooks().size());
    for(Book book : event.getBooks()) {
      if(mStateFilter == null || book.getState() == mStateFilter) {
        Log.v(TAG, "Accepting: " + book);
        addOrReplace(book);
      } else {
        Log.v(TAG, "Rejecting: " + book);
      }
    }

    notifyDataSetChanged();
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
    final ViewHolder holder;
    if(convertView == null) {
      convertView = LayoutInflater.from(mContext).inflate(mLayoutResource, null);
      holder = new ViewHolder(convertView);
      convertView.setTag(holder);
    } else {
      holder = (ViewHolder) convertView.getTag();
    }

    Book book = getItem(position);
    holder.populate(book);

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

  private void addOrReplace(Book book) {
    int position = mBooks.indexOf(book);
    if(position < 0) {
      mBooks.add(book);
    } else {
      mBooks.remove(position);
      mBooks.add(position, book);
    }
  }

  static class ViewHolder {
    ViewHolder(View view) {
      titleText = (TextView) view.findViewById(R.id.textTitle);
      authorText = (TextView) view.findViewById(R.id.textAuthor);
      segmentedProgressBar = (SegmentBar) view.findViewById(R.id.progressReadingProgress);
      coverImage = (ImageView) view.findViewById(R.id.imageCover);
      closingRemarkText = (TextView) view.findViewById(R.id.textClosingRemark);
      finishedAtText = (TextView) view.findViewById(R.id.textFinishedAt);
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
        // TODO nicer default cover
        coverImage.setImageResource(android.R.drawable.ic_menu_gallery);
        if(!TextUtils.isEmpty(book.getCoverUrl())) {
          coverImage.setVisibility(View.VISIBLE);
          Picasso.with(coverImage.getContext()).load(book.getCoverUrl()).into(coverImage);
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
          finishedAtText.setText(Utils.humanPastDate(book.getLastOpenedAt()));
          finishedAtText.setVisibility(View.VISIBLE);
        } else {
          finishedAtText.setVisibility(View.GONE);
        }
      }
    }

    // Required fields
    TextView titleText;
    TextView authorText;

    // Optional fields
    SegmentBar segmentedProgressBar;
    ImageView coverImage;
    TextView closingRemarkText;
    TextView finishedAtText;
  }
}
