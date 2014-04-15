package com.readtracker.android.adapters;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.readtracker.android.R;
import com.squareup.picasso.Picasso;

import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;

/** Adapter for displaying book search results. */
public class SearchResultAdapter extends ArrayAdapter<BookItem> {
  protected static final String TAG = SearchResultAdapter.class.getSimpleName();

  /**
   * Cache item to avoid repeated view look-ups
   */
  static class ViewHolder {
    public ViewHolder(View view) {
      ButterKnife.inject(this, view);
    }
    @InjectView(R.id.textTitle) TextView textTitle;
    @InjectView(R.id.tvAuthor) TextView textAuthor;
    @InjectView(R.id.imageCover) ImageView imageCover;
  }

  public SearchResultAdapter(Context context, int resource, int textViewResourceId, List<BookItem> books) {
    super(context, resource, textViewResourceId, books);
  }

  @Override
  public View getView(int position, View convertView, ViewGroup parent) {
    final BookItem item = getItem(position);
    final ViewHolder viewHolder;

    // Inflate the view of it's not yet initialized
    if(convertView == null) {
      convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_item_book, null);

      // Cache the items view look-ups
      viewHolder = new ViewHolder(convertView);

      //noinspection ConstantConditions
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
      if(!TextUtils.isEmpty(item.coverURL)) {
        Picasso.with(getContext()).load(item.coverURL).into(viewHolder.imageCover);
      }
    } else {
      viewHolder.imageCover.setImageResource(0);
      viewHolder.imageCover.setVisibility(View.GONE);
    }

    return convertView;
  }
}
