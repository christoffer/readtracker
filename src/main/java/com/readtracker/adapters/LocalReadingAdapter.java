package com.readtracker.adapters;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.ImageView;
import android.widget.TextView;
import com.readtracker.R;
import com.readtracker.support.DrawableGenerator;
import com.readtracker.support.Utils;
import com.readtracker.custom_views.SegmentBar;
import com.readtracker.db.LocalReading;
import com.readtracker.support.ReadmillApiHelper;
import com.readtracker.thirdparty.DrawableManager;

import java.util.*;

/**
 * Lists the local reading entity on the home screen with a progress bar,
 * connected state indicator etc.
 * <p/>
 * The list of local readings is managed from the fragment adapter.
 */
public class LocalReadingAdapter extends ArrayAdapter<LocalReading> {
  private static final String TAG = LocalReadingAdapter.class.getName();

  /**
   * Reading filters
   */
  public static final CharSequence FILTER_ACTIVE = "@active";
  public static final CharSequence FILTER_INACTIVE = "@inactive";

  // Inflater for new views
  private static LayoutInflater mInflater;

  // Layout to inflate when rendering items
  private int mLayoutResource;

  // Drawable manager used for getting or downloading covers
  private static DrawableManager mDrawableManager;

  // Reference to the list used in the activity
  private ArrayList<LocalReading> mParentList;

  // List of current items to show
  private ArrayList<LocalReading> mObjects;

  // Lock for synchronizing list access
  private final Object mLock = new Object();

  // Custom filter for filter on local reading attributes
  private LocalReadingFilter mFilter;

  // Used to cache view look-ups
  class ViewHolder {
    // Required
    TextView textTitle;
    TextView textAuthor;

    // Optional
    SegmentBar progressReadingProgress;
    ImageView imageCover;
    TextView textFoundVia;
    TextView textClosingRemark;
    TextView textFinishedAt;
  }

  public LocalReadingAdapter(Context context,
                             int resource,
                             int textViewResourceId,
                             DrawableManager drawableMgr,
                             ArrayList<LocalReading> localReadings) {
    super(context, resource, textViewResourceId, localReadings);

    mParentList = localReadings;
    //noinspection unchecked
    synchronized(mLock) {
      mObjects = new ArrayList<LocalReading>();
      mObjects.addAll(mParentList);
    }

    Log.d(TAG, "Creating adapter with set of " + (localReadings == null ? "NULL" : localReadings.size()) + " local readings");
    mLayoutResource = resource;

    mInflater = (LayoutInflater) context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
    mDrawableManager = drawableMgr;
  }

  /**
   * Resets the items in this list to the list managed by the parent.
   */
  public void resetParentList() {
    synchronized(mLock) {
      mObjects.clear();
      mObjects.addAll(mParentList);
    }
  }

  @Override public Filter getFilter() {
    if(mFilter == null) {
      mFilter = new LocalReadingFilter();
    }
    return mFilter;
  }

  @Override public int getCount() {
    synchronized(mLock) {
      return mObjects == null ? 0 : mObjects.size();
    }
  }

  @Override public LocalReading getItem(int position) {
    LocalReading localReading;
    synchronized(mLock) {
      localReading = mObjects == null ? null : mObjects.get(position);
    }
    return localReading;
  }

  @Override
  public View getView(int position, View convertView, ViewGroup parent) {
    View view = convertView;
    LocalReading localReading = getItem(position);

    ViewHolder holder;
    if(view == null) {
      view = mInflater.inflate(mLayoutResource, null);
      holder = createViewHolder(view);
      view.setTag(holder);
    } else {
      holder = (ViewHolder) view.getTag();
    }

    renderLocalReading(localReading, holder);

    int backColor = getContext().getResources().getColor(R.color.background);
    int activeColor = getContext().getResources().getColor(R.color.default_button_color_pressed);
    view.setBackgroundDrawable(DrawableGenerator.generateListItemBackground(activeColor, backColor));
    return view;
  }

