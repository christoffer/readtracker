package com.readtracker.android.db.export;

import android.content.Context;

import com.readtracker.android.db.Book;
import com.readtracker.android.db.Quote;
import com.readtracker.android.db.Session;
import com.readtracker.android.test_support.DatabaseTestCase;
import com.readtracker.android.test_support.SharedExampleAsserts;
import com.readtracker.android.test_support.TestUtils;

import junit.framework.Assert;

import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static com.readtracker.android.test_support.TestUtils.randomString;

public class JSONImporterTest extends DatabaseTestCase {

  /**
   * Helper method to return the JSON de-serializer
   * @return JSONImporter
   */
  private JSONImporter getImporter() {
    return new JSONImporter(getDatabaseManager());
  }

  /**
   * Get a file to import, make sure that the database contains no (0) books,
   * and import the file contents, then assert that database again.
   * @throws Exception
   */
  @Test
  public void jsonImporterTest_ImportBookVersionOne() throws Exception {
    File fileToImport = _copyResourceFile("export_version_1.json");

    assertEquals(0, getDatabaseManager().getAll(Book.class).size());
    getImporter().importFile(fileToImport);

    _assertImportedBooks();
  }

  /**
   * Same as jsonImporterTest_ImportBookVersionOne() but with a different file to import.
   * @throws Exception
   */
  @Test
  public void jsonImporterTest_ImportBookVersionTwo() throws Exception {
    File fileToImport = _copyResourceFile("export_version_2.json");

    assertEquals(0, getDatabaseManager().getAll(Book.class).size());
    getImporter().importFile(fileToImport);

    _assertImportedBooks();
  }

  /**
   * Create a file to import with a specified book and a pre-existing book.
   * We save the books into the database and assert the state (size) of the
   * database along the side. We assume no conflict during the save.
   * @throws Exception
   */
  @Test
  public void jsonImporterTest_ImportBookWithNoMergeConflict() throws Exception {
    Book bookToImport = TestUtils.buildBook("постои конфликт", "авторот", 200);
    File fileToImport = _createExportFileForBooks(Arrays.asList(bookToImport));

    Book preExistingBook = TestUtils.buildBook("Metamorphosis", "Franz Kafka", 400);
    getDatabaseManager().save(preExistingBook);

    assertEquals(1, getDatabaseManager().getAll(Book.class).size());
    getImporter().importFile(fileToImport);

    List<Book> books = getDatabaseManager().getAll(Book.class);
    assertEquals(2, getDatabaseManager().getAll(Book.class).size());

    assertFalse(books.get(0).getTitle().equals(books.get(1).getTitle()));
  }

  /**
   * Create and save a pre-existing book into the database, then try to import
   * the same book again. Assert that the simple conflict/merge has been done
   * successfully, and the book contains the previous id and updated title and contents.
   * @throws Exception
   */
  @Test
  public void jsonImporterTest_ImportBookWithSimpleMergeConflict() throws Exception {
    Book existing = TestUtils.buildBook("Metamorphosis", "Franz Kafka", 200);
    getDatabaseManager().save(existing);
    final long preId = existing.getId();

    Book importBook = TestUtils.buildBook("Metamorphosis", "Franz Kafka", 300);
    File importFile = _createExportFileForBooks(Arrays.asList(importBook));

    getImporter().importFile(importFile);

    List<Book> books = getDatabaseManager().getAll(Book.class);
    assertEquals(1, books.size());
    Book bookAfterImport = books.get(0);
    assertEquals(preId, bookAfterImport.getId());
    assertEquals("Metamorphosis", bookAfterImport.getTitle());
    assertEquals("Franz Kafka", bookAfterImport.getAuthor());

    // Only check one merged value here, as this is based on the #merge() method, which is
    // validated elsewhere.
    assertEquals(300f, bookAfterImport.getPageCount());
  }

