package com.readtracker.android.db.export;

import com.readtracker.android.db.Book;
import com.readtracker.android.db.Quote;
import com.readtracker.android.db.Session;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/** Parser for version 2 of the Export file format. */
public class ExportedFileParser {

  /** Returns a list of all books in the import JSON file. */
  public List<Book> parse(String fileContent) throws JSONException {
    JSONObject jsonRoot = new JSONObject(fileContent);
    List<Book> books = new ArrayList<Book>();
    JSONArray jsonBooks = jsonRoot.getJSONArray("books");

    for(int index = 0; index < jsonBooks.length(); index++) {
      JSONObject jsonBookObject = jsonBooks.getJSONObject(index);
      Book importedBook = buildBookFromJSON(jsonBookObject);
      books.add(importedBook);
    }

    return books;
  }

  private Book buildBookFromJSON(JSONObject jsonObject) throws JSONException {
    Book book = new Book();
    JsonWrapper json = new JsonWrapper(jsonObject);

    book.setTitle(json.getString("title"));
    book.setAuthor(json.getString("author"));
    book.setCurrentPosition(json.getFloatOrZero("current_position"));
    book.setCurrentPositionTimestampMs(json.getLong("current_position_timestamp"));
    book.setFirstPositionTimestampMs(json.getLong("first_position_timestamp"));
    book.setPageCount(json.getFloat("page_count"));
    book.setCoverImageUrl(json.getString("cover_image_url"));
    book.setClosingRemark(json.getString("closing_remark"));

    String stateName = json.getString("state");
    book.setState(stateName == null ? Book.State.Unknown : Book.State.valueOf(stateName));

    JSONArray quotes = json.getArray("quotes");
    List<Quote> quotesInBook = book.getQuotes();
    for(int i = 0; i < quotes.length(); i++) {
      JSONObject jsonQuote = quotes.getJSONObject(i);
      Quote quote = buildQuoteFromJSON(book, jsonQuote);
      quotesInBook.add(quote);
    }

    JSONArray sessions = json.getArray("sessions");
    List<Session> sessionsInBook = book.getSessions();
    for(int i = 0; i < sessions.length(); i++) {
      JSONObject jsonSession = sessions.getJSONObject(i);
      Session session = buildSessionFromJSON(book, jsonSession);
      sessionsInBook.add(session);
    }

    return book;
  }

  private Session buildSessionFromJSON(Book book, JSONObject jsonSession) {
    Session session = new Session();
    JsonWrapper wrapper = new JsonWrapper(jsonSession);

    session.setBook(book);
    session.setTimestampMs(wrapper.getLongOrZero("timestamp"));
    session.setStartPosition(wrapper.getFloatOrZero("start_position"));
    session.setEndPosition(wrapper.getFloatOrZero("end_position"));
    session.setDurationSeconds(wrapper.getLongOrZero("duration_seconds"));

    return session;
  }

  private Quote buildQuoteFromJSON(Book book, JSONObject jsonQuote) {
    Quote quote = new Quote();
    JsonWrapper wrapper = new JsonWrapper(jsonQuote);

    quote.setBook(book);
    quote.setContent(wrapper.getString("content"));
    quote.setAddTimestampMs(wrapper.getLong("add_timestamp"));
    quote.setPosition(wrapper.getFloat("position"));

    return quote;
  }

  /** Provides some customized field handling we want for json parsing. */
  private static class JsonWrapper {
    private final JSONObject mJsonObj;

    public JsonWrapper(JSONObject jsonObject) {
      mJsonObj = jsonObject;
    }

    /** Returns null if the field is missing or null, otherwise the value */
    private Long getLong(String key) {
      if(mJsonObj.has(key) && !mJsonObj.isNull(key)) {
        return mJsonObj.optLong(key);
      } else {
        return null;
      }
    }

    /** Returns null if the field is missing or null, otherwise the value */
    private Long getLongOrZero(String key) {
      return mJsonObj.optLong(key);
    }

    /** Get JSON null values correctly for String fields. */
    private String getString( String key) {
      String value = mJsonObj.optString(key, null);
      return value == null || value.equals("null") ? null : value;
    }

    /** Return null for float as null, not NaN */
    private Float getFloat(String key) {
      double value = mJsonObj.optDouble(key);
      if(Double.isNaN(value)) {
        return null;
      }
      return (float) value;
    }

    /** Return 0 for float as null, not NaN */
    private float getFloatOrZero(String key) {
      double value = mJsonObj.optDouble(key);
      if(Double.isNaN(value)) {
        return 0f;
      }
      return (float) value;
    }

    /** Non null fetching of arrays */
    public JSONArray getArray(String key) {
      JSONArray array = mJsonObj.optJSONArray(key);
      return array == null ? new JSONArray() : array;
    }
  }
}
