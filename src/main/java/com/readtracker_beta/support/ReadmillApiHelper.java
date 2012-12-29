package com.readtracker_beta.support;

import android.util.Log;
import com.readmill.api.ReadmillWrapper;
import com.readmill.api.RequestBuilder;
import com.readmill.api.Token;
import com.readtracker_beta.db.LocalHighlight;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import static com.readtracker_beta.support.ReadmillApiHelper.ReadingState.ABANDONED;
import static com.readtracker_beta.support.ReadmillApiHelper.ReadingState.FINISHED;

/**
 * Class to bridge any interaction with the Readmill API.
 * <p/>
 * Maintains a current token as well as provides convenience methods
 * for common tasks.
 */
public class ReadmillApiHelper {
  private static final String TAG = ReadmillApiHelper.class.getName();

  private static final String ISO8601 = "yyyy-MM-dd'T'HH:mm:ss'Z'";
  private static final SimpleDateFormat iso8601Format = new SimpleDateFormat(ISO8601);

  ReadmillWrapper mWrapper;

  public ReadmillApiHelper(ReadmillWrapper wrapper) {
    mWrapper = wrapper;
  }

  // ===========================
  // Creating
  // ===========================

  /**
   * Creates a reading at Readmill for the given book.
   *
   * @param readmillBookId Book to create reading for
   * @param isPublic       true if the reading should be public, false if it should be private
   * @return The created reading (or an existing reading if it already existed)
   * @throws ReadmillException if the request to readmill was not successful
   */
  public JSONObject createReading(long readmillBookId, boolean isPublic) throws ReadmillException {
    Log.i(TAG, "Creating reading on Readmill for ReadmillBook with id:" + readmillBookId + " (" + (isPublic ? "public" : "private") + ")");

    String endpoint = String.format("/books/%d/readings", readmillBookId);
    RequestBuilder request = mWrapper.post(endpoint).
      readingPrivate(!isPublic).
      readingState("reading");
    JSONObject response = sendRequest(request, "create reading", 200, 201, 409); // 409 is a already existing reading – returns the reading

    // Unwrap
    JSONObject reading = response.optJSONObject("reading");

    // "Upgrade" the reading if it's in state "interesting"
    activateReading(reading);

    return reading;
  }

  /**
   * Creates a Book on Readmill
   *
   * @param title  Title of the book
   * @param author Author of the book
   * @return The created ReadmillBook or null
   * @throws ReadmillException if the request was not successful
   */
  public JSONObject createBook(String title, String author) throws ReadmillException {
    Log.i(TAG, "Creating book on readmill with title: " + title + ", and author: " + author);

    JSONObject book = null;
    RequestBuilder request = mWrapper.post("/books").bookTitle(title).bookAuthor(author);
    try {
      book = sendRequest(request, "create a book", 200, 201);
      return book.getJSONObject("book");
    } catch(JSONException e) {
      throw new ReadmillException("Unexpected JSON response: " + dumpJSON(book));
    }
  }

  /**
   * Creates a highlight on Readmill by copying a local highlight.
   *
   * @param localHighlight the local highlight to copy
   * @return the created highlight from Readmill
   * @throws ReadmillException if the request was not successful
   */
  public JSONObject createHighlight(LocalHighlight localHighlight) throws ReadmillException {
    Log.i(TAG, "Creating highlight on readmill for local highlight: " + localHighlight.toString());

    String endpoint = String.format("/readings/%d/highlights", localHighlight.readmillReadingId);
    RequestBuilder request = mWrapper.post(endpoint).
      highlightContent(localHighlight.content).
      highlightLocators(_buildLocator(localHighlight)).
      highlightHighlightedAt(localHighlight.highlightedAt);

    JSONObject remoteHighlight = null;
    try {
      remoteHighlight = sendRequest(request, "create a highlight", 200, 201).
        getJSONObject("highlight");
      Log.i(TAG, "Created remote highlight with id: " + remoteHighlight.getLong("id"));
      return remoteHighlight;
    } catch(JSONException e) {
      throw new ReadmillException("Unexpected JSON response: " + dumpJSON(remoteHighlight));
    }
  }

  /**
   * Tiny helper to construct a locator for a highlight.
   *
   * @param highlight highlight to construct locator for
   * @return the constructed locator or an empty JSON object
   */
  private JSONObject _buildLocator(LocalHighlight highlight) {
    try {
      JSONObject locators = new JSONObject();
      locators.put("mid", highlight.content);
      locators.put("position", highlight.position);
      return locators;
    } catch(JSONException e) {
      return new JSONObject();
    }
  }

  // ===========================
  // Updating
  // ===========================