  /**
   * Create and save a book in the database with sessions and quotes,
   * then delete the book. Try to import the same book with clashing
   * session and quote and assert that the save cascades these conflicting changes.
   * @throws Exception
   */
  @Test
  public void jsonImporterTest_ImportBookWithNestedMergeConflict() throws Exception {
    // Create a book in the database that we later want to import
    Book imported = TestUtils.buildBook("Metamorphosis", "Franz Kafka", 200);
    {
      Session noConflictSession = TestUtils.buildSession(imported, 0.3f, 0.4f, 234, 3);
      Session conflictedSession = TestUtils.buildSession(imported, 0.8f, 0.9f, 456, 2);

      Quote noConflictQuote = TestUtils.buildQuote(imported, "imported with freedom", 0.5f, 3);
      Quote conflictedQuote = TestUtils.buildQuote(imported, "crash!", 0.95f, 2);

      getDatabaseManager().saveAll(
          imported, noConflictQuote, conflictedQuote, noConflictSession, conflictedSession
      );
    }

    File importFile = _createExportFileForBooks(Arrays.asList(imported));
    getDatabaseManager().delete(imported); // delete temporary book

    // Create a new book in the database that we want to collide with
    Book existing = TestUtils.buildBook("Metamorphosis", "Franz Kafka", 200);
    {
      Session noConflictSession = TestUtils.buildSession(existing, 0.1f, 0.2f, 123, 1);
      Session conflictedSession = TestUtils.buildSession(existing, 0.8f, 0.9f, 456, 2);

      Quote noConflictQuote = TestUtils.buildQuote(existing, "freedom", 0.5f, 1);
      Quote conflictedQuote = TestUtils.buildQuote(existing, "crash!", 0.95f, 2);

      getDatabaseManager().saveAll(
          existing, noConflictQuote, conflictedQuote, noConflictSession, conflictedSession
      );
    }

    getImporter().importFile(importFile);

    List<Book> books = getDatabaseManager().getAll(Book.class);
    assertEquals(1, books.size());
    Book book = books.get(0);
    book.loadQuotes(getDatabaseManager());
    book.loadSessions(getDatabaseManager());

    assertEquals(3, book.getSessions().size());
    // NOTE(christoffer) this depends on order, which is not ideal. It makes the assumtion that
    // the returned sessions will be sorted by id (creation order)
    assertEquals(123, book.getSessions().get(0).getDurationSeconds());
    assertEquals(456, book.getSessions().get(1).getDurationSeconds()); // 2 was overwritten by 3
    assertEquals(234, book.getSessions().get(2).getDurationSeconds());

    assertEquals(3, book.getQuotes().size());
    assertEquals(Long.valueOf(1), book.getQuotes().get(0).getAddTimestampMs());
    assertEquals(Long.valueOf(2), book.getQuotes().get(1).getAddTimestampMs());
    assertEquals(Long.valueOf(3), book.getQuotes().get(2).getAddTimestampMs());
  }

  /**
   * Helper method to create a File with JSON output from a list of Book entities.
   * @param books List<Book>
   * @return File with JSON output
   * @throws Exception
   */
  private File _createExportFileForBooks(List<Book> books) throws Exception {
    Context context = getContext();
    setContext(context);
    Assert.assertNotNull(context);
    File exportFile = new File(context.getFilesDir(), randomString());
    String content = new JSONExporter(getDatabaseManager()).exportAll(books).toString();
    FileOutputStream fos = new FileOutputStream(exportFile);
    fos.write(content.getBytes());
    fos.close();
    return exportFile;
  }

  /**
   * Helper method to assert imported books from database
   * against our SharedExampleAsserts class.
   */
  private void _assertImportedBooks() {
    List<Book> booksInDatabase = getDatabaseManager().getAll(Book.class);
    for(Book book : booksInDatabase) {
      book.loadSessions(getDatabaseManager());
      book.loadQuotes(getDatabaseManager());
    }

    SharedExampleAsserts.assertExampleBooksVersion2(booksInDatabase);
  }

  /**
   * Helper method to create a File from a resource path.
   * @param resourcePath String path to resource
   * @return File
   * @throws IOException
   */
  private File _copyResourceFile(String resourcePath) throws IOException {
    Context context = getContext();
    setContext(context);
    Assert.assertNotNull(context);
    File exportFile = new File(context.getFilesDir(), randomString());
    String content = TestUtils.readFixtureFile(resourcePath);
    FileOutputStream fos = new FileOutputStream(exportFile);
    fos.write(content.getBytes());
    fos.close();
    return exportFile;
  }
}
