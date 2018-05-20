package com.readtracker.android;

import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.ViewAssertion;
import android.support.test.espresso.matcher.ViewMatchers;
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
import static android.support.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static com.readtracker.android.CustomViewAssertions.isNotPresent;

@RunWith(AndroidJUnit4.class)
public class UserOnboardingTest {

  @Rule
  public ActivityTestRule mActivityRule = new ActivityTestRule<HomeActivity>(HomeActivity.class) {
    @Override
    protected void beforeActivityLaunched() {
      TestUtils.clearPreferences(InstrumentationRegistry.getTargetContext());
      super.beforeActivityLaunched();
    }
  };

  private ViewAssertion isVisible() {
    return matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE));
  }

  @Test
  public void user_onboarding_shown_persistently_until_dismissed() {
    onView(withId(R.id.introduction_text)).check(isVisible());

    mActivityRule.finishActivity();
    mActivityRule.launchActivity(null);

    onView(withId(R.id.introduction_text)).check(isVisible());

    onView(withId(R.id.start_using_button)).perform(click());
    onView(withId(R.id.introduction_text)).check(isNotPresent());

    mActivityRule.finishActivity();
    mActivityRule.launchActivity(null);

    onView(withId(R.id.introduction_text)).check(isNotPresent());
  }
}
