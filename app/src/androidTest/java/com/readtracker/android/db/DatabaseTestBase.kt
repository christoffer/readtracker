package com.readtracker.android.db

import androidx.test.core.app.ApplicationProvider
import com.readtracker.android.ReadTrackerApp
import org.junit.After

const val TEST_DATABASE_NAME = "readtracker-test.db"

abstract class DatabaseTestBase {
    @After
    fun clearDatabase() {
        val appContext = ApplicationProvider.getApplicationContext<ReadTrackerApp>()
        appContext.deleteDatabase(TEST_DATABASE_NAME)
    }

    fun getManagerOfTestDatabaseAtVersion(dbVersion: Int): DatabaseManager {
        val appContext = ApplicationProvider.getApplicationContext<ReadTrackerApp>()
        val dbHelper = DatabaseHelper(appContext, TEST_DATABASE_NAME, null, dbVersion)
        appContext.deleteDatabase(TEST_DATABASE_NAME)
        return DatabaseManager(dbHelper)
    }

    fun getManagerOfCleanTestDatabase(): DatabaseManager {
        return getManagerOfTestDatabaseAtVersion(DatabaseHelper.DATABASE_VERSION)
    }
}
