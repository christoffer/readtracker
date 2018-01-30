package com.readtracker.android.db

import android.support.test.filters.SmallTest
import com.readtracker.android.support.DatabaseTestCase
import com.readtracker.android.support.TestUtils
import org.junit.Assert.*

import org.junit.Test

class MigrationTest : DatabaseTestCase() {

    /**
     * Helper function to return a DatabaseHelper entity for managing
     * the db connection and setup for the tests.
     * @return [DatabaseHelper]
     */
    override fun createDatabaseHelper(): DatabaseHelper {
        assertNotNull(getContext())
        val migrationStartVersion = 11
        return DatabaseHelper(getContext(), DatabaseTestCase.DATABASE_NAME, null, migrationStartVersion)
    }

    /**
     * Migrate broken session timestamps from version 3.1
     *
     * Create a session with a timestamp < 1971, that will be corrupted once
     * it's saved in the buggy 3.1 version of OrmLite. It is expected that it
     * will be saved in seconds instead of milliseconds. Then we create another
     * session that we expect to be perfectly fine. We fix the broken data if we find any
     * using the migrateVersion31Sessions function. Then we assert the results.
     */
    @SmallTest
    @Test
    fun migrationTest_MigrateFrom11to12_BugFix() {
        val book = TestUtils.buildRandomBook()
        databaseManager.save(book)

        val brokenSession = Session().apply {
            timestampMs = 11158586000L
            this@apply.book = book
        }
        databaseManager.save(brokenSession)

        val probablyOkSession = Session().apply {
            timestampMs = 894771386000L
            this@apply.book = book
        }
        databaseManager.save(probablyOkSession)

        DatabaseHelper.migrateVersion31Sessions(databaseManager)

        val session = databaseManager.getAll(Session::class.java)
        assertEquals(2, session.size)

        val tresholdDate = 31536000000L /* Fri, 01 Jan 1971 00:00:00 GMT */
        assertTrue(session[0].timestampMs >= tresholdDate)
        assertTrue(session[1].timestampMs >= tresholdDate)
    }
}
