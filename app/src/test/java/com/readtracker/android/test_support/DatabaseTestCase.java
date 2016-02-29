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
@Ignore
public class DatabaseTestCase extends AndroidTestCase {
  public final static String DATABASE_NAME = "readtracker-test.db";
  public static int DATABASE_VERSION = DatabaseHelper.DATABASE_VERSION;

  private DatabaseManager mDatabaseManager;
  private DatabaseHelper mDatabaseHelper;
  private Context context;

  @Before
  public void setUp() throws Exception {

    context = RuntimeEnvironment.application;
    setContext(context);

    mDatabaseHelper = createDatabaseHelper();
    deleteDatabase();
    mDatabaseManager = new DatabaseManager(getDatabaseHelper());
  }

  @After
  public void tearDown() throws Exception {
    deleteDatabase();
  }

  @Ignore
  protected DatabaseHelper createDatabaseHelper() {
    return new DatabaseHelper(getContext(), DATABASE_NAME, null, DATABASE_VERSION);
  }

  @Ignore
  protected void deleteDatabase() {
    context.deleteDatabase(DATABASE_NAME);
  }

  @Ignore
  protected DatabaseManager getDatabaseManager() {
    return mDatabaseManager;
  }

  @Ignore
  protected DatabaseHelper getDatabaseHelper() {
    return mDatabaseHelper;
  }
}
