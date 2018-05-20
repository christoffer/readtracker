package com.readtracker.android.activities;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.test.espresso.ViewAssertion;
import android.support.test.espresso.matcher.ViewMatchers;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import com.readtracker.R;
import com.readtracker.android.ReadTrackerApp;
import com.readtracker.android.support.CleanSetupTestBase;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static com.readtracker.android.support.ViewAssertions.isNotPresent;

@RunWith(AndroidJUnit4.class)
public class UserOnboardingTest extends CleanSetupTestBase<HomeActivity> {

  @Rule
  public ActivityTestRule mActivityRule = createTestRule(HomeActivity.class);

  @Override protected void beforeActivityLaunched(Context context) {
    SharedPreferences.Editor preferences = context.getSharedPreferences(
        ReadTrackerApp.PREFERENCES_FILE_NAME, Context.MODE_PRIVATE
    ).edit();
    preferences.clear();
    preferences.commit();
  }

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
