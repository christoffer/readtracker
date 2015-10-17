package com.readtracker.android.db;

import com.readtracker.android.test_support.TestUtils;

import junit.framework.TestCase;

public class SessionTest extends TestCase {
  public void test_merge() throws Exception {
    Book book = new Book();
    Session original = new Session();

    original.setDurationSeconds(123);
    original.setTimestampMs(123456789L);
    original.setStartPosition(0.25f);
    original.setEndPosition(0.75f);

    Session merged = new Session();
    merged.merge(original);

    assertEquals(123, merged.getDurationSeconds());
    assertEquals(123456789L, merged.getTimestampMs());
    assertEquals(0.25f, merged.getStartPosition());
    assertEquals(0.75f, merged.getEndPosition());
  }

}