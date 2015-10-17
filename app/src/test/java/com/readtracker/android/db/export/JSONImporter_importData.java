package com.readtracker.android.db.export;

import android.content.Context;
import android.test.mock.MockContext;

import com.readtracker.android.db.Book;
import com.readtracker.android.db.Quote;
import com.readtracker.android.db.Session;
import com.readtracker.android.test_support.DatabaseTestCase;
import com.readtracker.android.test_support.TestUtils;

import junit.framework.Assert;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static com.readtracker.android.test_support.TestUtils.randomString;

public class JSONImporter_importData extends DatabaseTestCase {
  private JSONImporter getImporter() {
    return new JSONImporter(getDatabaseManager());
  }

  public void test_import_version_1_file() throws Exception, ImportException {
    File fileToImport = _copyResourceFile("export_version_1.json");

    assertEquals(0, getDatabaseManager().getAll(Book.class).size());
    getImporter().importFile(fileToImport);

    _assertImportedBooks();
  }

  public void test_import_version_2_file() throws Exception, ImportException {
    File fileToImport = _copyResourceFile("export_version_2.json");

    assertEquals(0, getDatabaseManager().getAll(Book.class).size());
    getImporter().importFile(fileToImport);

    _assertImportedBooks();
  }

  public void test_import_with_no_merge_conflict() throws Exception, ImportException {
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

  public void test_import_with_simple_merge_conflict() throws Exception, ImportException {
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

  public void test_import_with_nested_merge_conflicts() throws Exception, ImportException {
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

  private File _createExportFileForBooks(List<Book> books) throws Exception {
    Context context = new MockContext();
    setContext(context);
    Assert.assertNotNull(context);
    File exportFile = new File(context.getFilesDir(), randomString());
    String content = new JSONExporter(getDatabaseManager()).exportAll(books).toString();
    FileOutputStream fos = new FileOutputStream(exportFile);
    fos.write(content.getBytes());
    fos.close();
    return exportFile;
  }

  private void _assertImportedBooks() {
    List<Book> booksInDatabase = getDatabaseManager().getAll(Book.class);
    for(Book book : booksInDatabase) {
      book.loadSessions(getDatabaseManager());
      book.loadQuotes(getDatabaseManager());
    }

    SharedExampleAsserts.assertExampleBooksVersion2(booksInDatabase);
  }

  private File _copyResourceFile(String resourcePath) throws IOException {
    Context context = new MockContext();
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
