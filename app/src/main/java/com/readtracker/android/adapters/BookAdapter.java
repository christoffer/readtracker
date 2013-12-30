package com.readtracker.android.adapters;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.readtracker.android.R;
import com.readtracker.android.thirdparty.DrawableManager;

import java.util.List;

/**
 * Adapter that shows a single book entity without a connect to a reading.
 * <p/>
 * Shows title, author and optionally a cover.
 */
public class BookAdapter extends ArrayAdapter<BookItem> {
  protected static final String TAG = null;
  protected static LayoutInflater mInflater;
  protected static DrawableManager mDrawableManager = new DrawableManager();

  /**
   * Cache item to avoid repeated view look-ups
   */
  private class ViewHolder {
    public TextView textTitle;
    public TextView textAuthor;
    public ImageView imageCover;
  }

  public BookAdapter(Context context, int resource, int textViewResourceId, List<BookItem> books) {
    super(context, resource, textViewResourceId, books);
    mInflater = (LayoutInflater) context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
  }

  public void cleanUpDrawables() {
    mDrawableManager.recycleAll();
  }

  @Override
  public View getView(int position, View convertView, ViewGroup parent) {
    final BookItem item = getItem(position);
    final ViewHolder viewHolder;

    // Inflate the view of it's not yet initialized
    if(convertView == null) {
      convertView = mInflater.inflate(R.layout.list_item_book, null);
      // Cache the items view look-ups
      viewHolder = new ViewHolder();
      viewHolder.textTitle = (TextView) convertView.findViewById(R.id.textTitle);
      viewHolder.textAuthor = (TextView) convertView.findViewById(R.id.tvAuthor);
      viewHolder.imageCover = (ImageView) convertView.findViewById(R.id.imageCover);
      convertView.setTag(viewHolder);
    } else {
      viewHolder = (ViewHolder) convertView.getTag();
    }

    // Assign values from the item to the view
    viewHolder.textTitle.setText(item.title);
    viewHolder.textAuthor.setText(item.author);
    viewHolder.imageCover.setImageResource(android.R.drawable.ic_menu_gallery);

    if(item.coverURL != null) {
      viewHolder.imageCover.setImageResource(android.R.drawable.ic_menu_gallery);
      viewHolder.imageCover.setVisibility(View.VISIBLE);
      mDrawableManager.fetchDrawableOnThread(item.coverURL, viewHolder.imageCover);
    } else {
      viewHolder.imageCover.setImageResource(0);
      viewHolder.imageCover.setVisibility(View.GONE);
    }

    return convertView;
  }
}
