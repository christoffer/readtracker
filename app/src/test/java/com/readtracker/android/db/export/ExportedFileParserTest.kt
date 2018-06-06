package com.readtracker.android.db.export

import com.readtracker.android.db.Book
import src.JSONFixtureAssertions
import org.json.JSONException
import org.junit.Assert.assertEquals
import org.junit.Test
import src.readFixtureFile

class ExportedFileParserTest {

    /**
     * Check that the parser handles a json correctly.
     */
    @Test
    @Throws(Exception::class)
    fun exportedFileParserTest_buildBookFromJson_returnListOfBooks() {
        val books = parseTestFile("export_version_2.json")
        JSONFixtureAssertions.assertBookListMatchesExpectedResultFromFixtureImport(books)
    }

    /**
     * Check that the parser handles null in json correctly.
     */
    @Test
    @Throws(Exception::class)
    fun exportedFileParserTest_buildBookFromJsonWithNullCases_returnListOfBooks() {
        val books = parseTestFile("export_version_2_null_fields.json")
        assertEquals(1, books.size)

        val book = books[0]
        JSONFixtureAssertions.assertBookMatchesExpectedResultFromNullFieldsFixture(book)
    }

    /**
     * Check that the parser handles missing fields json correctly.
     */
    @Test
    @Throws(Exception::class)
    fun exportedFileParserTest_buildBookFromJsonWithMissingValues_returnListOfBooks() {
        val books = parseTestFile("export_version_2_missing_fields.json")
        assertEquals(1, books.size)

        val book = books[0]
        JSONFixtureAssertions.assertBookMatchesExpectedResultFromNullFieldsFixture(book)
    }

    /**
     * Helper method for opening up a json file from a given path and pass it along to the parser.
     *
     * @param filename path to JSON
     * @return [List] of [Book] items from parsing the contents of the JSON
     * @throws JSONException if something goes wrong during parsing
    */
    @Throws(JSONException::class)
    private fun parseTestFile(filename: String): List<Book> {
        val fileContent = readFixtureFile(filename)
        val parser = ExportedFileParser()
        return parser.parse(fileContent)
    }
}