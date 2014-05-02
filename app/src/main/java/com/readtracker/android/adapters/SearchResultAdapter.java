package com.readtracker.android.adapters;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.readtracker.R;
import com.squareup.picasso.Picasso;

import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;

/** Adapter for displaying book search results. */
public class SearchResultAdapter extends ArrayAdapter<BookItem> {

  /** Cache item to avoid repeated view look-ups */
  static class ViewHolder {
    public ViewHolder(View view) {
      ButterKnife.inject(this, view);
    }
    @InjectView(R.id.textTitle) TextView textTitle;
    @InjectView(R.id.tvAuthor) TextView textAuthor;
    @InjectView(R.id.imageCover) ImageView imageCover;
  }

  public SearchResultAdapter(Context context, List<BookItem> books) {
    super(context, R.layout.list_item_book, R.id.textTitle, books);
  }

  @Override
  public View getView(int position, View convertView, ViewGroup parent) {
    final BookItem item = getItem(position);
    final ViewHolder viewHolder;

    // Inflate the view of it's not yet initialized
    if(convertView == null) {
      convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_item_book, parent, false);

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
    viewHolder.imageCover.setImageResource(R.drawable.bookmark);

    if(item.coverURL != null) {
      viewHolder.imageCover.setImageResource(R.drawable.bookmark);
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
