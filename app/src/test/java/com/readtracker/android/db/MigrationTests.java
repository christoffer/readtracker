package com.readtracker.android.db;

import android.content.Context;
import android.test.mock.MockContext;

import com.readtracker.android.test_support.DatabaseTestCase;
import com.readtracker.android.test_support.TestUtils;

import junit.framework.Assert;

import java.util.List;

public class MigrationTests extends DatabaseTestCase {

  @Override protected DatabaseHelper createDatabaseHelper() {
    int migrationStartVersion = 11;
    Context context = new MockContext();
    setContext(context);
    Assert.assertNotNull(context);
    return new DatabaseHelper(context, DATABASE_NAME, null, migrationStartVersion);
  }

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
