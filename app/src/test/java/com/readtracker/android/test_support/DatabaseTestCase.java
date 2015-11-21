package com.readtracker.android.test_support;

import android.content.Context;
import android.test.AndroidTestCase;
import android.test.mock.MockContext;

import com.readtracker.android.db.DatabaseHelper;
import com.readtracker.android.db.DatabaseManager;

import junit.framework.Assert;

public class DatabaseTestCase extends AndroidTestCase {
  public final static String DATABASE_NAME = "readtracker-test.db";
  public static int DATABASE_VERSION = DatabaseHelper.DATABASE_VERSION;

  private DatabaseManager mDatabaseManager;
  private DatabaseHelper mDatabaseHelper;
  Context context;


  @Override protected void setUp() throws Exception {
    super.setUp();
    context = new MockContext();
    setContext(context);
    Assert.assertNotNull(context);

    mDatabaseHelper = createDatabaseHelper();
    deleteDatabase();
    mDatabaseManager = new DatabaseManager(mDatabaseHelper);
  }

  @Override protected void tearDown() throws Exception {
    super.tearDown();
    deleteDatabase();
  }

  protected DatabaseHelper createDatabaseHelper() {
    return new DatabaseHelper(context, DATABASE_NAME, null, DATABASE_VERSION);
  }

  protected void deleteDatabase() {
    context.deleteDatabase(DATABASE_NAME);
  }

  protected DatabaseManager getDatabaseManager() {
    return mDatabaseManager;
  }

  protected DatabaseHelper getDatabaseHelper() {
    return mDatabaseHelper;
  }
}
