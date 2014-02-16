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
import com.readtracker.android.support.DrawableGenerator;
import com.readtracker.android.support.Utils;
import com.squareup.otto.Subscribe;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

/** Adapter for displaying a filtered list of books. */
public class BookAdapter extends BaseAdapter implements ListAdapter {
  private static final String TAG = BookAdapter.class.getName();

  private final Context mContext;

  // Layout to inflate when rendering items
  private int mLayoutResource;

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
        mBooks.add(book);
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

  static class ViewHolder {
    ViewHolder(View view) {
      textTitle = (TextView) view.findViewById(R.id.textTitle);
      textAuthor = (TextView) view.findViewById(R.id.textAuthor);
      progressReadingProgress = (SegmentBar) view.findViewById(R.id.progressReadingProgress);
      imageCover = (ImageView) view.findViewById(R.id.imageCover);
      closingRemarkText = (TextView) view.findViewById(R.id.textClosingRemark);
      textFinishedAt = (TextView) view.findViewById(R.id.textFinishedAt);
    }

    void populate(Book book) {
      // Required fields
      textTitle.setText(book.getTitle());
      textAuthor.setText(book.getAuthor());

      // Optional fields
//      if(progressReadingProgress != null) {
//        progressReadingProgress.setVisibility(View.VISIBLE);
//        float[] progressStops = book.getProgressStops();
//        if(progressStops == null) {
//          progressStops = new float[]{(float) book.progress};
//        }
//        progressReadingProgress.setStops(progressStops);
//        progressReadingProgress.setColor(book.getColor());
//      }

      if(imageCover != null) {
        // TODO nicer default cover
        imageCover.setImageResource(android.R.drawable.ic_menu_gallery);
        if(!TextUtils.isEmpty(book.getCoverUrl())) {
          imageCover.setVisibility(View.VISIBLE);
          Picasso.with(imageCover.getContext()).load(book.getCoverUrl()).into(imageCover);
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

      if(textFinishedAt != null) {
        if(book.getState() == Book.State.Finished) {
          textFinishedAt.setText(Utils.humanPastDate(book.getLastOpenedAt()));
          textFinishedAt.setVisibility(View.VISIBLE);
        } else {
          textFinishedAt.setVisibility(View.GONE);
        }
      }
    }

    // Required
    TextView textTitle;
    TextView textAuthor;

    // Optional
    SegmentBar progressReadingProgress;
    ImageView imageCover;
    TextView closingRemarkText;
    TextView textFinishedAt;
  }
}
