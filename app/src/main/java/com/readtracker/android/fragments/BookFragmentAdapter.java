package com.readtracker.android.fragments;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import com.readtracker.android.R;
import com.readtracker.android.activities.BookActivity;
import com.readtracker.android.db.Book;
import com.readtracker.android.db.LocalHighlight;
import com.readtracker.android.db.LocalReading;
import com.readtracker.android.db.LocalSession;
import com.readtracker.android.support.SessionTimer;

import java.util.ArrayList;

/** Handles the fragments for the book activity */
public class BookFragmentAdapter extends FragmentStatePagerAdapter {
  private final Context mContext;
  private boolean mBrowseMode;

  public BookFragmentAdapter(Context context, FragmentManager fm) {
    super(fm);
    mContext = context;
  }

  @Override
  public Fragment getItem(int position) {
    if(position == getSessionsPageIndex()) {
      return ReadingSessionsFragment.newInstance();
    } else if(position == getReadingPageIndex()) {
      // Keep a reference to the ReadingFragment since we want the ability to
      // interrogate for the current session state
      return ReadingFragment.newInstance();
    } else if(position == getQuotesPageIndex()) {
      return QuoteFragment.newInstance();
    }
    return null;
  }

  @Override
  public int getCount() {
    return mBrowseMode ? 2 : 3;
  }

  @Override public CharSequence getPageTitle(int position) {
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
