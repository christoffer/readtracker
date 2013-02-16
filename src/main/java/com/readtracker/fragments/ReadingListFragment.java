package com.readtracker.fragments;

import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import com.readtracker.ApplicationReadTracker;
import com.readtracker.IntentKeys;
import com.readtracker.R;
import com.readtracker.activities.HomeActivity;
import com.readtracker.adapters.LocalReadingAdapter;
import com.readtracker.db.LocalReading;
import com.readtracker.interfaces.LocalReadingInteractionListener;

import java.util.ArrayList;

/**
 * Fragment for rendering a list of LocalReadings.
 * <p/>
 * Requires the Activity where it is attached to implement the local reading
 * interaction callback interface.
 *
 * @see LocalReadingInteractionListener
 */
public class ReadingListFragment extends ListFragment {
  private static final String TAG = ReadingListFragment.class.getName();
  private LocalReadingAdapter listAdapterReadings;

  // Which resources to render list items with
  private int itemLayoutResourceId;

  // What filter to apply to the local reading list
  private CharSequence mLocalReadingFilter;

  /**
   * Creates a new instance of the fragment
   *
   * @param itemLayoutResourceId resource id of layout to use for rendering readings
   * @return the new instance
   */
  public static ReadingListFragment newInstance(int itemLayoutResourceId, CharSequence localReadingFilter) {
    ReadingListFragment instance = new ReadingListFragment();
    instance.setItemLayoutResourceId(itemLayoutResourceId);
    instance.setLocalReadingFilter(localReadingFilter);
    return instance;
  }

  @Override
  public void onCreate(Bundle in) {
    Log.v(TAG, "onCreate()");
    super.onCreate(in);
    if(in != null) {
      Log.v(TAG, "thawing state");
      itemLayoutResourceId = in.getInt(IntentKeys.RESOURCE_ID);
    }
  }

  @Override
  public void onSaveInstanceState(Bundle out) {
    Log.v(TAG, "onSaveInstanceState()");
    super.onSaveInstanceState(out);
    out.putInt(IntentKeys.RESOURCE_ID, itemLayoutResourceId);
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    Log.v(TAG, "onCreateView()");
    return inflater.inflate(R.layout.fragment_reading_list, container, false);
  }

  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    Log.v(TAG, "onActivityCreated()");
    super.onActivityCreated(savedInstanceState);

    try {
      //noinspection UnusedDeclaration
      LocalReadingInteractionListener classCassTest = (LocalReadingInteractionListener) getActivity();
    } catch(ClassCastException ignored) {
      throw new RuntimeException("Hosting activity must implement " + LocalReadingInteractionListener.class.getName());
    }

    listAdapterReadings = new LocalReadingAdapter(
      getActivity(),
      itemLayoutResourceId,
      R.id.textTitle,
      ApplicationReadTracker.getDrawableManager(),
      getLocalReadings()
    );

    setListAdapter(listAdapterReadings);
  }

  @Override public void onListItemClick(ListView listView, View clickedView, int position, long id) {
    Log.v(TAG, "onListItemClick()");
    LocalReading clickedReading = (LocalReading) listView.getItemAtPosition(position);
    ((LocalReadingInteractionListener) getActivity()).onLocalReadingClicked(clickedReading);
  }


  /**
   * Makes the fragment reload the list of readings from the parent activity
   */
  public void notifyDataSetChanged() {
    Log.v(TAG, "notifyDataSetChanged()");
    if(listAdapterReadings != null) {
      listAdapterReadings.resetParentList();
      if(mLocalReadingFilter != null) {
        listAdapterReadings.getFilter().filter(mLocalReadingFilter);
      }
    } else {
      Log.d(TAG, "notifyDataSetChanged not yet initialized");
    }
  }

  // TODO Add public void addLocalReading(LocalReading localReading) {}

  /**
   * Sets the layout resource to use for rendering this lists readings
   *
   * @param resourceId id of resource to use
   */
  public void setItemLayoutResourceId(int resourceId) {
    this.itemLayoutResourceId = resourceId;
  }

  /**
   * Sets the filter to use for filtering the list of local readings.
   *
   * Constant values defined in LocalReadingAdapter.
   *
   * @see LocalReadingAdapter
   *
   * @param localReadingFilter filter to use.
   */
  public void setLocalReadingFilter(CharSequence localReadingFilter) {
    mLocalReadingFilter = localReadingFilter;
  }

  private ArrayList<LocalReading> getLocalReadings() {
    Log.v(TAG, "getLocalReadings()");
    final ArrayList<LocalReading> localReadings = ((HomeActivity) getActivity()).getLocalReadings();
    Log.d(TAG, "Returning local readings array object: " + System.identityHashCode(localReadings));
    return localReadings;
  }
}
