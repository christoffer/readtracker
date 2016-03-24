package com.readtracker.android;

import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.matcher.ViewMatchers;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;

import com.readtracker.R;
import com.readtracker.android.activities.HomeActivity;
import com.readtracker.android.util.CustomMatcher;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static android.support.test.espresso.matcher.ViewMatchers.withId;

/**
 * Created by anthonymonori on 05/10/15.
 * <a href="http://d.android.com/tools/testing/testing_android.html">Testing Fundamentals</a>
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class HomeActivityInstrumentationTest {

  @Rule
  public ActivityTestRule<HomeActivity> mActivityRule =
      new ActivityTestRule<HomeActivity>(HomeActivity.class) {
        @Override
        protected void beforeActivityLaunched() {
          ReadTrackerApp.from(InstrumentationRegistry.getTargetContext()).getPreferences().edit().putBoolean(ReadTrackerApp.KEY_FIRST_TIME, true).commit();
          super.beforeActivityLaunched();
        }
      };

  @Test
  public void findIntroductionViewAndCheckAssertion() {
    // Find Introduction View and check the text contents
    onView(withId(R.id.introduction_text)).check(matches(CustomMatcher.withHtmlText(R.string.introduction_text)));

    // Check if View still exists
    onView(withId(R.id.introduction_layout)).check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));

    // Check for 'Start using' button, click on it, and check for View again
    onView(withId(R.id.start_using_button)).check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE))).perform(click());
    onView(withId(R.id.introduction_layout)).check(matches(withEffectiveVisibility(ViewMatchers.Visibility.GONE)));
  }



}
