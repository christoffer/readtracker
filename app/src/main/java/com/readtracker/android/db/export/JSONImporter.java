package com.readtracker.android.db.export;

import android.util.Log;

import com.readtracker.android.db.Book;
import com.readtracker.android.db.DatabaseManager;
import com.readtracker.android.db.Quote;
import com.readtracker.android.db.Session;
import com.readtracker.android.support.Utils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class JSONImporter {
  private static final String TAG = JSONImporter.class.getSimpleName();
  private final DatabaseManager mDatabaseManager;
  private final ProgressListener mProgressListener;

  public interface ProgressListener {
    void onProgressUpdate(int currentBook, int totalBooks);
  }

  public JSONImporter(DatabaseManager databaseManager, ProgressListener listener) {
    mDatabaseManager = databaseManager;
    mProgressListener = listener;
  }

  public static class ImportResultReport {
    public int mergedBookCount = 0;
    public int createdBookCount = 0;
    public int createdQuotesCount = 0;
    public int createdSessionCount = 0;

    @Override public String toString() {
      return String.format(Locale.getDefault(),
          "merged books: %d, created books: %d, created quotes: %d, created sessions: %d",
          mergedBookCount, createdBookCount, createdQuotesCount, createdSessionCount
      );
    }
  }

  /**
   * Imports a previously a exported data file from any previous version of ReadTracker.
   * Returns a ImportResultsReport.
   */
  public ImportResultReport importFile(File importFile) throws IOException, ImportException {
    String fileContent = Utils.readInputFile(importFile);
    int formatVersion = getFormatVersion(fileContent);

    if(formatVersion == 1) {
      return importFromVersion1(fileContent);
    } else if(formatVersion == 2) {
      return importFromVersion2(fileContent);
    } else {
      throw new UnexpectedImportDataFormatException("Unknown format version");
    }
  }

  /**
   * Initial format has broken JSON syntax for lists of books. Instead of being a list of books,
   * it's just a concatenation of single book json objects.
   * <code>
   * { "title": "Metamorphosis", ... }{ "title": "Game of Thrones", ... }
   * </code>
   */
  private ImportResultReport importFromVersion1(String fileContent) throws ImportException {
    // Convert version 1 to version 2 and use version 2 importer
    fileContent = fileContent.replaceAll("[}][{]", "}, {");
    fileContent = String.format("{ \"format_version\": 2, \"books\": [%s] }", fileContent);
    return importFromVersion2(fileContent);
  }

  /**
   * Version 2 is very similar to version 1, but has a correct JSON wrapped around the objects:
   * <code>
   * {
   * "format_version": 2,
   * "books": [
   *   { "title": "Metamorphosis", ... },
   *   { "title": "Game of Thrones", ... }
   * ]
   * }
   * </code>
   */
  private ImportResultReport importFromVersion2(String fileContent) throws ImportException {
    try {
      ExportedFileParser fileParser = new ExportedFileParser();
      List<Book> booksToImport = fileParser.parse(fileContent);
      return importAndMergeBooks(booksToImport);
    } catch(JSONException e) {
      final String message = String.format("Unknown import format error: %s", e.getMessage());
      throw new UnexpectedImportDataFormatException(message);
    }
  }

  /** Merges a list of books with the current books in the database. */
  private ImportResultReport importAndMergeBooks(List<Book> booksToImport) {
    List<Book> existingBooks = mDatabaseManager.getAll(Book.class);
    ImportResultReport report = new ImportResultReport();
    final int numBooksToImport = booksToImport.size();
    for(int i = 0; i < numBooksToImport; i++) {
      Book bookToImport = booksToImport.get(i);
      Book bookToPersist;
      mProgressListener.onProgressUpdate(i, numBooksToImport);

      if(existingBooks.contains(bookToImport)) {
        Book existingBook = existingBooks.get(existingBooks.indexOf(bookToImport));
        existingBook.merge(bookToImport);
        report.createdQuotesCount += importMissingQuotes(mDatabaseManager, existingBook, bookToImport.getQuotes());
        report.createdSessionCount += importMissingSession(mDatabaseManager, existingBook, bookToImport.getSessions());
        bookToPersist = existingBook;
        report.mergedBookCount += 1;
      } else {
        bookToPersist = bookToImport;
        report.createdQuotesCount += bookToPersist.getSessions().size();
        report.createdSessionCount += bookToPersist.getQuotes().size();
        report.createdBookCount += 1;
      }

      mDatabaseManager.save(bookToPersist);
      mDatabaseManager.saveAll(bookToPersist.getSessions());
      mDatabaseManager.saveAll(bookToPersist.getQuotes());
    }

    return report;
  }

  /**
   * Imports a list of Quotes into a Book, skipping existing entries.
   * Returns the number of imported Quotes.
   */
  private long importMissingQuotes(DatabaseManager dbManager, Book book, List<Quote> quotesToImport) {
    book.loadQuotes(dbManager); // make sure the book has all it's quotes loaded
    List<Quote> currentQuotes = book.getQuotes();
    long importedQuotes = 0;
    for(Quote candidate : quotesToImport) {
      candidate.setBook(book); // needed for equality check
      if(!currentQuotes.contains(candidate)) {
        Quote spawn = new Quote();
        spawn.setBook(book);
        spawn.merge(candidate);
        dbManager.save(spawn);
        currentQuotes.add(spawn);
        importedQuotes += 1;
      } else {
        Log.d(TAG, String.format("Skipping %s (duplicate)", quotesToImport));
      }
    }

    return importedQuotes;
  }

  /** Imports a list of Sessions into a Book, skipping existing entries. */
  private long importMissingSession(DatabaseManager dbManager, Book book, List<Session> otherSessions) {
    book.loadSessions(dbManager); // make sure the book has all it's quotes loaded
    List<Session> currentSessions = book.getSessions();
    long importedSessionCount = 0;
    for(Session candidate : otherSessions) {
      candidate.setBook(book); // needed for equality check
      if(!currentSessions.contains(candidate)) {
        Session spawn = new Session();
        spawn.setBook(book);
        spawn.merge(candidate);
        dbManager.save(spawn);
        currentSessions.add(spawn);
        importedSessionCount += 1;
      } else {
        Log.d(TAG, String.format("Skipping %s (duplicate)", candidate));
      }
    }

    return importedSessionCount;
  }

  private int getFormatVersion(String exportFileContent) throws UnexpectedImportDataFormatException {
    try {
      JSONObject data = new JSONObject(exportFileContent);
      if(data.has("format_version")) {
        return data.getInt("format_version");
      }

      // If the file is valid JSON (apparently version 1 JSON is, or at least the Android JSON lib
      // thinks so, even though it's not really well formed), the assume it is version 1 if it has
      // title and author in there.
      if(data.has("title") && data.has("author")) {
        return 1;
      }
    } catch(JSONException e) {
      // unknown file format
    }

    throw new UnexpectedImportDataFormatException("Failed to get format version from content");
  }
}
