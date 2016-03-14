package com.readtracker.android.db;

import android.test.AndroidTestCase;

import org.junit.Test;

public class QuoteTest extends AndroidTestCase {

  /**
   * Create a book entity and set a quote on it. Merge this information
   * into another book entity and assert the information.
   */
  @Test
  public void quoteTest_MergeInformationFromBookToBook_ReturnsBookCopy() {
    Book book = new Book();
    Quote original = new Quote();

    original.setContent("I have content");
    original.setAddTimestampMs(123456789L);
    original.setPosition(0.45f);
    original.setBook(book);

    Quote merged = new Quote();
    merged.merge(original);

    assertEquals("I have content", merged.getContent());
    assertEquals(Long.valueOf(123456789L), merged.getAddTimestampMs());
    assertEquals(0.45f, merged.getPosition());
    assertSame(book, merged.getBook());
  }
}