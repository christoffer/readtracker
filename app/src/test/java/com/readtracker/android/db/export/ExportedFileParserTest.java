package com.readtracker.android.db.export;

import com.readtracker.android.db.Book;
import com.readtracker.android.test_support.TestUtils;

import junit.framework.TestCase;

import org.json.JSONException;

import java.util.List;

public class ExportedFileParserTest extends TestCase {

  public void test_build_book_from_json() throws Exception {
    List<Book> books = _parseTestFile("export_version_2.json");
    SharedExampleAsserts.assertExampleBooksVersion2(books);
  }

  /**
   * Check that the parser handles null in json correctly.
   */
  public void test_build_null_values_from_json() throws Exception {
    List<Book> books = _parseTestFile("export_version_2_null_fields.json");
    assertEquals(1, books.size());
    Book book = books.get(0);
    SharedExampleAsserts.assertNullBookVersion2(book);
  }

  /** Check that the parser handles missing fields json correctly. */
  public void test_build_missing_values_from_json() throws Exception {
    List<Book> books = _parseTestFile("export_version_2_missing_fields.json");
    assertEquals(1, books.size());
    Book book = books.get(0);
    SharedExampleAsserts.assertNullBookVersion2(book);
  }

  private List<Book> _parseTestFile(String filename) throws JSONException {
    final String fileContent = TestUtils.readFixtureFile(filename);
    final ExportedFileParser parser = new ExportedFileParser();
    return parser.parse(fileContent);
  }
}