package com.readtracker_beta.fragments;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import com.readtracker_beta.activities.ActivityBook;
import com.readtracker_beta.db.LocalHighlight;
import com.readtracker_beta.db.LocalReading;
import com.readtracker_beta.db.LocalSession;
import com.readtracker_beta.support.SessionTimer;

import java.util.ArrayList;

/**
 * Handles the fragments for the book activity
 */
public class BookFragmentAdapter extends FragmentStatePagerAdapter {
  private boolean mBrowseMode;

  private FragmentRead mFragmentReadInstance;

  private LocalReading mLocalReading;
  private ArrayList<LocalSession> mLocalSessions;
  private ArrayList<LocalHighlight> mLocalHighlights;
  private SessionTimer mSessionTimer;

  public BookFragmentAdapter(FragmentManager fm, ActivityBook.LocalReadingBundle bundle) {
    super(fm);
    setBundle(bundle);
  }

  public void setBundle(ActivityBook.LocalReadingBundle bundle) {
    mLocalReading = bundle.localReading;
    mLocalSessions = bundle.localSessions;
    mLocalHighlights = bundle.localHighlights;
  }

  @Override
  public Fragment getItem(int position) {
    if(position == getSessionsPageIndex()) {
      return FragmentSessions.newInstance(mLocalReading, mLocalSessions);
    } else if(position == getReadingPageIndex()) {
      // Keep a reference to the FragmentRead since we want the ability to
      // interrogate for the current session state
      mFragmentReadInstance = (FragmentRead) FragmentRead.newInstance(mLocalReading, mSessionTimer);
      return mFragmentReadInstance;
    } else if(position == getHighlightsPageIndex()) {
      return FragmentHighlight.newInstance(mLocalReading, mLocalHighlights);
    }
    return null;
  }

  @Override
  public int getCount() {
    return mBrowseMode ? 2 : 3;
  }

  @Override public CharSequence getPageTitle(int position) {
    if(position == getSessionsPageIndex()) {
      return "Summary";
    } else if(position == getReadingPageIndex()) {
      return "Read";
    } else if(position == getHighlightsPageIndex()) {
      return "Highlights";
    }
    return "";
  }

  public int getSessionsPageIndex() {
    return 0;
  }

  public int getReadingPageIndex() {
    return mBrowseMode ? -1 : 1;
  }

  public int getHighlightsPageIndex() {
    return mBrowseMode ? 1 : 2;
  }

  public void setBrowserMode(boolean browserMode) {
    mBrowseMode = browserMode;
  }

  /**
   * Get the current reading state
   *
   * @return the current reading state as a value object or null
   */
  public SessionTimer getSessionTimer() {
    if(mFragmentReadInstance != null) {
      return mFragmentReadInstance.getSessionTimer();
    }
    return null;
  }

  /**
   * Sets the current reading state
   *
   * @param sessionTimer reading state value object to extract state from
   */
  public void setReadingState(SessionTimer sessionTimer) {
    mSessionTimer = sessionTimer;
  }
}
