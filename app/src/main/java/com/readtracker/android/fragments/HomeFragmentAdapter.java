package com.readtracker.android.fragments;

import android.content.res.Resources;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentStatePagerAdapter;
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
  private boolean mUseCompactFinishList = false;

  public HomeFragmentAdapter(BaseActivity activity, boolean useCompactFinishList) {
    super(activity.getSupportFragmentManager());
    mResources = activity.getResources();
    mUseCompactFinishList = useCompactFinishList;
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
      if(mUseCompactFinishList) {
        Log.d(TAG, "Creating book list fragment with compact finish views");
        return BookListFragment.newInstance(R.layout.book_list_item_finished_compact, Book.State.Finished);
      } else {
        Log.d(TAG, "Creating book list fragment with expanded finish views");
        return BookListFragment.newInstance(R.layout.book_list_item_finished, Book.State.Finished);
      }
    } else if(position == FRAGMENT_ACTIVE_POSITION) {
      Log.d(TAG, "Creating book list fragment with reading views");
      return BookListFragment.newInstance(R.layout.book_list_item_reading, Book.State.Reading);
    }

    Log.w(TAG, "Could not figure out what fragment to return. Returning null.");
    return null;
  }
}
