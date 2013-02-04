package com.readtracker.fragments;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.util.Log;
import android.view.ViewGroup;
import com.readtracker.R;
import com.readtracker.db.LocalReading;
import com.readtracker.interfaces.LocalReadingInteractionListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Managers a set of local readings and partitions them into two states:
 * finished and active.
 */
public class HomeFragmentAdapter extends FragmentStatePagerAdapter {
  private static final String TAG = HomeFragmentAdapter.class.getName();


  // Total number of pages in the adapter (affects getCount() and the fragment
  // cache array size)
  private static final int NUM_PAGES = 2;

  // Page indexes
  private static final int FRAGMENT_FINISHED = 0;
  private static final int FRAGMENT_ACTIVE = 1;

  // Keep references to current fragments to allow updating them when the
  // list of local readings changes
  private ReadingListFragment[] fragments = new ReadingListFragment[NUM_PAGES];

  // Bucket local readings on state
  private final ArrayList<LocalReading> finishedReadings = new ArrayList<LocalReading>();
  private final ArrayList<LocalReading> activeReadings = new ArrayList<LocalReading>();

  // A mapping between a local reading instance (in any list) and its id
  private HashMap<Integer, LocalReading> mLocalReadingMap = new HashMap<Integer, LocalReading>();

  public HomeFragmentAdapter(FragmentManager fragmentManager, ArrayList<LocalReading> localReadings) {
    super(fragmentManager);
    setLocalReadings(localReadings);
  }

  @Override public int getCount() { return NUM_PAGES; }

  @Override public CharSequence getPageTitle(int position) {
    switch(position) {
      case FRAGMENT_FINISHED:
        return "Finished";
      case FRAGMENT_ACTIVE:
        return "Reading";
      default:
        return "";
    }
  }

  @Override public Fragment getItem(int position) {
    ReadingListFragment fragment = null;

    if(position == FRAGMENT_FINISHED) {
      fragment = ReadingListFragment.newInstance(finishedReadings, R.layout.local_reading_item_finished);
    } else if(position == FRAGMENT_ACTIVE) {
      fragment = ReadingListFragment.newInstance(activeReadings, R.layout.local_reading_item_active);
    }

    // Keep a reference to the active fragment around so we can update it later
    if(fragment != null) {
      fragments[position] = fragment;
      return fragment;
    }

    return null;
  }

  @Override
  public void destroyItem(ViewGroup container, int position, Object object) {
    super.destroyItem(container, position, object);
    fragments[position] = null;
  }

  /**
   * Adds a LocalReading to the correct fragment list.
   *
   * @param localReading Reading to add to one of the lists
   */
  public void add(LocalReading localReading) {
    Log.d(TAG, "Adding: " + localReading.toString());
    if(localReading.isActive()) {
      activeReadings.add(localReading);
    } else {
      finishedReadings.add(localReading);
    }
    refreshFragments();
    mLocalReadingMap.put(localReading.id, localReading);
  }

  /**
   * Removes all instances of a local reading with the given id from all lists.
   *
   * @param localReadingId id of the local reading to remove
   */
  public void removeReadingsWithId(int localReadingId) {
    LocalReading localReading = getLocalReadingById(localReadingId);
    if(localReading == null) {
      Log.d(TAG, "Asked to remove reading with id: " + localReadingId + ", but none found");
      return;
    }

    Log.d(TAG, "Found local reading with id, remove from all lists: " + localReading.toString());
    activeReadings.remove(localReading);
    finishedReadings.remove(localReading);

    mLocalReadingMap.remove(localReading.id);
  }

  /**
   * Inserts a local reading into the list of managed readings.
   * The reading will be placed in a bucket according to it's state.
   * Any previous LocalReadings with the same id will be removed.
   *
   * @param localReading The new or updated local reading
   */
  public void put(LocalReading localReading) {
    Log.i(TAG, "Putting: " + localReading);
    removeReadingsWithId(localReading.id);
    add(localReading);
  }

  /**
   * Removes all local readings from the adapter and updates the fragments.
   */
  public void clear() {
    Log.i(TAG, "Clearing all items");
    activeReadings.clear();
    finishedReadings.clear();
    mLocalReadingMap.clear();
    notifyDataSetChanged();
  }

  /**
   * Sets the list of local readings to display in its child fragments and
   * refreshes the fragments to show the new list.
   *
   * @param localReadings List of local readings to display
   */
  public void setLocalReadings(List<LocalReading> localReadings) {
    clear();
    if(localReadings != null && localReadings.size() > 0) {
      for(LocalReading localReading : localReadings) {
        add(localReading);
      }
    }
    refreshFragments();
  }

  /**
   * Updates all the fragments with their corresponding lists of local readings.
   */
  public void refreshFragments() {
    Log.d(TAG, "Refreshing fragment reading lists");

    refreshFragment(FRAGMENT_ACTIVE, activeReadings);
    refreshFragment(FRAGMENT_FINISHED, finishedReadings);
  }

  /**
   * Updates a specific fragment with a list of local readings.
   *
   * @param fragmentIndex index in the local fragment cache of the fragment to update
   * @param localReadings list of readings to make current
   */
  public void refreshFragment(int fragmentIndex, ArrayList<LocalReading> localReadings) {
    if(fragmentIndex >= 0 && fragmentIndex < fragments.length && fragments[fragmentIndex] != null) {
      fragments[fragmentIndex].setLocalReadings(localReadings);
    }
  }

  /**
   * Gets the default page to show in a view pager
   *
   * @return the default page
   */
  public int getDefaultPage() {
    return FRAGMENT_ACTIVE;
  }

  /**
   * Gets a instance of a managed LocalReading by its id
   *
   * @param localReadingId local reading id to get instance for
   * @return the local reading instance or null if the id was not mapped
   */
  public LocalReading getLocalReadingById(int localReadingId) {
    return mLocalReadingMap.get(localReadingId);
  }
}
