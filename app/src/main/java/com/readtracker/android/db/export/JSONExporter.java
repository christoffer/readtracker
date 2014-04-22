package com.readtracker.android.db.export;

import com.readtracker.android.db.Book;
import com.readtracker.android.db.DatabaseManager;
import com.readtracker.android.db.Quote;
import com.readtracker.android.db.Session;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

/** Serializer and deserializer of model data to and from JSON. */
public class JSONExporter {
  private final DatabaseManager mDatabaseMgr;

  public JSONExporter(DatabaseManager db) {
    mDatabaseMgr = db;
  }

  /** Exports all model data as a JSON string. */
  public String exportAll() throws JSONException {
    List<Book> books = mDatabaseMgr.getAll(Book.class);
    StringBuilder json = new StringBuilder();

    for(Book book : books) {
      book.loadQuotes(mDatabaseMgr);
      book.loadSessions(mDatabaseMgr);
      json.append(exportCompleteBook(book));
    }

    return json.toString();
  }

  private String exportCompleteBook(Book book) throws JSONException {
    JSONObject json = createSingleBookJson(book);
    json.put("sessions", createSessionListJson(book.getSessions()));
    json.put("quotes", createQuoteListJson(book.getQuotes()));

    return json.toString();
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