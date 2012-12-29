package com.readtracker_beta.fragments;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import com.readtracker_beta.activities.BookActivity;
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

  private ReadingFragment mReadingFragmentInstance;

  private LocalReading mLocalReading;
  private ArrayList<LocalSession> mLocalSessions;
  private ArrayList<LocalHighlight> mLocalHighlights;
  private SessionTimer mSessionTimer;

  public BookFragmentAdapter(FragmentManager fm, BookActivity.LocalReadingBundle bundle) {
    super(fm);
    setBundle(bundle);
  }

  public void setBundle(BookActivity.LocalReadingBundle bundle) {
    mLocalReading = bundle.localReading;
    mLocalSessions = bundle.localSessions;
    mLocalHighlights = bundle.localHighlights;
  }

  @Override
  public Fragment getItem(int position) {
    if(position == getSessionsPageIndex()) {
      return ReadingSessionsFragment.newInstance(mLocalReading, mLocalSessions);
    } else if(position == getReadingPageIndex()) {
      // Keep a reference to the ReadingFragment since we want the ability to
      // interrogate for the current session state
      mReadingFragmentInstance = (ReadingFragment) ReadingFragment.newInstance(mLocalReading, mSessionTimer);
      return mReadingFragmentInstance;
    } else if(position == getHighlightsPageIndex()) {
      return HighlightFragment.newInstance(mLocalReading, mLocalHighlights);
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
    if(mReadingFragmentInstance != null) {
      return mReadingFragmentInstance.getSessionTimer();
    }
    return new SessionTimer(mLocalReading.id);
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
