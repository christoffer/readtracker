package com.readtracker.android.support;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

@SuppressWarnings("UnusedDeclaration")
public class HttpUtils {
  /**
   * Consumes a http response as JSON.
   *
   * @param response Response to parse.
   * @return The parsed JSONObject or null if it could not parsed or an
   * error occurred while consuming it.
   */
  public static JSONObject optJSON(HttpResponse response) {
    try {
      return getJSON(response);
    } catch(IOException e) {
      e.printStackTrace();
      return null;
    } catch(JSONException e) {
      e.printStackTrace();
      return null;
    }
  }

  /**
   * Consumes a http response as JSON.
   *
   * @param response Response to parse.
   * @return The parsed JSONObject
   * @throws IOException   When an error occurred while consuming the response
   * @throws JSONException When the response was not valid json
   */
  public static JSONObject getJSON(HttpResponse response) throws IOException, JSONException {
    return new JSONObject(getString(response));
  }

  /**
   * Consumes a http response as a string.
   *
   * @param response The http response to consume
   * @return The response body of the response as a string
   * @throws IOException If there was an error consuming the response
   */
  public static String getString(HttpResponse response) throws IOException {
    HttpEntity entity = response.getEntity();
    return EntityUtils.toString(entity);
  }

  /**
   * Consumes a http response as a string.
   *
   * @param response The http response to consume
   * @return The response body of the response as a string or null
   * if the http response could not be consumed.
   */
  public static String optString(HttpResponse response) {
    try {
      return getString(response);
    } catch(IOException e) {
      e.printStackTrace();
      return null;
    }
  }
}
