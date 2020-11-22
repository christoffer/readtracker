package com.readtracker.android.fragments;

import android.content.Context;

import com.readtracker.R;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

/**
 * Handles the fragments for the book activity
 */
public class BookFragmentAdapter extends FragmentPagerAdapter {
  private final Context mContext;
  private final Page[] mPages;

  /** Page identifiers. */
  public static enum Page { SUMMARY, READING, QUOTES }

  public BookFragmentAdapter(Context context, FragmentManager fm, Page[] pages) {
    super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
    mContext = context;
    mPages = pages;
  }

  @NotNull @Override
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
