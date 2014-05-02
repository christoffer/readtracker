package com.readtracker.android.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;

import com.readtracker.BuildConfig;
import com.readtracker.android.activities.BaseActivity;
import com.readtracker.android.db.DatabaseManager;
import com.squareup.otto.Bus;

/**
 * A base fragment that attaches to a BaseActivity.
 */
public abstract class BaseFragment extends Fragment {
  private Bus mBus;
  private DatabaseManager mDatabaseManager;

  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    mBus = ((BaseActivity) getActivity()).getBus();
    mDatabaseManager = ((BaseActivity) getActivity()).getDatabaseManager();
  }

  @Override public void onResume() {
    super.onResume();
    if(BuildConfig.DEBUG) {
      Log.v(getClass().getSimpleName(), "Register on bus");
    }

    mBus.register(this);
  }

  @Override public void onPause() {
    super.onPause();
    if(BuildConfig.DEBUG) {
      Log.v(getClass().getSimpleName(), "Unregister from bus");
    }

    mBus.unregister(this);
  }

  protected Bus getBus() {
    return mBus;
  }

  protected DatabaseManager getDatabaseManager() {
    return mDatabaseManager;
  }
}
