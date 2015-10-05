package com.readtracker.android.db;

import junit.framework.TestCase;

public class QuoteTest extends TestCase {

  public void test_merge() throws Exception {
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