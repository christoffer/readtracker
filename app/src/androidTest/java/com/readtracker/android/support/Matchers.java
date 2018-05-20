package com.readtracker.android.support;

import android.support.test.espresso.matcher.BoundedMatcher;
import android.util.Log;
import android.view.View;
import android.widget.ListView;

import com.readtracker.R;
import com.readtracker.android.db.Book;
import com.readtracker.android.fragments.BookListFragment;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

import static org.hamcrest.core.AllOf.allOf;
import static android.support.test.espresso.matcher.ViewMatchers.isDescendantOfA;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withTagValue;
import static org.hamcrest.CoreMatchers.is;

public class Matchers {
  /**
   * Data matcher for finding books
   */
  public static Matcher<Object> withBookTitle(final String title) {
    return new BoundedMatcher<Object, Book>(Book.class) {
      @Override
      public boolean matchesSafely(Book book) {
        Log.d("FOO", String.format("%s %b", book.getTitle(), book.getTitle().equals(title)));
        return book.getTitle().equals(title);
      }

      @Override
      public void describeTo(Description description) {
        description.appendText(String.format("with title '%s'", title));
      }
    };
  }

  /**
   * Matches a list view inside a BookListFragment with the given layout resource id.
   * <p>
   * This is used to differentiate between the reading list and the finished list.
   */
  public static Matcher<View> withBookListLayout(final int layoutResourceId) {
    return new TypeSafeMatcher<View>(View.class) {
      @Override protected boolean matchesSafely(View view) {
        final Object bookListTag = BookListFragment.getTagNameForItemResourceId(layoutResourceId);
        return allOf(
            isDescendantOfA(withTagValue(is(bookListTag))),
            withId(android.R.id.list)
        ).matches(view);
      }

      @Override public void describeTo(Description description) {
        description.appendText(String.format("with book list layout '%d'", layoutResourceId));
      }
    };
  }

  /**
   * Matches a certain number of items in a list view
   */
  public static Matcher<View> withListItemCount(final int itemCount) {
    return new BoundedMatcher<View, ListView>(ListView.class) {
      @Override public boolean matchesSafely(final ListView listView) {
        return listView.getCount() == itemCount;
      }

      @Override public void describeTo(final Description description) {
        description.appendText(String.format("with list item count %d", itemCount));
      }
    };
  }
}
