package com.readtracker.android.flows;

import android.support.test.InstrumentationRegistry;
import android.support.test.rule.ActivityTestRule;

import com.readtracker.R;
import com.readtracker.android.ReadTrackerApp;
import com.readtracker.android.activities.HomeActivity;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.closeSoftKeyboard;
import static android.support.test.espresso.action.ViewActions.replaceText;
import static android.support.test.espresso.action.ViewActions.scrollTo;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withParent;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;

@Ignore
abstract class FlowTestBase {
  @Rule
  public ActivityTestRule<HomeActivity> mActivityTestRule = new ActivityTestRule<>(HomeActivity.class, false, false);

  @Before
  public void clearOnboardingAndLaunch() {
    ReadTrackerApp app = ((ReadTrackerApp) InstrumentationRegistry.getTargetContext().getApplicationContext());
    if(app.getFirstTimeFlag()) {
      app.setFirstTimeFlag(false);
    }

    mActivityTestRule.launchActivity(null);
  }

  protected void addBookFromHomeActivity(String title, String author, String pageCount) {
    // Click "Add book"
    onView(withId(R.id.add_book_menu)).perform(click());

    // Click "New book..."
    onView(allOf(
        withParent(withId(R.id.flipperBookSearchActions)),
        withId(R.id.buttonNew)
    )).perform(click());

    // Enter title
    onView(withId(R.id.title_edit))
        .perform(scrollTo(), replaceText(title), closeSoftKeyboard());

    // Enter author
    onView(withId(R.id.author_edit))
        .perform(scrollTo(), replaceText(author), closeSoftKeyboard());

    // Add page numbers
    onView(withId(R.id.page_count_edit))
        .perform(scrollTo(), replaceText(pageCount), closeSoftKeyboard());

    // Click "Add book"
    onView(allOf(withId(R.id.add_or_save_button), withText(R.string.add_book_add)))
        .perform(scrollTo(), click());
  }
}
