package com.readtracker.android.flows;

import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import com.readtracker.R;
import com.readtracker.android.activities.HomeActivity;
import com.readtracker.android.support.CleanSetupTestBase;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.espresso.Espresso.closeSoftKeyboard;
import static android.support.test.espresso.Espresso.onData;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isChecked;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withHint;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static com.readtracker.android.support.Matchers.withBookListLayout;
import static com.readtracker.android.support.Matchers.withBookTitle;
import static com.readtracker.android.support.Matchers.withListItemCount;

@RunWith(AndroidJUnit4.class)
public class AddBookManuallyFlowTestBase extends CleanSetupTestBase<HomeActivity> {

  @Rule
  public ActivityTestRule mActivityRule = createTestRule(HomeActivity.class);

  @Test
  public void test_add_book_flow_page_tracking() {
    onView(withId(R.id.add_book_menu)).perform(click());
    // NOTE(christoffer) Occasionally the soft keyboard seems to obstruct the button for some reason
    closeSoftKeyboard();
    onView(withId(R.id.manually_add_book_btn)).perform(click());
    onView(withHint(R.string.add_book_title_hint)).perform(typeText("Magnificent Unicorns"));
    onView(withHint(R.string.add_book_author_hint)).perform(typeText("Christoffer Klang"));
    closeSoftKeyboard();
    onView(withText(R.string.add_book_track_using_pages)).check(matches(isChecked()));
    onView(withHint(R.string.add_book_page_count_hint)).perform(typeText("17"));
    closeSoftKeyboard();
    onView(withId(R.id.add_or_save_button)).perform(click());

    // Assert the right book was added
    onData(withBookTitle("Magnificent Unicorns")).inAdapterView(
        withBookListLayout(R.layout.book_list_item_reading)
    ).check(matches(isDisplayed()));

    // Assert the right number of books was added to each category
    onView(withBookListLayout(R.layout.book_list_item_reading)).check(
        matches(withListItemCount(1))
    );
    onView(withBookListLayout(R.layout.book_list_item_finished)).check(
        matches(withListItemCount(0))
    );
  }
}
