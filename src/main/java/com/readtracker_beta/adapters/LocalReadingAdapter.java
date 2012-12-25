package com.readtracker_beta.adapters;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import com.readtracker_beta.R;
import com.readtracker_beta.support.Utils;
import com.readtracker_beta.custom_views.SegmentBar;
import com.readtracker_beta.db.LocalReading;
import com.readtracker_beta.support.ReadmillApiHelper;
import com.readtracker_beta.thirdparty.DrawableManager;

import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import static com.readtracker_beta.support.ReadmillApiHelper.ReadingState.READING;

/**
 * Lists the local reading entity on the home screen with a progress bar,
 * connected state indicator etc.
 */
public class LocalReadingAdapter extends ArrayAdapter<LocalReading> {
  // Inflater for new views
  private static LayoutInflater mInflater;

  // Layout to inflate when rendering items
  private int mLayoutResource;

  // Drawable manager used for getting or downloading covers
  private static DrawableManager mDrawableManager;

  // Maps items by their Readmill reading id for fast look-ups
  private static HashMap<Integer, LocalReading> mIdMap = new HashMap<Integer, LocalReading>();

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
                             List<LocalReading> localReadings) {
    super(context, resource, textViewResourceId, localReadings);
    mLayoutResource = resource;
    for(LocalReading localReading : localReadings) {
      mIdMap.put(localReading.id, localReading);
    }
    mInflater = (LayoutInflater) context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
    mDrawableManager = drawableMgr;
  }

  /**
   * Compare two LocalReading:s by their state or freshness.
   * <p/>
   * A is bigger than B if A is reading and B is not.
   * B is bigger than A if B is reading and A is not.
   * <p/>
   * Otherwise they are compared by when they were last read.
   */
  private Comparator<LocalReading> mLocalReadingComparator = new Comparator<LocalReading>() {
    @Override
    public int compare(LocalReading rdA, LocalReading rdB) {
      if(rdA.readmillState == READING && rdB.readmillState != READING)
        return -1;
      if(rdB.readmillState == READING && rdA.readmillState != READING)
        return 1;
      return (int) (rdB.lastReadAt - rdA.lastReadAt); // newest to oldest
    }
  };

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
    return view;
  }

  @Override
  public void add(LocalReading localReading) {
    super.add(localReading);
    mIdMap.put(localReading.id, localReading);
    sort(mLocalReadingComparator);
  }

  @Override
  public void insert(LocalReading localReading, int index) {
    add(localReading);
  }

  @Override
  public void clear() {
    super.clear();
    mIdMap.clear();
  }

  @Override
  public void remove(LocalReading localReading) {
    super.remove(localReading);
    mIdMap.remove(localReading.id);
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
        progressStops = new float[]{(float) localReading.progress};
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
}
