package com.readtracker.android.flows;

import android.support.test.runner.AndroidJUnit4;
import android.view.View;
import android.widget.TextView;

import com.readtracker.R;
import com.readtracker.android.support.TestUtils;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Random;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.Espresso.pressBack;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.closeSoftKeyboard;
import static android.support.test.espresso.action.ViewActions.replaceText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static com.readtracker.android.support.CustomMatchers.childAtPosition;
import static org.hamcrest.Matchers.allOf;

@RunWith(AndroidJUnit4.class)
public class CoreExperienceFlows extends FlowTestBase {

  /**
   * Verifies that the user can add a book, edit it and that the edits are persisted.
   */
  @Test
  public void testAddAndEditBookFlow() {
    final String initialTitle = TestUtils.generateRandomString(15);
    final String initialAuthor = TestUtils.generateRandomString(15);
    final String initialPageCount = Integer.toString((new Random()).nextInt(100) + 1);

    final String modifiedTitle = TestUtils.generateRandomString(15);
    final String modifiedAuthor = TestUtils.generateRandomString(15);
    final String modifiedPageCount = Integer.toString((new Random()).nextInt(100) + 1);

    addBookFromHomeActivity(initialTitle, initialAuthor, initialPageCount);

    // Assert that the first item is our book
    onView(allOf(childAtPosition(withId(android.R.id.list), 0), isDisplayed()))
        .check(matches(listItemWithTitleAndAuthor(initialTitle, initialAuthor)))
        .perform(click());

    // Open the book settings
    onView(withId(R.id.book_activity_edit_book_menu_item))
        .perform(click());

    // Verify existing values and update them
    onView(withId(R.id.title_edit))
        .check(matches(withText(initialTitle)))
        .perform(replaceText(modifiedTitle), closeSoftKeyboard());

    onView(withId(R.id.author_edit))
        .check(matches(withText(initialAuthor)))
        .perform(replaceText(modifiedAuthor), closeSoftKeyboard());

    onView(withId(R.id.page_count_edit))
        .check(matches(withText(initialPageCount)))
        .perform(replaceText(modifiedPageCount), closeSoftKeyboard());

    // Save and close
    onView(withId(R.id.add_or_save_button)).perform(click());

    // Open the book settings
    onView(withId(R.id.book_activity_edit_book_menu_item))
        .perform(click());

    // Verify updated values in the book
    onView(withId(R.id.title_edit)).check(matches(withText(modifiedTitle)));
    onView(withId(R.id.author_edit)).check(matches(withText(modifiedAuthor)));
    onView(withId(R.id.page_count_edit)).check(matches(withText(modifiedPageCount)));

    // Go back to the reading view
    pressBack();

    // Go back to the book list
    pressBack();

    // Assert that the first item is our book with updated values
    onView(allOf(childAtPosition(withId(android.R.id.list), 0), isDisplayed()))
        .check(matches(listItemWithTitleAndAuthor(modifiedTitle, modifiedAuthor)));
  }

  private Matcher<? super View> listItemWithTitleAndAuthor(final String title, final String author) {
    return new TypeSafeMatcher<View>() {
      @Override
      public void describeTo(Description description) {
        description.appendText("item with title " + title + " and author " + author);
      }

      @Override
      public boolean matchesSafely(View view) {
        TextView titleTextView = (TextView) view.findViewById(R.id.textTitle);
        TextView authorTextView = (TextView) view.findViewById(R.id.textAuthor);
        return (
            titleTextView != null &&
                authorTextView != null &&
                titleTextView.getText().toString().equals(title) &&
                authorTextView.getText().toString().equals(author)
        );
      }
    };
  }
}
