package com.readtracker.android.fragments;

import android.content.res.Resources;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.util.Log;

import com.readtracker.R;
import com.readtracker.android.activities.BaseActivity;
import com.readtracker.android.db.Book;

/**
 * Managers a set of local readings and partitions them into two states:
 * finished and active.
 */
public class HomeFragmentAdapter extends FragmentPagerAdapter {
  private static final String TAG = HomeFragmentAdapter.class.getSimpleName();

  // Number of pages total
  private static final int NUM_PAGES = 2;

  // Page indexes
  private static final int FRAGMENT_FINISHED = 0;
  private static final int FRAGMENT_ACTIVE = 1;
  private final Resources mResources;

  // Flag to use compact mode for the finished reading list
  private boolean mUseCompactFinishList = false;

  public HomeFragmentAdapter(BaseActivity activity, boolean useCompactFinishList) {
    super(activity.getSupportFragmentManager());
    mResources = activity.getResources();
    mUseCompactFinishList = useCompactFinishList;
  }

  @Override public int getCount() { return NUM_PAGES; }

  @Override public CharSequence getPageTitle(int position) {
    switch(position) {
      case FRAGMENT_FINISHED:
        return mResources.getString(R.string.home_fragment_title_finished);
      case FRAGMENT_ACTIVE:
        return mResources.getString(R.string.home_fragment_title_reading);
      default:
        return "";
    }
  }

  @Override public Fragment getItem(int position) {
    if(position == FRAGMENT_FINISHED) {
      if(mUseCompactFinishList) {
        return BookListFragment.newInstance(R.layout.book_list_item_finished_compact, Book.State.Finished);
      } else {
        return BookListFragment.newInstance(R.layout.book_list_item_finished, Book.State.Finished);
      }
    } else if(position == FRAGMENT_ACTIVE) {
      return BookListFragment.newInstance(R.layout.book_list_item_reading, Book.State.Reading);
    }

    Log.w(TAG, "Could not figure out what fragment to return. Returning null.");
    return null;
  }

  /** Returns the default page to show for this adapter. */
  public int getDefaultPage() {
    return FRAGMENT_ACTIVE;
  }
}
