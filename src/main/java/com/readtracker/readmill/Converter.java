package com.readtracker.readmill;

import com.readtracker.db.LocalHighlight;
import com.readtracker.db.LocalReading;
import com.readtracker.db.LocalSession;
import org.json.JSONException;
import org.json.JSONObject;

import static com.readtracker.readmill.ReadmillApiHelper.*;

/**
 * Converts data from Readmill to ReadTracker and vice versa.
 */
public class Converter {
  public static final String TAG = Converter.class.getName();

  /**
   * Creates a LocalReading instance from a Readmill reading JSON.
   * <p/>
   * The JSON must follow the format specified by:
   * http://developer.readmill.com/api/docs/v2/get/readings/id.html
   *
   * @param source source json
   * @return the created LocalReading instance
   * @throws JSONException if source was not in the required format
   */
  public static LocalReading createLocalReadingFromReadingJSON(JSONObject source) throws JSONException {
    LocalReading localReading = new LocalReading();
    mergeWithJSON(localReading, source);
    return localReading;
  }

  /**
   * Merges a LocalReading instance with information from a Readmill reading JSON.
   * <p/>
   * The JSON must follow the format specified by:
   * http://developer.readmill.com/api/docs/v2/get/readings/id.html
   *
   * @param localReading the local reading to merge into
   * @param source       source json
   * @throws org.json.JSONException if source was not in the required format
   */
  public static void mergeWithJSON(LocalReading localReading, JSONObject source) throws JSONException {
    guardNullSource(source);
    JSONObject jsonBook = source.getJSONObject("book");
    JSONObject jsonUser = source.getJSONObject("user");

    localReading.readmillReadingId = source.getLong("id");
    localReading.readmillTouchedAt = parseISO8601ToUnix(source.getString("touched_at"));
    localReading.readmillState = toIntegerState(source.getString("state"));
    localReading.progress = source.getDouble("progress");
    localReading.lastReadAt = localReading.readmillTouchedAt;
    localReading.readmillClosingRemark = source.getString("closing_remark");

    // Extract book
    localReading.readmillBookId = jsonBook.getLong("id");
    localReading.title = jsonBook.optString("title", "Unknown title");
    localReading.author = jsonBook.optString("author", "Unknown author");
    localReading.coverURL = jsonBook.getString("cover_url");

    // Extract user
    localReading.readmillUserId = jsonUser.getLong("id");
  }

  /**
   * Creates a ReadingHighlight instance from a Readmill highlight JSON.
   * <p/>
   * The JSON must follow the format specified by:
   * http://developer.readmill.com/api/docs/v2/get/highlights/id.html
   *
   * @param source source json
   * @return the created ReadingHighlight
   * @throws JSONException if source was not in the required format
   */
  public static LocalHighlight createHighlightFromReadmillJSON(JSONObject source) throws JSONException {
    guardNullSource(source);
    LocalHighlight target = new LocalHighlight();
    target.content = source.getString("content");
    target.readmillHighlightId = source.getLong("id");
    target.readmillReadingId = source.getJSONObject("reading").getLong("id");
    target.readmillUserId = source.getJSONObject("user").getLong("id");
    target.highlightedAt = parseISO8601(source.getString("highlighted_at"));
    target.readmillPermalinkUrl = source.getString("permalink_url");
    target.position = source.optDouble("position", 0.0);

    return target;
  }

  /**
   * Creates a ReadingSession instance from a Readmill Reading Period.
   *
   * @param source the json object representing the readmill period
   * @return the ReadingSession instance
   * @throws JSONException if source was not in the required format
   */
  public static LocalSession createReadingSessionFromReadmillPeriod(JSONObject source) throws JSONException {
    guardNullSource(source);
    LocalSession session = new LocalSession();
    session.durationSeconds = source.getLong("duration");
    session.occurredAt = parseISO8601(source.getString("started_at"));
    session.progress = source.getDouble("progress");
    session.readmillReadingId = source.getJSONObject("reading").getLong("id");
    session.sessionIdentifier = source.getString("identifier");

    return session;
  }

  /**
   * Guard against the argument being null by throwing an exception
   *
   * @param jsonObject The object to guard against null
   * @throws JSONException if the object is null
   */
  private static void guardNullSource(JSONObject jsonObject) throws JSONException {
    if(jsonObject == null) {
      throw new JSONException("Received NULL object");
    }
  }
}
