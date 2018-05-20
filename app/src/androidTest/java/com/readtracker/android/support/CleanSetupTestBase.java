package com.readtracker.android.support;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.support.test.InstrumentationRegistry;
import android.support.test.rule.ActivityTestRule;

import com.readtracker.android.ReadTrackerApp;
import com.readtracker.android.db.DatabaseHelper;

public abstract class CleanSetupTestBase<T extends Activity> {
  protected ActivityTestRule createTestRule(Class<T> cls) {
    return new ActivityTestRule<T>(cls) {
      @Override
      protected void beforeActivityLaunched() {
        final Context context = InstrumentationRegistry.getTargetContext();

        // Setup
        removeFirstTimeFlag(context);
        clearDatabase(context);

        // Hooks
        CleanSetupTestBase.this.beforeActivityLaunched(context);

        super.beforeActivityLaunched();
      }
    };
  }

  protected void beforeActivityLaunched(Context context) {
    // Implement in subclasses
  }

  private void clearDatabase(Context context) {
    context.deleteDatabase(DatabaseHelper.DATABASE_NAME);
  }

  private void removeFirstTimeFlag(Context context) {
    SharedPreferences.Editor preferences = context.getSharedPreferences(
        ReadTrackerApp.PREFERENCES_FILE_NAME, Context.MODE_PRIVATE
    ).edit();
    preferences.putBoolean(ReadTrackerApp.KEY_FIRST_TIME, false);
    preferences.commit();
  }
}
