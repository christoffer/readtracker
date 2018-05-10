package com.readtracker.android.db.export

import android.support.test.filters.SmallTest
import com.readtracker.android.db.Book
import com.readtracker.android.support.DatabaseTestCase
import com.readtracker.android.support.TestUtils
import com.readtracker.android.support.TestUtils.buildSession
import com.readtracker.android.support.Utils
import org.json.JSONObject
import org.junit.Test
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode
import java.io.File
import java.io.FileInputStream
import java.util.*

class JSONExporterTest : DatabaseTestCase() {

    /**
     * Helper method for returning the JSON serializer
     * @return JSONExporter
     */
    private val exporter: JSONExporter
        get() = JSONExporter(databaseManager)

    /**
     * Get a list of books that we assemble, export it into
     * JSON and assert agains the expected output.
     * @throws Exception
     */
    @Test
    @SmallTest
    @Throws(Exception::class)
    fun jsonExporterTest_ExportPopulatedBook_ReturnsStringOfOutputPath() {
        val books = populateBooksForExpectedOutput()

        // Convert to string to get type agnostic comparison
        val actual = exporter.exportAll(books).toString()
        val expected = TestUtils.readFixtureFile("expected_output_of_populated_book_test.json")

        JSONAssert.assertEquals(expected, actual, true)
    }

    /**
     * Similar to [jsonExporterTest_ExportPopulatedBook_ReturnsStringOfOutputPath],
     * but tests the write out to disk.
     * @throws Exception
     */
    @Test
    @SmallTest
    @Throws(Exception::class)
    fun jsonExporterTest_ExportPopulatedBook_ReturnsExportedJsonFromIO() {
        val books = populateBooksForExpectedOutput()

        val exportFilename = TestUtils.randomString()
        val exportFile = File(getContext().filesDir, exportFilename)

        exporter.exportToDisk(books, exportFile)

        val inputStream = FileInputStream(getContext().getFileStreamPath(exportFilename))
        val exportFileContent = Utils.readInputStream(inputStream)

        val actual = JSONObject(exportFileContent)
        val expected = TestUtils.readFixtureFile("expected_output_of_populated_book_test.json")

        JSONAssert.assertEquals(expected, actual, JSONCompareMode.NON_EXTENSIBLE)
    }

    /**
     * Helper method to assemble a list of [Book]s with known data.
     * @return [List] of [Book] entities with data, [Quote]s and [Session]s.
    */
    private fun populateBooksForExpectedOutput(): List<Book> {
        val metamorphosis = Book().apply {
            title = "Metamorphosis"
            author = "Franz Kafka"
            closingRemark = "Dude, poor guy!"
            coverImageUrl = "https://images-of-books/metamorphosis.png"
            currentPosition = 0.5f
            currentPositionTimestampMs = 213456789L
            firstPositionTimestampMs = 123456789L
            pageCount = 123.45f
            state = Book.State.Finished
        }
        databaseManager.save(metamorphosis)

        TestUtils.buildQuote(metamorphosis, "unicorn", 0.45f, 123456799L).let { unicornQuote ->
            metamorphosis.quotes.add(unicornQuote)
            databaseManager.save(unicornQuote)
        }

        TestUtils.buildQuote(metamorphosis, "guppy", 0f, 4564321L).let { guppyQuote ->
            metamorphosis.quotes.add(guppyQuote)
            databaseManager.save(guppyQuote)
        }

        buildSession(metamorphosis, 0.45f, 0.75f, 1234, 4000000L).let { firstSession ->
            metamorphosis.sessions.add(firstSession)
            databaseManager.save(firstSession)
        }

        buildSession(metamorphosis, 0.75f, 1.0f, 600, 5000000L).let { secondSession ->
            metamorphosis.sessions.add(secondSession)
            databaseManager.save(secondSession)
        }

        val androidFurDummies = Book().apply {
            title = "Android Apps Entwicklung für Dummies"
            author = "Donn Felker"
            state = Book.State.Finished
            currentPosition = 0.00872093066573143f
            currentPositionTimestampMs = 1400856553800L
            pageCount = 344f
            coverImageUrl = "http://bks8.books.google.de/books?id=KPjmuog"
        }

        databaseManager.save(androidFurDummies)

        buildSession(androidFurDummies, 0f, 0.00872093066573143f, 3, 1399535743000L).let { session ->
            androidFurDummies.sessions.add(session)
            databaseManager.save(session)
        }

        buildSession(androidFurDummies, 0.008720931f, 0.9800000190734863f, 1146, 1400856553800L).let { session ->
            androidFurDummies.sessions.add(session)
            databaseManager.save(session)
        }

        val emptyBook = Book()
        databaseManager.save(emptyBook)

        return Arrays.asList(metamorphosis, androidFurDummies, emptyBook)
    }
}