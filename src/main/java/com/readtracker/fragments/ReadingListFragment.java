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
import com.readtracker.adapters.LocalReadingAdapter;
import com.readtracker.db.LocalReading;
import com.readtracker.interfaces.LocalReadingInteractionListener;

import java.util.*;

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
  private int itemLayoutResourceId;
  private LocalReadingInteractionListener interactionListener;
  private ArrayList<LocalReading> localReadings;

  private Comparator<LocalReading> mLocalReadingComparator = new Comparator<LocalReading>() {
    @Override
    public int compare(LocalReading readingA, LocalReading readingB) {
      // Sort readings freshest to stalest
      return (int) (readingB.lastReadAt - readingA.lastReadAt);
    }
  };

  /**
   * Creates a new instance of the fragment
   *
   * @param itemLayoutResourceId resource id of layout to use for rendering readings
   * @return the new instance
   */
  public static ReadingListFragment newInstance(ArrayList<LocalReading> localReadings, int itemLayoutResourceId) {
    ReadingListFragment instance = new ReadingListFragment();
    instance.setItemLayoutResourceId(itemLayoutResourceId);
    instance.setLocalReadings(localReadings);
    return instance;
  }

  @Override
  public void onCreate(Bundle in) {
    super.onCreate(in);
    if(in != null) {
      ArrayList<LocalReading> frozenReadings = in.getParcelableArrayList(IntentKeys.LOCAL_READINGS);
      itemLayoutResourceId = in.getInt(IntentKeys.RESOURCE_ID);
      setLocalReadings(frozenReadings);
    }
  }

  @Override
  public void onSaveInstanceState(Bundle out) {
    super.onSaveInstanceState(out);
    out.putParcelableArrayList(IntentKeys.LOCAL_READINGS, localReadings);
    out.putInt(IntentKeys.RESOURCE_ID, itemLayoutResourceId);
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.fragment_reading_list, container, false);
    return view;
  }

  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);

    listAdapterReadings = new LocalReadingAdapter(
      getActivity(),
      itemLayoutResourceId,
      R.id.textTitle,
      ApplicationReadTracker.getDrawableManager(),
      localReadings
    );

    setListAdapter(listAdapterReadings);
  }

  @Override public void onListItemClick(ListView listView, View clickedView, int position, long id) {
    LocalReading clickedReading = (LocalReading) listView.getItemAtPosition(position);
    if(interactionListener != null) {
      interactionListener.onLocalReadingClicked(clickedReading);
    }
  }

  /**
   * Sets the list of LocalReadings to display
   * <p/>
   * Uses an empty list if set to null.
   *
   * @param localReadings list of local readings to display
   */
  public void setLocalReadings(ArrayList<LocalReading> localReadings) {
    Log.v(TAG, "Setting list of local readings to list with size: " + (localReadings == null ? "NULL" : localReadings.size()));

    if(this.localReadings == null) {
      this.localReadings = new ArrayList<LocalReading>();
    } else {
      this.localReadings.clear();
    }

    this.localReadings.addAll(localReadings);
    Collections.sort(this.localReadings, mLocalReadingComparator);

    if(listAdapterReadings != null) {
      listAdapterReadings.notifyDataSetChanged();
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

  public void setInteractionListener(HomeFragmentAdapter listener) {
    interactionListener = listener;
  }
}
