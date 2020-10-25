package com.readtracker.android.db

import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After

const val TEST_DATABASE_NAME = "readtracker-test.db"

abstract class DatabaseTestBase {
    @After
    fun clearDatabase() {
        InstrumentationRegistry.getTargetContext().deleteDatabase(TEST_DATABASE_NAME)
    }

    fun getManagerOfTestDatabaseAtVersion(dbVersion: Int): DatabaseManager {
        val dbHelper = DatabaseHelper(InstrumentationRegistry.getTargetContext(), TEST_DATABASE_NAME, null, dbVersion)
        InstrumentationRegistry.getTargetContext().deleteDatabase(TEST_DATABASE_NAME)
        return DatabaseManager(dbHelper)
    }

    fun getManagerOfCleanTestDatabase(): DatabaseManager {
        return getManagerOfTestDatabaseAtVersion(DatabaseHelper.DATABASE_VERSION)
    }
}