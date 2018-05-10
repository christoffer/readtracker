package com.readtracker.android.support

import android.content.Context
import com.readtracker.android.db.DatabaseHelper
import com.readtracker.android.db.DatabaseManager
import org.junit.After
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest=Config.NONE)
abstract class DatabaseTestCase {

    protected lateinit var databaseManager: DatabaseManager
    protected lateinit var databaseHelper: DatabaseHelper

    /**
     * Set the context (using RoboElectric), clean up the database and set the database manager.
     */
    @Before
    fun setUp() {
        databaseHelper = createDatabaseHelper()
        deleteDatabase()
        databaseManager = DatabaseManager(databaseHelper)
    }

    /**
     * Delete the database
     */
    @After
    fun tearDown() {
        deleteDatabase()
    }

    open fun createDatabaseHelper(): DatabaseHelper {
        return DatabaseHelper(getContext(), DATABASE_NAME, null, DATABASE_VERSION)
    }

    open fun deleteDatabase() {
        getContext().deleteDatabase(DATABASE_NAME)
    }

    protected fun getContext(): Context = RuntimeEnvironment.application.applicationContext

    companion object {
        const val DATABASE_NAME = "readtracker-test.db"
        const val DATABASE_VERSION = DatabaseHelper.DATABASE_VERSION
    }
}
