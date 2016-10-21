package com.readtracker.android;

import android.support.test.filters.LargeTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import com.readtracker.R;
import com.readtracker.android.activities.HomeActivity;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.Matchers.not;

// NOTE This must be run on a fresh install, as the introduction screen only comes up once
@RunWith(AndroidJUnit4.class)
@LargeTest
public class OnboardingTests {

  @Rule
  public ActivityTestRule<HomeActivity> mActivityRule = new ActivityTestRule<>(HomeActivity.class);

  @Test
  public void showsIntroduction() {
    onView(withId(R.id.introduction_text)).check(matches(isDisplayed()));

    // Check for 'Start using' button, click on it, and check for View again
    onView(withId(R.id.start_using_button)).perform(click());
    onView(withId(R.id.introduction_layout)).check(matches(not(isDisplayed())));
  }
}
