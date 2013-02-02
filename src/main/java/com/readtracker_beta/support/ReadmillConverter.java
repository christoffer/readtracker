package com.readtracker_beta.support;

import com.readtracker_beta.db.LocalHighlight;
import com.readtracker_beta.db.LocalReading;
import com.readtracker_beta.db.LocalSession;
import org.json.JSONException;
import org.json.JSONObject;

import static com.readtracker_beta.support.ReadmillApiHelper.*;

/**
 * Converts data from Readmill to ReadTracker and vice versa.
 */
public class ReadmillConverter {
  public static final String TAG = ReadmillConverter.class.getName();

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
    mergeLocalReadingWithJSON(localReading, source);
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
  public static void mergeLocalReadingWithJSON(LocalReading localReading, JSONObject source) throws JSONException {
    guardNullSource(source);
    JSONObject jsonBook = source.getJSONObject("book");
    JSONObject jsonUser = source.getJSONObject("user");

    localReading.readmillReadingId = source.getLong("id");
    localReading.readmillTouchedAt = parseISO8601ToUnix(source.getString("touched_at"));
    localReading.readmillState = toIntegerState(source.getString("state"));
    localReading.progress = source.getDouble("progress");
    localReading.lastReadAt = localReading.readmillTouchedAt;
    localReading.readmillPrivate = source.getBoolean("private");

    localReading.readmillClosingRemark = optString("closing_remark", null, source);

    if(localReading.locallyClosedAt == 0) {
      localReading.locallyClosedAt = parseISO8601ToUnix(source.getString("ended_at"));
    }

    // Extract book
    localReading.readmillBookId = jsonBook.getLong("id");
    localReading.title = optString("title", "Unknown title", jsonBook);
    localReading.author = optString("author", "Unknown author", jsonBook);
    localReading.coverURL = optString("cover_url", null, jsonBook);

    // Extract user
    localReading.readmillUserId = jsonUser.getLong("id");
  }

  /**
   * Merge an instance of a LocalHighlight with a Readmill highlight JSON.
   * <p/>
   * The JSON must follow the format specified by:
   * http://developer.readmill.com/api/docs/v2/get/highlights/id.html
   *
   * @param target The local highlight to merge with
   * @param source the Readmill highlight data to merge in
   * @throws JSONException if source is not in the required format
   */
  public static void mergeLocalHighlightWithJson(LocalHighlight target, JSONObject source) throws JSONException {
    guardNullSource(source);
    target.content = source.getString("content");
    target.readmillHighlightId = source.getLong("id");
    target.readmillReadingId = source.getJSONObject("reading").getLong("id");
    target.readmillUserId = source.getJSONObject("user").getLong("id");
    target.highlightedAt = parseISO8601(source.getString("highlighted_at"));
    target.readmillPermalinkUrl = source.getString("permalink_url");
    target.position = source.optDouble("position", 0.0);
    target.likeCount = source.optInt("likes_count", 0);
    target.commentCount = source.optInt("comments_count", 0);
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
    LocalHighlight target = new LocalHighlight();
    mergeLocalHighlightWithJson(target, source);
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
   * Work around to get a string response from JSON, avoiding the
   * (intentional) JSON string bug.
   *
   * Use this method instead of JSObject.optString()
   *
   * @link http://code.google.com/p/android/issues/detail?id=13830
    */
  public static String optString(String key, String opt, JSONObject source) throws JSONException {
    return source.isNull(key) ? opt : source.getString(key);
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
