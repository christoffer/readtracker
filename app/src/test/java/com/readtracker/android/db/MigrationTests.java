package com.readtracker.android.db;

import com.readtracker.android.test_support.DatabaseTestCase;
import com.readtracker.android.test_support.TestUtils;

import junit.framework.Assert;

import org.junit.Ignore;
import org.junit.Test;

import java.util.List;

public class MigrationTests extends DatabaseTestCase {

  @Ignore
  public DatabaseHelper createDatabaseHelper() {
    int migrationStartVersion = 11;
    Assert.assertNotNull(getContext());
    return new DatabaseHelper(getContext(), DATABASE_NAME, null, migrationStartVersion);
  }

  @Test
  public void test_migration_12() {
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
