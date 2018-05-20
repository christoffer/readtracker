package com.readtracker.android.support;

import android.support.test.espresso.NoMatchingViewException;
import android.support.test.espresso.ViewAssertion;
import android.support.test.espresso.util.HumanReadables;
import android.view.View;

import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;

public class ViewAssertions {
  public static ViewAssertion isNotPresent() {
    return new ViewAssertion() {
      @Override
      public void check(View view, NoMatchingViewException noView) {
        if(view != null && isDisplayed().matches(view)) {
          final String msg = String.format("View is present: %s", HumanReadables.describe(view));
          throw new AssertionError(msg);
        }
      }
    };
  }
}
