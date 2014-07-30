package com.readtracker.android.test_support;

import android.test.AndroidTestCase;

import com.readtracker.android.db.DatabaseHelper;
import com.readtracker.android.db.DatabaseManager;

public class DatabaseTestCase extends AndroidTestCase {
  public final static String DATABASE_NAME = "readtracker-test.db";
  public static int DATABASE_VERSION = DatabaseHelper.DATABASE_VERSION;

  private DatabaseManager mDatabaseManager;
  private DatabaseHelper mDatabaseHelper;

  @Override protected void setUp() throws Exception {
    super.setUp();
    mDatabaseHelper = createDatabaseHelper();
    deleteDatabase();
    mDatabaseManager = new DatabaseManager(mDatabaseHelper);
  }

  @Override protected void tearDown() throws Exception {
    super.tearDown();
    deleteDatabase();
  }

  protected DatabaseHelper createDatabaseHelper() {
    return new DatabaseHelper(getContext(), DATABASE_NAME, null, DATABASE_VERSION);
  }

  protected void deleteDatabase() {
    getContext().deleteDatabase(DATABASE_NAME);
  }

  protected DatabaseManager getDatabaseManager() {
    return mDatabaseManager;
  }

  protected DatabaseHelper getDatabaseHelper() {
    return mDatabaseHelper;
  }
}