  /**
   * Closes a reading on Readmill with an optional closingRemark.
   *
   * @param readmillReadingId Readmill id of reading to close
   * @param state             state to close with (finished or abandoned)
   * @param closingRemark     The closing remark for the book. Ignored if null.
   * @return true if the close was successful. False otherwise
   */
  public boolean closeReading(long readmillReadingId, int state, String closingRemark) {
    Log.i(TAG, "Closing reading with id " + readmillReadingId +
      " with state " + state + " and closing remark " + (closingRemark == null ? "" : closingRemark));

    String endpoint = String.format("/readings/%d", readmillReadingId);

    if(state != FINISHED && state != ReadingState.ABANDONED) {
      Log.w(TAG, "Got invalid state value: " + state + ". Accepting only " + ReadingState.FINISHED + " or " + ABANDONED);
      return false;
    }

    String stringState = getStringState(state);
    RequestBuilder request = mWrapper.put(endpoint).readingState(stringState);
    if(closingRemark != null) {
      request.readingClosingRemark(closingRemark);
    }
    try {
      sendRequest(request, "close reading", 200);
      return true;
    } catch(ReadmillException e) {
      Log.w(TAG, "Failed to update Reading", e);
      return false;
    }
  }

  /**
   * Deletes a reading on Readmill by id.
   *
   * @param readmillReadingId Readmill ID of reading to delete.
   * @return true if the reading was deleted, false otherwise.
   */
  public boolean deleteReading(long readmillReadingId) {
    return false;
  }

  /**
   * Creates a ping on Readmill for a given session and reading.
   * Pings are used to update progress of Readmill readings. A session
   * identifier identifies which reading session the ping is for
   * (how many "times" a user has read in a book).
   *
   * @param sessionIdentifier identifier for ping, used to group several pings
   *                          into a reading session. A new reading session is
   *                          created for each unique identifier.
   * @param readmillReadingId the readmill reading to ping
   * @param progress          how far into the book the user is at the time of the ping
   * @param durationSeconds   durations, in seconds, since the last ping
   * @param occurredAt        when the ping started
   * @throws ReadmillException if the request was not sent successfully
   */
  public void createPing(String sessionIdentifier,
                         long readmillReadingId,
                         double progress,
                         long durationSeconds,
                         Date occurredAt) throws ReadmillException {
    Log.i(TAG, "Pinging reading with id " + readmillReadingId);

    String endpoint = String.format("/readings/%d/ping", readmillReadingId);

    Log.d(TAG, "PING endpoint: " + endpoint);

    RequestBuilder request = mWrapper.post(endpoint).
      pingDuration(durationSeconds).
      pingProgress(progress).
      pingIdentifier(sessionIdentifier).
      pingOccurredAt(occurredAt);

    sendRequest(request, "create reading", 201);
  }

  // ===========================
  // Reading
  // ===========================

  /**
   * Gets the user for the current token (GET /me).
   *
   * @return The current user, or null if an error occurred, or the token
   *         is not set
   */
  public JSONObject getCurrentUser() {
    Log.d(TAG, "Fetching current user");

    RequestBuilder request = mWrapper.get("/me");

    JSONObject response = null;
    try {
      response = sendRequest(request, "fetch current user", 200);
      return response.getJSONObject("user");
    } catch(ReadmillException e) {
      Log.w(TAG, "Failed to fetch current user", e);
    } catch(JSONException e) {
      Log.w(TAG, "Unexpected response from Readmill: " + dumpJSON(response));
    }

    return null;
  }

  /**
   * Collects the list of ReadingPeriods for a given ReadmillReading.
   * <p/>
   * Currently only supports getting the latest 100 periods.
   *
   * @param readmillReadingId ID of Readmill reading to fetch periods for
   * @return The list of ReadingPeriods for the given reading id
   */
  public ArrayList<JSONObject> getPeriodsForReadingId(long readmillReadingId) {
    Log.d(TAG, "Fetching list of Readmill Reading Periods for Readmill Reading with id: " + readmillReadingId);
    String endpoint = String.format("/readings/%d/periods", readmillReadingId);
    RequestBuilder request = mWrapper.get(endpoint).count(100);
    ArrayList<JSONObject> result = new ArrayList<JSONObject>();

    JSONObject items = null;
    try {
      items = sendRequest(request, "fetch reading periods", 200);
      JSONArray periods = items.getJSONArray("items");
      for(int i = 0; i < periods.length(); i++) {
        result.add(periods.getJSONObject(i).getJSONObject("period"));
      }
      return result;
    } catch(ReadmillException e) {
      Log.w(TAG, "Failed to fetch list of reading periods", e);
    } catch(JSONException e) {
      Log.w(TAG, "Unexpected response from Readmill: " + dumpJSON(items), e);
    }
    return null;
  }

