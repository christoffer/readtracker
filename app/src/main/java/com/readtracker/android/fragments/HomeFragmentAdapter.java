package com.readtracker.android.fragments;

import android.content.res.Resources;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentStatePagerAdapter;
import android.util.Log;

import com.readtracker.R;
import com.readtracker.android.activities.BaseActivity;
import com.readtracker.android.db.Book;

/**
 * Managers a set of local readings and partitions them into two states:
 * finished and active.
 */
public class HomeFragmentAdapter extends FragmentStatePagerAdapter {
  private static final String TAG = HomeFragmentAdapter.class.getSimpleName();

  // Number of pages total
  private static final int NUM_PAGES = 2;

  // Page indexes
  private static final int FRAGMENT_FINISHED_POSITION = 0;
  private static final int FRAGMENT_ACTIVE_POSITION = 1;

  public static final int DEFAULT_POSITION = FRAGMENT_ACTIVE_POSITION;

  private final Resources mResources;

  // Flag to use compact mode for the finished reading list
  private final boolean mUseCompactFinishList;
  // Flag to use full dates instead of human-friendly ones
  private final boolean mUseFullDates;

  public HomeFragmentAdapter(BaseActivity activity, boolean useCompactFinishList, boolean useFullDates) {
    super(activity.getSupportFragmentManager());
    mResources = activity.getResources();
    mUseCompactFinishList = useCompactFinishList;
    mUseFullDates = useFullDates;
  }

  @Override public int getCount() { return NUM_PAGES; }

  @Override public CharSequence getPageTitle(int position) {
    switch(position) {
      case FRAGMENT_FINISHED_POSITION:
        return mResources.getString(R.string.home_fragment_title_finished);
      case FRAGMENT_ACTIVE_POSITION:
        return mResources.getString(R.string.home_fragment_title_reading);
      default:
        return "";
    }
  }

  @Override public Fragment getItem(int position) {
    if(position == FRAGMENT_FINISHED_POSITION) {
        return BookListFragment.newInstance(R.layout.book_list_item_finished, Book.State.Finished, mUseCompactFinishList, mUseFullDates);
    } else if(position == FRAGMENT_ACTIVE_POSITION) {
      Log.d(TAG, "Creating book list fragment with reading views");
      return BookListFragment.newInstance(R.layout.book_list_item_reading, Book.State.Reading, mUseCompactFinishList, mUseFullDates);
    }

    Log.w(TAG, "Could not figure out what fragment to return. Returning null.");
    return null;
  }
}
