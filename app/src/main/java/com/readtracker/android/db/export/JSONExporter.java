package com.readtracker.android.db.export;

import android.app.Activity;
import android.os.Environment;
import android.util.Log;

import com.readtracker.android.ReadTrackerApp;
import com.readtracker.android.db.Book;
import com.readtracker.android.db.DatabaseManager;
import com.readtracker.android.db.Quote;
import com.readtracker.android.db.Session;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

/** Serializer and deserializer of model data to and from JSON. */
public class JSONExporter {
  public static final int FORMAT_VERSION = 2;

  private static final String TAG = JSONExporter.class.getSimpleName();

  private static final String DEFAULT_EXPORT_FILENAME = "readtracker.json";
  private static final File DEFAULT_EXPORT_DIRECTORY =
      Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);

  private final DatabaseManager mDatabaseMgr;

  public static JSONExporter from(Activity activity) {
    return new JSONExporter(((ReadTrackerApp) activity.getApplication()).getDatabaseManager());
  }

  JSONExporter(DatabaseManager db) {
    mDatabaseMgr = db;
  }

  /**
   * Exports all model data to the default path.
   *
   * @return the written file if successfully exported, <code>null</code> otherwise.
   */
  public File exportToDisk() {
    List<Book> books = mDatabaseMgr.getAll(Book.class);
    File destination = new File(DEFAULT_EXPORT_DIRECTORY, DEFAULT_EXPORT_FILENAME);
    if(!destination.exists()) {
      // Some older devices doesn't seem to have a Download dir by default, so fallback to just the
      // external storage directory.
      destination = new File(Environment.getExternalStorageDirectory(), DEFAULT_EXPORT_FILENAME);
    }

    if(exportToDisk(books, destination)) {
      return destination;
    }

    return null;
  }

  /**
   * Exports all model data to a file.
   *
   * @return true if exported, false otherwise.
   */
  public boolean exportToDisk(List<Book> books, File outputFile) {
    try {
      final String jsonData = exportAll(books).toString();
      FileOutputStream fos = new FileOutputStream(outputFile);
      fos.write(jsonData.getBytes());
      fos.close();
      return true;
    } catch(JSONException ex) {
      Log.w(TAG, "Failed to export JSON data", ex);
    } catch(FileNotFoundException ex) {
      Log.w(TAG, "Failed to export JSON data", ex);
    } catch(IOException ex) {
      Log.w(TAG, "Failed to export JSON data", ex);
    }

    return false;
  }

  /** Exports all books as a JSON object. */
  JSONObject exportAll(List<Book> books) throws JSONException {
    JSONObject export = new JSONObject();

    JSONArray exportedBooks = new JSONArray();

    for(Book book : books) {
      book.loadQuotes(mDatabaseMgr);
      book.loadSessions(mDatabaseMgr);
      JSONObject exportedBook = exportCompleteBook(book);
      exportedBooks.put(exportedBook);
    }

    export.put("books", exportedBooks);
    export.put("format_version", FORMAT_VERSION);

    return export;
  }

  private JSONObject exportCompleteBook(Book book) throws JSONException {
    JSONObject json = createSingleBookJson(book);

    json.put("sessions", createSessionListJson(book.getSessions()));
    json.put("quotes", createQuoteListJson(book.getQuotes()));

    return json;
  }

  JSONArray createSessionListJson(List<Session> sessions) throws JSONException {
    JSONArray sessionsJson = new JSONArray();
    for(Session session : sessions) {
      sessionsJson.put(createSingleSessionJson(session));
    }
    return sessionsJson;
  }

  JSONArray createQuoteListJson(List<Quote> quotes) throws JSONException {
    JSONArray quotesJson = new JSONArray();
    for(Quote quote : quotes) {
      quotesJson.put(createSingleQuoteJson(quote));
    }
    return quotesJson;
  }

  JSONObject createSingleBookJson(Book book) throws JSONException {
    JSONObject json = new JSONObject();
    json.put(Book.Columns.TITLE, book.getTitle());
    json.put(Book.Columns.AUTHOR, book.getAuthor());
    json.put(Book.Columns.COVER_IMAGE_URL, book.getCoverImageUrl());
    json.put(Book.Columns.PAGE_COUNT, book.getPageCount());
    json.put(Book.Columns.CURRENT_POSITION, book.getCurrentPosition());
    json.put(Book.Columns.CURRENT_POSITION_TIMESTAMP, book.getCurrentPositionTimestampMs());
    json.put(Book.Columns.FIRST_POSITION_TIMESTAMP, book.getFirstPositionTimestampMs());
    json.put(Book.Columns.CLOSING_REMARK, book.getClosingRemark());

    String stateName = book.getState() == null ? null : book.getState().toString();
    json.put(Book.Columns.STATE, stateName);

    return json;
  }

  JSONObject createSingleSessionJson(Session session) throws JSONException {
    JSONObject json = new JSONObject();
    json.put(Session.Columns.START_POSITION, session.getStartPosition());
    json.put(Session.Columns.END_POSITION, session.getEndPosition());
    json.put(Session.Columns.DURATION_SECONDS, session.getDurationSeconds());
    json.put(Session.Columns.TIMESTAMP, session.getTimestampMs());
    return json;
  }

  JSONObject createSingleQuoteJson(Quote quote) throws JSONException {
    JSONObject json = new JSONObject();
    json.put(Quote.Columns.CONTENT, quote.getContent());
    json.put(Quote.Columns.ADD_TIMESTAMP, quote.getAddTimestampMs());
    json.put(Quote.Columns.POSITION, quote.getPosition());
    return json;
  }
}