  /**
   * Gets all readings for a given user id. Only fetches readings that are
   * in state "reading", "finished" or "abandoned"
   *
   * @param userId Readmill user id to fetch readings for
   * @return the list of readings or null
   * @throws ReadmillException if Readmill request was not successful
   */
  public ArrayList<JSONObject> getReadingsForUserId(long userId) throws ReadmillException {
    Log.d(TAG, "Fetching list of Readmill Readings for Readmill User with id: " + userId);
    ArrayList<JSONObject> result = new ArrayList<JSONObject>();

    String endpoint = String.format("/users/%d/readings", userId);
    RequestBuilder request = mWrapper.get(endpoint).count(100).states("reading,finished,abandoned");

    JSONObject collection = null;
    try {
      collection = sendRequest(request, "fetch readings", 200);
      Log.v(TAG, collection.toString());
      JSONArray readings = collection.getJSONArray("items");
      for(int i = 0; i < readings.length(); i++) {
        result.add(readings.getJSONObject(i).getJSONObject("reading"));
      }
      return result;
    } catch(JSONException e) {
      Log.w(TAG, "Unexpected response from Readmill: " + dumpJSON(collection), e);
    }
    return null;
  }

  /**
   * Fetches a list of highlights for a given Readmill reading.
   *
   * @param readmillReadingId The id of the reading to fetch highlights for
   * @return The list of highlights
   */
  public ArrayList<JSONObject> getHighlightsWithReadingId(long readmillReadingId) {
    Log.d(TAG, "Fetching list of Readmill Highlights for Readmill Reading with id: " + readmillReadingId);

    ArrayList<JSONObject> result = new ArrayList<JSONObject>();

    String endpoint = String.format("/readings/%d/highlights", readmillReadingId);
    RequestBuilder request = mWrapper.get(endpoint).count(100);

    try {
      JSONObject collection = sendRequest(request, "fetch reading highlights", 200);
      Log.v(TAG, collection.toString());
      JSONArray highlights = collection.getJSONArray("items");
      if(highlights != null) {
        for(int i = 0; i < highlights.length(); i++) {
          result.add(highlights.getJSONObject(i).getJSONObject("highlight"));
        }
      }
      return result;
    } catch(ReadmillException e) {
      Log.w(TAG, "Failed to fetch list of reading highlights", e);
    } catch(JSONException e) {
      Log.w(TAG, "Unexpected response from Readmill", e);
    }
    return null;
  }

  // ===========================
  // Authorization
  // ===========================


  /**
   * Exchanges an authorization code for a token.
   * <p/>
   * Fires the tokenChanged event if the exchange was successful and a new token
   * was acquired.
   *
   * @param code Authorization code to exchange for a token
   * @return true if the exchange was successful and a token was acquired,
   *         false otherwise
   */
  public boolean authorize(String code) {
    try {
      Token accessToken = mWrapper.obtainTokenOrThrow(code);
      mWrapper.setToken(accessToken);
      return true;
    } catch(IOException e) {
      Log.w(TAG, "Failed to obtain token", e);
    } catch(JSONException e) {
      Log.w(TAG, "Failed to obtain token", e);
    }
    return false;
  }

  /**
   * Construct a url where the user can authorize the application
   *
   * @return the authorization url
   */
  public String authorizeUrl() {
    return mWrapper.getAuthorizationURL().toString();
  }

  /**
   * Get the url where the user can create an account
   *
   * @return the url for creating an account
   */
  public String createAccountUrl() {
    return mWrapper.getAuthorizationURL().toString();
  }


  // =======
  // HELPERS
  // =======

  /**
   * Activates a reading that is in "interesting" state by pushing it to the
   * "reading" state.
   * <p/>
   * Nothing is done if the reading is not currently in state "interesting".
   *
   * @param reading Reading to activate.
   * @throws ReadmillException if the update request to Readmill was made and
   *                           failed
   */
  private void activateReading(JSONObject reading) throws ReadmillException {
    if(reading == null || !reading.optString("state", "").equals("interesting")) {
      return;
    }

    try {
      Log.d(TAG, "Readmill replied with interesting reading. Attempting upgrade to 'reading'");
      String endpoint = String.format("/readings/%d", reading.getInt("id"));
      RequestBuilder request = mWrapper.put(endpoint).readingState("reading");
      sendRequest(request, "update reading to 'reading' state", 200);
    } catch(JSONException e) {
      fail("get reading id", e);
    }
  }

