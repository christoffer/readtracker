package com.readtracker.android.fragments;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import com.readtracker.android.R;

/**
 * Handles the fragments for the book activity
 */
public class BookFragmentAdapter extends FragmentPagerAdapter {
  private final Context mContext;
  private boolean mBrowseMode;

  public BookFragmentAdapter(Context context, FragmentManager fm) {
    super(fm);
    mContext = context;
  }

  @Override
  public Fragment getItem(int position) {
    if(position == getSessionsPageIndex()) {
      return SummaryFragment.newInstance();
    } else if(position == getReadingPageIndex()) {
      // Keep a reference to the ReadFragment since we want the ability to
      // interrogate for the current session state
      return ReadFragment.newInstance();
    } else if(position == getQuotesPageIndex()) {
      return QuoteFragment.newInstance();
    }
    return null;
  }

  @Override
  public int getCount() {
    return mBrowseMode ? 2 : 3;
  }

  @Override
  public CharSequence getPageTitle(int position) {
    if(position == getSessionsPageIndex()) {
      return mContext.getString(R.string.book_fragment_header_summary);
    } else if(position == getReadingPageIndex()) {
      return mContext.getString(R.string.book_fragment_header_read);
    } else if(position == getQuotesPageIndex()) {
      return mContext.getString(R.string.book_fragment_header_quotes);
    }
    return "";
  }

  public int getSessionsPageIndex() {
    return 0;
  }

  public int getReadingPageIndex() {
    return mBrowseMode ? -1 : 1;
  }

  public int getQuotesPageIndex() {
    return mBrowseMode ? 1 : 2;
  }

  public void setBrowserMode(boolean browserMode) {
    mBrowseMode = browserMode;
  }
}
