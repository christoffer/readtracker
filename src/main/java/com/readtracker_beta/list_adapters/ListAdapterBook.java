package com.readtracker_beta.list_adapters;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.readtracker_beta.R;
import com.readtracker_beta.thirdparty.DrawableManager;

import java.util.List;

/**
 * Adapter that shows a single book entity without a connect to a reading.
 * <p/>
 * Shows title, author and optionally a cover.
 */
public class ListAdapterBook extends ArrayAdapter<ListItemBook> {
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

  public ListAdapterBook(Context context, int resource, int textViewResourceId, List<ListItemBook> books) {
    super(context, resource, textViewResourceId, books);
    mInflater = (LayoutInflater) context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
  }

  public void cleanUpDrawables() {
    mDrawableManager.recycleAll();
  }

  @Override
  public View getView(int position, View convertView, ViewGroup parent) {
    final ListItemBook item = getItem(position);
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
