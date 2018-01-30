package com.readtracker.android.db.export

import android.support.test.filters.SmallTest
import com.readtracker.android.db.Book
import com.readtracker.android.db.Model
import com.readtracker.android.support.DatabaseTestCase
import com.readtracker.android.support.SharedExampleAsserts
import com.readtracker.android.support.TestUtils
import com.readtracker.android.support.TestUtils.randomString
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.*

class JSONImporterTest : DatabaseTestCase() {

    /**
     * Helper method to return the JSON de-serializer
     * @return JSONImporter
     */
    private val importer: JSONImporter
        get() = JSONImporter(databaseManager)

    /**
     * Get a file to import, make sure that the database contains no (0) [Book] entities,
     * and import the file contents, then assert that database again.
     * @throws Exception
     */
    @SmallTest
    @Test
    @Throws(Exception::class)
    fun jsonImporterTest_ImportBookVersionOne() {
        val fileToImport = copyResourceFile("export_version_1.json")

        assertEquals(0, databaseManager.getAll(Book::class.java).size)
        importer.importFile(fileToImport)

        assertImportedBooks()
    }

    /**
     * Same as [jsonImporterTest_ImportBookVersionOne] but with a different file to import.
     * @throws Exception
     */
    @SmallTest
    @Test
    @Throws(Exception::class)
    fun jsonImporterTest_ImportBookVersionTwo() {
        val fileToImport = copyResourceFile("export_version_2.json")

        assertEquals(0, databaseManager.getAll(Book::class.java).size)
        importer.importFile(fileToImport)

        assertImportedBooks()
    }

    /**
     * Create a file to import with a specified [Book] and a pre-existing book.
     * We save the books into the database and assert the state (size) of the
     * database along the side. We assume no conflict during the save.
     * @throws Exception
     */
    @SmallTest
    @Test
    @Throws(Exception::class)
    fun jsonImporterTest_ImportBookWithNoMergeConflict() {
        val bookToImport = TestUtils.buildBook("постои конфликт", "авторот", 200f)
        val fileToImport = createExportFileForBooks(Arrays.asList(bookToImport))
        val preExistingBook = TestUtils.buildBook("Metamorphosis", "Franz Kafka", 400f)

        databaseManager.save(preExistingBook)

        assertEquals(1, databaseManager.getAll(Book::class.java).size)
        importer.importFile(fileToImport)
        assertEquals(2, databaseManager.getAll(Book::class.java).size)

        val books = databaseManager.getAll(Book::class.java)
        assertFalse(books[0].title == books[1].title)
    }

    /**
     * Create and save a pre-existing [Book] into the database, then try to import
     * the same book again. Assert that the simple conflict/merge has been done
     * successfully, and the book contains the previous id and updated title and contents.
     * @throws Exception
     */
    @SmallTest
    @Test
    @Throws(Exception::class)
    fun jsonImporterTest_ImportBookWithSimpleMergeConflict() {
        val existing = TestUtils.buildBook("Metamorphosis", "Franz Kafka", 200f)
        databaseManager.save(existing)
        val preId = existing.id.toLong()

        val importBook = TestUtils.buildBook("Metamorphosis", "Franz Kafka", 300f)
        val importFile = createExportFileForBooks(Arrays.asList(importBook))

        importer.importFile(importFile)

        val books = databaseManager.getAll(Book::class.java)
        assertEquals(1, books.size)
        val bookAfterImport = books[0]

        assertEquals(preId, bookAfterImport.id.toLong())
        assertEquals("Metamorphosis", bookAfterImport.title)
        assertEquals("Franz Kafka", bookAfterImport.author)

        // Only check one merged value here, as this is based on the [Session] method, which is
        // validated elsewhere.
        assertEquals(300f, bookAfterImport.pageCount)
    }

