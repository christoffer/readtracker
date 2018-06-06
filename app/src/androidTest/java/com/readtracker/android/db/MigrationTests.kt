package com.readtracker.android.db

import junit.framework.Assert.assertTrue
import src.buildRandomBook
import org.junit.Assert
import org.junit.Test


class MigrationTest : DatabaseTestBase() {

    /**
     * Migrate broken session timestamps from version 3.1
     *
     * Create a session with a timestamp < 1971, that will be corrupted once
     * it's saved in the buggy 3.1 version of OrmLite. It is expected that it
     * will be saved in seconds instead of milliseconds. Then we create another
     * session that we expect to be perfectly fine. We fix the broken data if we find any
     * using the migrateVersion31Sessions function. Then we assert the results.
     */
    @Test
    fun migrationTest_MigrateFrom11to12_BugFix() {
        val dbManager = getManagerOfTestDatabaseAtVersion(11)

        val testBook = buildRandomBook()
        dbManager.save(testBook)

        fun saveSessionWithTimestamp(sessionTimestampMs: Long) {
            val session = Session().apply {
                timestampMs = sessionTimestampMs
                book = testBook
            }
            dbManager.save(session)
        }

        saveSessionWithTimestamp(11158586000L)
        saveSessionWithTimestamp(894771386000L)

        DatabaseHelper.migrateVersion31Sessions(dbManager)

        val session = dbManager.getAll(Session::class.java)
        Assert.assertEquals(2, session.size)

        val thresholdDate = 31536000000L /* Fri, 01 Jan 1971 00:00:00 GMT */
        Assert.assertTrue(session[0].timestampMs >= thresholdDate)
        Assert.assertTrue(session[1].timestampMs >= thresholdDate)
    }
}