  /**
   * Creates a view holder for the given view
   *
   * @param view View to create view holder for
   * @return the created view holder
   */
  private ViewHolder createViewHolder(View view) {
    ViewHolder viewHolder = new ViewHolder();
    viewHolder.textTitle = (TextView) view.findViewById(R.id.textTitle);
    viewHolder.textAuthor = (TextView) view.findViewById(R.id.textAuthor);
    viewHolder.progressReadingProgress = (SegmentBar) view.findViewById(R.id.progressReadingProgress);
    viewHolder.imageCover = (ImageView) view.findViewById(R.id.imageCover);
    viewHolder.textFoundVia = (TextView) view.findViewById(R.id.textFoundVia);
    viewHolder.textClosingRemark = (TextView) view.findViewById(R.id.textClosingRemark);
    viewHolder.textFinishedAt = (TextView) view.findViewById(R.id.textFinishedAt);
    return viewHolder;
  }

  /**
   * Populate a view holder with available data from a given local reading
   *
   * @param localReading reading to render
   * @param viewHolder   view holder instance to populate
   */
  private void renderLocalReading(LocalReading localReading, ViewHolder viewHolder) {
    // Required fields
    viewHolder.textTitle.setText(localReading.title);
    viewHolder.textAuthor.setText(localReading.author);

    // Optional fields
    if(viewHolder.progressReadingProgress != null) {
      viewHolder.progressReadingProgress.setVisibility(View.VISIBLE);
      float[] progressStops = localReading.getProgressStops();
      if(progressStops == null) {
        progressStops = new float[]{ (float) localReading.progress };
      }
      viewHolder.progressReadingProgress.setStops(progressStops);
      viewHolder.progressReadingProgress.setColor(localReading.getColor());
    }

    if(viewHolder.imageCover != null) {
      // TODO nicer default cover
      viewHolder.imageCover.setImageResource(android.R.drawable.ic_menu_gallery);
      if(localReading.coverURL != null) {
        viewHolder.imageCover.setVisibility(View.VISIBLE);
        mDrawableManager.fetchDrawableOnThread(localReading.coverURL, viewHolder.imageCover);
      }
    }

    if(viewHolder.textFoundVia != null) {
      viewHolder.textFoundVia.setText("Found via: " + localReading.getFoundVia());
    }

    if(viewHolder.textClosingRemark != null) {
      final TextView closingRemark = viewHolder.textClosingRemark;
      if(localReading.hasClosingRemark()) {
        closingRemark.setVisibility(View.VISIBLE);
        closingRemark.setText(localReading.readmillClosingRemark);
      } else {
        closingRemark.setVisibility(View.GONE);
      }
    }

    if(viewHolder.textFinishedAt != null) {
      if(localReading.isClosed() && localReading.locallyClosedAt > 0) {
        final String finishedAt = Utils.humanPastDate(new Date(localReading.locallyClosedAt * 1000));
        final String finishAction = localReading.readmillState == ReadmillApiHelper.ReadingState.ABANDONED ? "Abandoned" : "Finished";
        final String labelText = String.format("%s %s", finishAction, finishedAt);
        viewHolder.textFinishedAt.setText(labelText);
        viewHolder.textFinishedAt.setVisibility(View.VISIBLE);
      } else {
        viewHolder.textFinishedAt.setVisibility(View.GONE);
      }
    }
  }

  class LocalReadingFilter extends Filter {
    @Override protected FilterResults performFiltering(CharSequence q) {
      Log.v(TAG, String.format("performFiltering(%s)", (q == null ? "NULL" : q.toString())));
      FilterResults result = new FilterResults();
      if(q == null || q.length() == 0) {
        synchronized(mLock) {
          result.values = mParentList;
          result.count = mParentList.size();
          return result;
        }
      }

      ArrayList<LocalReading> filteredReadings = new ArrayList<LocalReading>();

      // Minor speed optimization to avoid string comparisons
      final boolean isFilterActive = q.equals(FILTER_ACTIVE);
      final boolean isFilterInactive = q.equals(FILTER_INACTIVE);

      for(LocalReading localReading : mParentList) {
        if(isFilterActive && localReading.isActive()) {
          filteredReadings.add(localReading);
        } else if(isFilterInactive && !localReading.isActive()) {
          filteredReadings.add(localReading);
        }
      }

      Log.i(TAG, String.format("Found %d readings matching %s", filteredReadings.size(), q));

      result.values = filteredReadings;
      result.count = filteredReadings.size();
      return result;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
      Log.v(TAG, "publishResults()");
      notifyDataSetChanged();
      synchronized(mLock) {
        mObjects = new ArrayList<LocalReading>(filterResults.count);
        mObjects.addAll((ArrayList<LocalReading>) filterResults.values);
      }
    }
  }
}
