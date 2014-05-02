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
  private static final String TAG = JSONExporter.class.getSimpleName();

  private static final String DEFAULT_EXPORT_FILENAME = "readtracker.json";

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
    File outputDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
    final File exportFile = new File(outputDir, DEFAULT_EXPORT_FILENAME);
    if(exportToDisk(exportFile)) {
      return exportFile;
    }
    return null;
  }

  /**
   * Exports all model data to a file.
   *
   * @return true if exported, false otherwise.
   */
  public boolean exportToDisk(File outputFile) {
    try {
      final String jsonData = exportAll();
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

  /** Exports all model data as a JSON string. */
  private String exportAll() throws JSONException {
    List<Book> books = mDatabaseMgr.getAll(Book.class);
    StringBuilder jsonSB = new StringBuilder();

    for(Book book : books) {
      book.loadQuotes(mDatabaseMgr);
      book.loadSessions(mDatabaseMgr);
      jsonSB.append(exportCompleteBook(book));
    }

    return jsonSB.toString();
  }

  private String exportCompleteBook(Book book) throws JSONException {
    JSONObject json = createSingleBookJson(book);
    json.put("sessions", createSessionListJson(book.getSessions()));
    json.put("quotes", createQuoteListJson(book.getQuotes()));

    return json.toString(4);
  }

  private JSONArray createSessionListJson(List<Session> sessions) throws JSONException {
    JSONArray sessionsJson = new JSONArray();
    for(Session session : sessions) {
      sessionsJson.put(createSingleSessionJson(session));
    }
    return sessionsJson;
  }

  private JSONArray createQuoteListJson(List<Quote> quotes) throws JSONException {
    JSONArray quotesJson = new JSONArray();
    for(Quote quote : quotes) {
      quotesJson.put(createSingleQuoteJson(quote));
    }
    return quotesJson;
  }

  private JSONObject createSingleBookJson(Book book) throws JSONException {
    JSONObject json = new JSONObject();
    json.put(Book.Columns.TITLE, book.getTitle());
    json.put(Book.Columns.AUTHOR, book.getAuthor());
    json.put(Book.Columns.COVER_IMAGE_URL, book.getCoverImageUrl());
    json.put(Book.Columns.PAGE_COUNT, book.getPageCount());
    json.put(Book.Columns.STATE, book.getState());
    json.put(Book.Columns.CURRENT_POSITION, book.getCurrentPosition());
    json.put(Book.Columns.CURRENT_POSITION_TIMESTAMP, book.getCurrentPositionTimestampMs());
    json.put(Book.Columns.FIRST_POSITION_TIMESTAMP, book.getFirstPositionTimestampMs());
    json.put(Book.Columns.CLOSING_REMARK, book.getClosingRemark());
    return json;
  }

  private JSONObject createSingleSessionJson(Session session) throws JSONException {
    JSONObject json = new JSONObject();
    json.put(Session.Columns.START_POSITION, session.getStartPosition());
    json.put(Session.Columns.END_POSITION, session.getEndPosition());
    json.put(Session.Columns.DURATION_SECONDS, session.getDurationSeconds());
    json.put(Session.Columns.TIMESTAMP, session.getTimestampMs());
    return json;
  }

  private JSONObject createSingleQuoteJson(Quote quote) throws JSONException {
    JSONObject json = new JSONObject();
    json.put(Quote.Columns.CONTENT, quote.getContent());
    json.put(Quote.Columns.ADD_TIMESTAMP, quote.getAddTimestampMs());
    json.put(Quote.Columns.POSITION, quote.getPosition());
    return json;
  }
}