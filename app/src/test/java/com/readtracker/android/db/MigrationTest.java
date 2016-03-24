package com.readtracker.android.db;

import com.readtracker.android.test_support.DatabaseTestCase;
import com.readtracker.android.test_support.TestUtils;

import junit.framework.Assert;

import org.junit.Ignore;
import org.junit.Test;

import java.util.List;

public class MigrationTest extends DatabaseTestCase {

  /**
   * Helper function to return a DatabaseHelper entity for managing
   * the db connection and setup for the tests.
   * @return DatabaseHelper
   */
  public DatabaseHelper createDatabaseHelper() {
    int migrationStartVersion = 11;
    Assert.assertNotNull(getContext());
    return new DatabaseHelper(getContext(), DATABASE_NAME, null, migrationStartVersion);
  }

  /**
   * Migrate broken session timestamps from version 3.1
   *
   * Create a session with a timestamp < 1971, that will be corrupted once
   * it's saved in the buggy 3.1 version of OrmLite. It is expected that it
   * will be saved in seconds instead of milliseconds. Then we create another
   * session that we expect to be perfectly fine. We fix the broken data if we find any
   * using the migrateVersion31Sessions function. Then we assert the results.
   */
  @Test
  public void migrationTest_MigrateFrom11to12_BugFix() {
    Book book = TestUtils.buildRandomBook();
    getDatabaseManager().save(book);

    Session brokenSession = new Session();
    brokenSession.setTimestampMs(11158586000L /* Sun, 10 May 1970 03:36:26 GMT, ms */);
    brokenSession.setBook(book);
    getDatabaseManager().save(brokenSession);

    Session probablyOkSession = new Session();
    probablyOkSession.setTimestampMs(894771386000L /* Sun, 10 May 1998 03:36:26 GMT, ms */);
    probablyOkSession.setBook(book);
    getDatabaseManager().save(probablyOkSession);

    DatabaseHelper.migrateVersion31Sessions(getDatabaseManager());

    List<Session> session = getDatabaseManager().getAll(Session.class);
    assertEquals(2, session.size());
    final long THRESHOLD_DATE = 31536000000L /* Fri, 01 Jan 1971 00:00:00 GMT */;
    assertTrue(session.get(0).getTimestampMs() >= THRESHOLD_DATE);
    assertTrue(session.get(1).getTimestampMs() >= THRESHOLD_DATE);
  }
}
