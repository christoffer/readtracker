package com.readtracker.android.db.export;

import com.readtracker.android.db.Book;
import com.readtracker.android.db.Quote;
import com.readtracker.android.db.Session;
import com.readtracker.android.support.Utils;
import com.readtracker.android.test_support.DatabaseTestCase;
import com.readtracker.android.test_support.TestUtils;

import org.json.JSONObject;
import org.junit.Ignore;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import static com.readtracker.android.test_support.TestUtils.buildSession;

public class JSONExporterTest extends DatabaseTestCase {

  private JSONExporter getExporter() {
    return new JSONExporter(getDatabaseManager());
  }

  @Test
  public void jsonExporterTest_ExportPopulatedBook_ReturnsStringOfOutputPath() throws Exception {
    List<Book> books = populateBooksForExpectedOutput();

    // Convert to string to get type agnostic comparison
    String actual = getExporter().exportAll(books).toString();
    String expected = TestUtils.readFixtureFile("expected_output_of_populated_book_test.json");

    JSONAssert.assertEquals(expected, actual, true);
  }

  @Test
  // Similar to export_all_books, but tests the write out to disk
  public void jsonExporterTest_ExportPopulatedBook_ReturnsExportedJsonFromIO() throws Exception {
    List<Book> books = populateBooksForExpectedOutput();

    String exportFilename = TestUtils.randomString();
    File exportFile = new File(getContext().getFilesDir(), exportFilename);

    getExporter().exportToDisk(books, exportFile);

    InputStream inputStream = new FileInputStream(getContext().getFileStreamPath(exportFilename));
    String exportFileContent = Utils.readInputStream(inputStream);

    JSONObject actual = new JSONObject(exportFileContent);
    String expected = TestUtils.readFixtureFile("expected_output_of_populated_book_test.json");

    JSONAssert.assertEquals(expected, actual, JSONCompareMode.NON_EXTENSIBLE);
  }

  @Ignore
  private List<Book> populateBooksForExpectedOutput() {
    Book metamorphosis = new Book();
    metamorphosis.setTitle("Metamorphosis");
    metamorphosis.setAuthor("Franz Kafka");
    metamorphosis.setClosingRemark("Dude, poor guy!");
    metamorphosis.setCoverImageUrl("https://images-of-books/metamorphosis.png");
    metamorphosis.setCurrentPosition(0.5f);
    metamorphosis.setCurrentPositionTimestampMs(213456789L);
    metamorphosis.setFirstPositionTimestampMs(123456789L);
    metamorphosis.setPageCount(123.45f);
    metamorphosis.setState(Book.State.Finished);
    getDatabaseManager().save(metamorphosis);

    Quote unicornQuote = TestUtils.buildQuote(metamorphosis, "unicorn", 0.45f, 123456799L);
    metamorphosis.getQuotes().add(unicornQuote);
    getDatabaseManager().save(unicornQuote);

    Quote guppyQuote = TestUtils.buildQuote(metamorphosis, "guppy", 0f, 4564321L);
    metamorphosis.getQuotes().add(guppyQuote);
    getDatabaseManager().save(guppyQuote);

    Session firstSession = buildSession(metamorphosis, 0.45f, 0.75f, 1234, 4000000L);
    metamorphosis.getSessions().add(firstSession);
    getDatabaseManager().save(firstSession);

    Session secondSession = buildSession(metamorphosis, 0.75f, 1.0f, 600, 5000000L);
    metamorphosis.getSessions().add(secondSession);
    getDatabaseManager().save(secondSession);

    Book androidFurDummies = new Book();
    androidFurDummies.setTitle("Android Apps Entwicklung für Dummies");
    androidFurDummies.setAuthor("Donn Felker");
    androidFurDummies.setState(Book.State.Finished);
    androidFurDummies.setCurrentPosition(0.00872093066573143f);
    androidFurDummies.setCurrentPositionTimestampMs(1400856553800L);
    androidFurDummies.setPageCount(344f);
    androidFurDummies.setCoverImageUrl("http://bks8.books.google.de/books?id=KPjmuog");
    getDatabaseManager().save(androidFurDummies);

    {
      Session session = buildSession(androidFurDummies, 0f, 0.00872093066573143f, 3, 1399535743000L);
      androidFurDummies.getSessions().add(session);
      getDatabaseManager().save(session);
    }

    {
      Session session = buildSession(androidFurDummies, 0.008720931f, 0.9800000190734863f, 1146, 1400856553800L);
      androidFurDummies.getSessions().add(session);
      getDatabaseManager().save(session);
    }

    Book emptyBook = new Book();
    getDatabaseManager().save(emptyBook);

    return Arrays.asList(metamorphosis, androidFurDummies, emptyBook);
  }
}