  /**
   * Wraps a request in a common error handling.
   *
   * @param request            The request to wrap
   * @param actionDescription  A short description used to build error messages
   * @param allowedStatusCodes (optional) A white list of status code that are
   *                           accepted as a successful result. If none are
   *                           provided all status codes are accepted as
   *                           successful. Otherwise an error is thrown if the
   *                           server response is not included.
   * @return the json response from Readmill
   * @throws ReadmillException if the request was not successful
   */
  private JSONObject sendRequest(RequestBuilder request,
                                 String actionDescription,
                                 int... allowedStatusCodes) throws ReadmillException {
    try {
      JSONObject response = request.fetchOrThrow();
      assertInRange(response, actionDescription, allowedStatusCodes);
      return response;
    } catch(JSONException e) {
      fail(actionDescription, e);
    } catch(IOException e) {
      fail(actionDescription, e);
    }
    return null;
  }

  /**
   * Logs and throws a ReadmillException
   *
   * @param action The action that failed
   * @param e      the exception it originally failed with
   * @throws ReadmillException always
   */
  private void fail(String action, Exception e) throws ReadmillException {
    Log.d(TAG, "Failed to " + action, e);
    String message = String.format("Failed to %s: %s", action, e.getMessage());
    throw new ReadmillException(message);
  }

  /**
   * Asserts that the given response from Readmill is allowed.
   * <p/>
   * It is considered allowed if the response includes a status that is included
   * in the provided white list – or – if the white list is empty.
   *
   * @param readmillResponse   response to handle
   * @param actionDescription  description of the action for the error message
   * @param allowedStatusCodes white list of allowed status codes
   * @throws ReadmillException if the response was not in range
   */
  private void assertInRange(JSONObject readmillResponse, String actionDescription, int... allowedStatusCodes) throws ReadmillException {
    if(allowedStatusCodes.length > 0) {
      int statusCode = readmillResponse.optInt("status", -1);

      boolean isAllowed = false;
      for(int allowedStatusCode : allowedStatusCodes) {
        if(statusCode == allowedStatusCode) {
          isAllowed = true;
          break;
        }
      }

      if(!isAllowed) {
        String message = "Could not " + actionDescription;
        String errorText = readmillResponse.optString("error");
        if(errorText != null) {
          message += ". The server responded with error: " + errorText;
        }
        throw new ReadmillException(message, statusCode);
      }
    }
  }

  /**
   * Convert an old integer value from the Readmill v1 API to a string value
   * from suitable for the Readmill v2 API.
   *
   * @param integerState Integer state to convert
   * @return the converted string or null if the value is out or range
   */
  private String getStringState(int integerState) {
    switch(integerState) {
      case ReadingState.INTERESTING:
        return "interesting";
      case ReadingState.READING:
        return "reading";
      case FINISHED:
        return "finished";
      case ReadingState.ABANDONED:
        return "abandoned";
      default:
        return null;
    }
  }

  /**
   * Parses a ISO 8601 date string and return it as a Date object.
   *
   * @param dateString string to parse
   * @return the Date object or null if it could not be parsed.
   */
  public static Date parseISO8601(String dateString) {
    try {
      return iso8601Format.parse(dateString);
    } catch(ParseException e) {
      return null;
    }
  }

  /**
   * Formats a date object as a ISO8601 Date string.
   *
   * @param date the Date object to format
   * @return The date in ISO8601 format or null
   */
  private static String toISO8601(Date date) {
    return date == null ? "" : iso8601Format.format(date);
  }

  /**
   * Parses a iso 8601 date string and converts it to Unix epoch (seconds since 1970)
   *
   * @param stringDate string to parse as date
   * @return the epoch time or 0 if it could not be parsed
   */
  public static long parseISO8601ToUnix(String stringDate) {
    Date date = parseISO8601(stringDate);
    return date == null ? 0 : date.getTime() / 1000;
  }

  /**
   * Convert a string state (v2) to integer value (v1).
   *
   * @param state string state to convert
   * @return the integer state or 0 if it could not be mapped
   */
  public static int toIntegerState(String state) {
    if(state.equals("interesting")) {
      return ReadingState.INTERESTING;
    } else if(state.equals("reading")) {
      return ReadingState.READING;
    } else if(state.equals("finished")) {
      return ReadingState.FINISHED;
    } else if(state.equals("abandoned")) {
      return ReadingState.ABANDONED;
    }
    return 0;
  }

  /**
   * Dump a JSON object as a string and handle null.
   *
   * @param jsonObject JSON object to dump
   * @return the JSON as a string or "NULL".
   */
  public static String dumpJSON(JSONObject jsonObject) {
    return jsonObject == null ? "NULL" : jsonObject.toString();
  }

  /**
   * Checks the state of the wrapper to see if a token is currently set.
   *
   * @return true if a token is set, false otherwise
   */
  public boolean hasToken() {
    return mWrapper.getToken() != null;
  }

  public void setToken(Token token) {
    mWrapper.setToken(token);
  }

  /**
   * Readmill reading state values
   */
  public static interface ReadingState {
    int INTERESTING = 1;
    int READING = 2;
    int FINISHED = 3;
    int ABANDONED = 4;
  }
}
