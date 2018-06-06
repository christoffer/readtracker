package com.readtracker.android.db

import android.support.test.InstrumentationRegistry.getTargetContext
import com.readtracker.android.db.export.JSONExporter
import com.readtracker.android.support.Utils
import src.JSONFixtureAssertions.populateBooksForExpectedOutput
import org.json.JSONObject
import org.junit.Test
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode
import src.randomString
import src.readFixtureFile
import java.io.File
import java.io.FileInputStream

class JSONExporterTest : DatabaseTestBase() {

    /**
     * Get a list of books that we assemble, export it into
     * JSON and assert against the expected output.
     * @throws Exception
     */
    @Test
    fun jsonExporterTest_ExportPopulatedBook_ReturnsStringOfOutputPath() {
        val databaseManager = getManagerOfCleanTestDatabase()
        val books = populateBooksForExpectedOutput(databaseManager)
        val exporter = JSONExporter.withDatabaseManager(databaseManager)

        // Convert to string to get type agnostic comparison
        val actual = exporter.exportAll(books).toString() + "foo"
        val expected = readFixtureFile("expected_output_of_populated_book_test.json")

        JSONAssert.assertEquals(expected, actual, true)
    }

    /**
     * Similar to [jsonExporterTest_ExportPopulatedBook_ReturnsStringOfOutputPath],
     * but tests the write out to disk.
     * @throws Exception
     */
    @Test
    fun jsonExporterTest_ExportPopulatedBook_ReturnsExportedJsonFromIO() {
        val databaseManager = getManagerOfCleanTestDatabase()
        val exporter = JSONExporter.withDatabaseManager(databaseManager)
        val fixtureBooks = populateBooksForExpectedOutput(databaseManager)
        val exportFilename = randomString()
        val exportFile = File(getTargetContext().filesDir, exportFilename)

        exporter.exportBooksToFile(fixtureBooks, exportFile)

        val inputStream = FileInputStream(getTargetContext().getFileStreamPath(exportFilename))
        val exportFileContent = Utils.readInputStream(inputStream)
        val actual = JSONObject(exportFileContent)
        val expected = readFixtureFile("expected_output_of_populated_book_test.json")
        JSONAssert.assertEquals(expected, actual, JSONCompareMode.NON_EXTENSIBLE)
    }
}