package com.readtracker.android.db;

import android.test.AndroidTestCase;

import org.junit.Test;

public class SessionTest extends AndroidTestCase {

  /**
   * Create a book entity and set a random session on it. Merges this
   * information onto another book entity and assert the session.
   */
  @Test
  public void sessionTest_MergeInformationFromSessionToSession_ReturnsSessionCopy() {
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