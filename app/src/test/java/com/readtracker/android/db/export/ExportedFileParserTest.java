package com.readtracker.android.db.export;

import android.test.AndroidTestCase;

import com.readtracker.android.db.Book;
import com.readtracker.android.test_support.SharedExampleAsserts;
import com.readtracker.android.test_support.TestUtils;

import org.json.JSONException;
import org.junit.Test;

import java.util.List;

public class ExportedFileParserTest extends AndroidTestCase {

  /**
   * Check that the parser handles a json correctly.
   */
  @Test
  public void exportedFileParserTest_buildBookFromJson_returnListOfBooks() throws Exception {
    List<Book> books = _parseTestFile("export_version_2.json");
    SharedExampleAsserts.assertExampleBooksVersion2(books);
  }

  /**
   * Check that the parser handles null in json correctly.
   */
  @Test
  public void exportedFileParserTest_buildBookFromJsonWithNullCases_returnListOfBooks() throws Exception {
    List<Book> books = _parseTestFile("export_version_2_null_fields.json");
    assertEquals(1, books.size());
    Book book = books.get(0);
    SharedExampleAsserts.assertNullBookVersion2(book);
  }

  /**
   * Check that the parser handles missing fields json correctly.
   */
  @Test
  public void exportedFileParserTest_buildBookFromJsonWithMissingValues_returnListOfBooks() throws Exception {
    List<Book> books = _parseTestFile("export_version_2_missing_fields.json");
    assertEquals(1, books.size());
    Book book = books.get(0);
    SharedExampleAsserts.assertNullBookVersion2(book);
  }

  /**
   * Helper method for opening up a json file from a given path and pass it along to the parser.
   *
   * @param filename path to JSON
   * @return List<Book> from parsing the contents of the JSON
   * @throws JSONException if something goes wrong during parsing
   */
  private List<Book> _parseTestFile(String filename) throws JSONException {
    final String fileContent = TestUtils.readFixtureFile(filename);
    final ExportedFileParser parser = new ExportedFileParser();
    return parser.parse(fileContent);
  }
}