package com.readtracker.android.fragments;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import com.readtracker.R;

import java.util.Arrays;

/**
 * Handles the fragments for the book activity
 */
public class BookFragmentAdapter extends FragmentPagerAdapter {
  private final Context mContext;
  private final Page[] mPages;

  /** Page identifiers. */
  public static enum Page { SUMMARY, READING, QUOTES }

  public BookFragmentAdapter(Context context, FragmentManager fm, Page[] pages) {
    super(fm);
    mContext = context;
    mPages = pages;
  }

  @Override
  public Fragment getItem(int position) {
    switch(mPages[position]) {
      case SUMMARY:
        return SummaryFragment.newInstance();
      case READING:
        return ReadFragment.newInstance();
      case QUOTES:
        return QuotesFragment.newInstance();
    }

    return null;
  }

  @Override
  public int getCount() {
    return mPages.length;
  }

  @Override
  public CharSequence getPageTitle(int position) {
    switch(mPages[position]) {
      case SUMMARY:
        return mContext.getString(R.string.book_fragment_header_summary);
      case READING:
        return mContext.getString(R.string.book_fragment_header_read);
      case QUOTES:
        return mContext.getString(R.string.book_fragment_header_quotes);
    }
    return "";
  }

  /** Returns the page index for the Page. */
  public int getPageIndex(Page page) {
    return Arrays.asList(mPages).indexOf(page);
  }
}