    /**
     * Create and save a [Book] in the database with [Session]s and [Quote]s,
     * then delete the book. Try to import the same book with clashing
     * session and quote and assert that the save cascades these conflicting changes.
     * @throws Exception
     */
    @SmallTest
    @Test
    @Throws(Exception::class)
    fun jsonImporterTest_ImportBookWithNestedMergeConflict() {
        // Create a book in the database that we later want to import
        val imported = TestUtils.buildBook("Metamorphosis", "Franz Kafka", 200f)

        with(imported) {
            val noConflictSession = TestUtils.buildSession(imported, 0.3f, 0.4f, 234, 3)
            val conflictedSession = TestUtils.buildSession(imported, 0.8f, 0.9f, 456, 2)

            val noConflictQuote = TestUtils.buildQuote(imported, "imported with freedom", 0.5f, 3)
            val conflictedQuote = TestUtils.buildQuote(imported, "crash!", 0.95f, 2)

            databaseManager.saveAll<Model>(
                    imported, noConflictQuote, conflictedQuote, noConflictSession, conflictedSession
            )
        }

        val importFile = createExportFileForBooks(Arrays.asList(imported))
        databaseManager.delete(imported) // delete temporary book

        // Create a new book in the database that we want to collide with
        val existing = TestUtils.buildBook("Metamorphosis", "Franz Kafka", 200f)

        with(existing) {
            val noConflictSession = TestUtils.buildSession(existing, 0.1f, 0.2f, 123, 1)
            val conflictedSession = TestUtils.buildSession(existing, 0.8f, 0.9f, 456, 2)

            val noConflictQuote = TestUtils.buildQuote(existing, "freedom", 0.5f, 1)
            val conflictedQuote = TestUtils.buildQuote(existing, "crash!", 0.95f, 2)

            databaseManager.saveAll<Model>(
                    existing, noConflictQuote, conflictedQuote, noConflictSession, conflictedSession
            )
        }

        importer.importFile(importFile)

        val books = databaseManager.getAll(Book::class.java)
        assertEquals(1, books.size)
        val book = books[0].apply {
            loadQuotes(databaseManager)
            loadSessions(databaseManager)
        }

        assertEquals(3, book.sessions.size)
        // NOTE(christoffer) this depends on order, which is not ideal. It makes the assumption that
        // the returned sessions will be sorted by id (creation order)
        assertEquals(123, book.sessions[0].durationSeconds)
        assertEquals(456, book.sessions[1].durationSeconds) // 2 was overwritten by 3
        assertEquals(234, book.sessions[2].durationSeconds)

        assertEquals(3, book.quotes.size)
        assertEquals(1L, book.quotes[0].addTimestampMs)
        assertEquals(2L, book.quotes[1].addTimestampMs)
        assertEquals(3L, book.quotes[2].addTimestampMs)
    }

    /**
     * Helper method to create a [File] with JSON output from a list of [Book] entities.
     *
     * @param books [List] of [Book] items
     * @return File with JSON output
     * @throws Exception
     */
    @Throws(Exception::class)
    private fun createExportFileForBooks(books: List<Book>): File {
        assertNotNull(getContext())

        val exportFile = File(getContext().filesDir, randomString())
        val content = JSONExporter(databaseManager).exportAll(books).toString()
        FileOutputStream(exportFile).run {
            write(content.toByteArray())
            close()
        }

        return exportFile
    }

    /**
     * Helper method to assert imported [Book]s from database
     * against our [SharedExampleAsserts] class.
     */
    private fun assertImportedBooks() {
        val booksInDatabase = databaseManager.getAll(Book::class.java)
        for (book in booksInDatabase) {
            book.loadSessions(databaseManager)
            book.loadQuotes(databaseManager)
        }

        SharedExampleAsserts.assertExampleBooksVersion2(booksInDatabase)
    }

    /**
     * Helper method to create a File from a resource path.
     * @param resourcePath String path to resource
     * @return File
     * @throws IOException
     */
    @Throws(IOException::class)
    private fun copyResourceFile(resourcePath: String): File {
        assertNotNull(getContext())

        val exportFile = File(getContext().filesDir, randomString())
        val content = TestUtils.readFixtureFile(resourcePath)
        FileOutputStream(exportFile).run {
            write(content.toByteArray())
            close()
        }
        return exportFile
    }
}
