package com.readtracker.db;

import org.junit.Before;
import org.junit.Test;

import java.util.Date;

import static org.junit.Assert.*;

public class LocalHighlightTest {
  private LocalHighlight localHighlight;

  private static final int FIRST_DAY_OF_2012 = 1325372400;

  @Before
  public void createTestSubject() {
    localHighlight = new LocalHighlight();
  }

  // lastSyncedBefore

  @Test
  public void testLastSyncedBeforeWhenNeverSynced() throws Exception {
    localHighlight.syncedAt = null;
    assertFalse(localHighlight.lastSyncedBefore(new Date()));
  }

  @Test
  public void testLastSyncedBeforeWhenSomethingHasHappened() throws Exception {
    localHighlight.syncedAt = new Date(FIRST_DAY_OF_2012);
    assertTrue(localHighlight.lastSyncedBefore(new Date(FIRST_DAY_OF_2012 + 1)));
  }

  @Test
  public void testLastSyncedBeforeWhenNothingHasHappened() throws Exception {
    localHighlight.syncedAt = new Date(FIRST_DAY_OF_2012);
    assertFalse(localHighlight.lastSyncedBefore(new Date(FIRST_DAY_OF_2012)));
    assertFalse(localHighlight.lastSyncedBefore(new Date(FIRST_DAY_OF_2012 - 1)));
  }

  // isEditedAfter

  @Test
  public void testIsEditedAfterWhenNeverEdited() throws Exception {
    localHighlight.editedAt = null;
    assertFalse(localHighlight.isEditedAfter(new Date(0)));
  }

  @Test
  public void testIsEditedAfterWhenEditedAfter() throws Exception {
    localHighlight.editedAt = new Date(FIRST_DAY_OF_2012);
    assertTrue(localHighlight.isEditedAfter(new Date(FIRST_DAY_OF_2012 - 1)));
  }

  @Test
  public void testIsEditedAfterWhenEditedBefore() throws Exception {
    localHighlight.editedAt = new Date(FIRST_DAY_OF_2012);
    assertFalse(localHighlight.lastSyncedBefore(new Date(FIRST_DAY_OF_2012)));
    assertFalse(localHighlight.lastSyncedBefore(new Date(FIRST_DAY_OF_2012 + 1)));
  }

  @Test
  public void testIsOfflineOnlyWhenOffline() {
    localHighlight.readmillHighlightId = -1;
    assertTrue(localHighlight.isOfflineOnly());
  }

  @Test
  public void testIsOfflineOnlyWhenOnline() {
    localHighlight.readmillHighlightId = 123;
    assertFalse(localHighlight.isOfflineOnly());
  }

  @Test
  public void testHasVisitablePermalinkWhenAbsolute() {
    localHighlight.readmillPermalinkUrl = "http://readmill.com/some/path";
    assertTrue(localHighlight.hasVisitablePermalink());
  }

  @Test
  public void testHasVisitablePermalinkWhenRelative() {
    localHighlight.readmillPermalinkUrl = "/some/path";
    assertFalse(localHighlight.hasVisitablePermalink());
  }

  @Test
  public void testHasVisitablePermalinkWhenNull() {
    localHighlight.readmillPermalinkUrl = null;
    assertFalse(localHighlight.hasVisitablePermalink());
  }
}
