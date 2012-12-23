package com.readtracker.fragments;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import com.readtracker.ApplicationReadTracker;
import com.readtracker.IntentKeys;
import com.readtracker.R;
import com.readtracker.db.LocalReading;
import com.readtracker.interfaces.LocalReadingInteractionListener;
import com.readtracker.list_adapters.ListAdapterLocalReading;

import java.util.ArrayList;

/**
 * Fragment for rendering a list of LocalReadings.
 * <p/>
 * Requires the Activity where it is attached to implement the local reading
 * interaction callback interface.
 *
 * @see LocalReadingInteractionListener
 */
public class FragmentReadingList extends Fragment {
  private static final String TAG = FragmentReadingList.class.getName();

  private final ArrayList<LocalReading> localReadings = new ArrayList<LocalReading>();
  private ListView listReadings;
  private ListAdapterLocalReading listAdapterReadings;
  private int itemLayoutResourceId;
  private LocalReadingInteractionListener interactionListener;

  /**
   * Creates a new instance of the fragment
   *
   * @param localReadings        a reference to the readings to manage
   * @param itemLayoutResourceId resource id of layout to use for rendering readings
   * @return the new instance
   */
  public static FragmentReadingList newInstance(ArrayList<LocalReading> localReadings, int itemLayoutResourceId) {
    FragmentReadingList instance = new FragmentReadingList();
    instance.setLocalReadings(localReadings);
    instance.setItemLayoutResourceId(itemLayoutResourceId);
    return instance;
  }

  @Override public void onAttach(Activity activity) {
    super.onAttach(activity);
    try {
      interactionListener = (LocalReadingInteractionListener) activity;
    } catch(ClassCastException ex) {
      throw new ClassCastException("Parent activity must implement " + LocalReadingInteractionListener.class.getName());
    }
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
    bindViews(view);
    return view;
  }

  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);

    listAdapterReadings = new ListAdapterLocalReading(
        getActivity(),
        itemLayoutResourceId,
        R.id.textTitle,
        ApplicationReadTracker.getDrawableManager(),
        localReadings
    );

    listReadings.setAdapter(listAdapterReadings);
    listReadings.setVisibility(View.VISIBLE);

    // Pass on clicked readings to the potential listener
    listReadings.setOnItemClickListener(new AdapterView.OnItemClickListener() {
      @Override
      public void onItemClick(AdapterView parent, View view, int position, long id) {
        LocalReading clickedReading = (LocalReading) listReadings.getItemAtPosition(position);
        if(interactionListener != null) {
          interactionListener.onLocalReadingClicked(clickedReading);
        }
      }
    });
  }

  private void bindViews(View view) {
    listReadings = (ListView) view.findViewById(R.id.listReadings);
  }

  /**
   * Sets the list of LocalReadings to display
   * <p/>
   * Uses an empty list if set to null.
   *
   * @param localReadings list of local readings to display
   */
  public void setLocalReadings(ArrayList<LocalReading> localReadings) {
    this.localReadings.clear();
    this.localReadings.addAll(localReadings);
    if(listAdapterReadings != null) {
      listAdapterReadings.notifyDataSetChanged();
    }
  }

  /**
   * Sets the layout resource to use for rendering this lists readings
   *
   * @param resourceId id of resource to use
   */
  public void setItemLayoutResourceId(int resourceId) {
    this.itemLayoutResourceId = resourceId;
  }
}
