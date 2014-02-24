package com.readtracker.android.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;

import com.readtracker.android.activities.BaseActivity;
import com.squareup.otto.Bus;

/**
 * A base fragment that attaches to a BaseActivity.
 */
public class BaseFragment extends Fragment {
  private Bus mBus;

  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    mBus = ((BaseActivity) getActivity()).getBus();
    mBus.register(this);
  }

  @Override
  public void onDetach() {
    super.onDetach();
    mBus.unregister(this);
  }
}
