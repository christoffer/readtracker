package com.readtracker.android.db;

import android.test.AndroidTestCase;

import com.readtracker.android.TestUtils;

import java.util.List;

public class MigrationTests extends AndroidTestCase {
  private final static String DATABASE_NAME = "readtracker-test.db";

  private DatabaseManager mDatabaseManager;

  @Override protected void setUp() throws Exception {
    super.setUp();
    DatabaseHelper databaseHelper = new DatabaseHelper(getContext(), DATABASE_NAME,
        null, 11);
    getContext().deleteDatabase(DATABASE_NAME);
    mDatabaseManager = new DatabaseManager(databaseHelper);
  }

  public void test_migration_12() {
    Book book = TestUtils.buildBook();
    mDatabaseManager.save(book);

    Session brokenSession = new Session();
    brokenSession.setTimestampMs(11158586000L /* Sun, 10 May 1970 03:36:26 GMT, ms */);
    brokenSession.setBook(book);
    mDatabaseManager.save(brokenSession);

    Session probablyOkSession = new Session();
    probablyOkSession.setTimestampMs(894771386000L /* Sun, 10 May 1998 03:36:26 GMT, ms */);
    probablyOkSession.setBook(book);
    mDatabaseManager.save(probablyOkSession);

    DatabaseHelper.migrateVersion31Sessions(mDatabaseManager);

    List<Session> session = mDatabaseManager.getAll(Session.class);
    assertEquals(2, session.size());
    final long THRESHOLD_DATE = 31536000000L /* Fri, 01 Jan 1971 00:00:00 GMT */;
    assertTrue(session.get(0).getTimestampMs() >= THRESHOLD_DATE);
    assertTrue(session.get(1).getTimestampMs() >= THRESHOLD_DATE);
  }
}
