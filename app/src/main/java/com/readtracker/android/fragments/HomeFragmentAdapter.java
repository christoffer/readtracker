package com.readtracker.android.fragments;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.util.Log;
import android.view.ViewGroup;

import com.readtracker.android.R;
import com.readtracker.android.adapters.LocalReadingAdapter;

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

  private boolean mCompactMode = false;

  public HomeFragmentAdapter(FragmentManager fragmentManager, boolean compactMode) {
    super(fragmentManager);
    mCompactMode = compactMode;
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
      if(mCompactMode) {
        fragment = ReadingListFragment.newInstance(R.layout.local_reading_item_finished_compact, LocalReadingAdapter.FILTER_INACTIVE);
      } else {
        fragment = ReadingListFragment.newInstance(R.layout.local_reading_item_finished, LocalReadingAdapter.FILTER_INACTIVE);
      }
    } else if(position == FRAGMENT_ACTIVE) {
      fragment = ReadingListFragment.newInstance(R.layout.local_reading_item_active, LocalReadingAdapter.FILTER_ACTIVE);
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

  @Override
  public void notifyDataSetChanged() {
    Log.v(TAG, "onNotifyDataSetChanged()");
    super.notifyDataSetChanged();

    // Tell all children that there's new data in the parent
    for(int i = 0; i < NUM_PAGES; i++) {
      if(fragments[i] != null) {
        fragments[i].notifyDataSetChanged();
      }
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
}
