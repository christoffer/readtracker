package com.readtracker.android.test_support;

import android.content.Context;
import android.test.AndroidTestCase;

import com.readtracker.android.db.DatabaseHelper;
import com.readtracker.android.db.DatabaseManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(manifest=Config.NONE)
abstract public class DatabaseTestCase extends AndroidTestCase {
  public final static String DATABASE_NAME = "readtracker-test.db";
  public static int DATABASE_VERSION = DatabaseHelper.DATABASE_VERSION;

  private DatabaseManager mDatabaseManager;
  private DatabaseHelper mDatabaseHelper;
  private Context context;

  /**
   * Set the context (using RoboElectric), clean up the database and set the database manager.
   */
  @Before
  public void setUp() {

    context = RuntimeEnvironment.application;
    setContext(context);

    mDatabaseHelper = createDatabaseHelper();
    deleteDatabase();
    mDatabaseManager = new DatabaseManager(getDatabaseHelper());
  }

  /**
   * Delete the database
   */
  @After
  public void tearDown() {
    deleteDatabase();
  }

  protected DatabaseHelper createDatabaseHelper() {
    return new DatabaseHelper(getContext(), DATABASE_NAME, null, DATABASE_VERSION);
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
