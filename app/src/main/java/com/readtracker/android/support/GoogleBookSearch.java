package com.readtracker.android.support;

import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class GoogleBookSearch {
  private static final String TAG = GoogleBookSearch.class.getName();
  public static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

  private static final Pattern ISBNPattern = Pattern.compile("^(?:isbn[ :]+)([0-9 -]+)$");
  private static final OkHttpClient client = new OkHttpClient();

  public static ArrayList<GoogleBook> search(String query) throws GoogleBookSearchException {
    Log.i(TAG, "Got raw search query:\"" + query + "\"");
    ArrayList<GoogleBook> results = new ArrayList<>();

    Uri queryUri = buildQueryUri(query);

    Request request = new Request.Builder()
        .url(queryUri.toString())
        .get()
        .build();

    try(Response response = client.newCall(request).execute()) {
      //noinspection ConstantConditions body is non-null when response created from execute()
      String responseBody = response.body().string();
      JSONObject json = new JSONObject(responseBody);
      JSONArray jsonItems = json.optJSONArray("items");

      if(jsonItems == null) {
        return new ArrayList<>(); // No results
      }

      for(int i = 0; i < jsonItems.length(); i++) {
        JSONObject jsonObject = (JSONObject) jsonItems.get(i);
        GoogleBook googleBook = new GoogleBook(jsonObject);
        if(googleBook.isValid()) {
          results.add(googleBook);
        }
      }
    } catch(Exception e) {
      Log.e(TAG, "Error while searching GoogleBooks", e);
      throw new GoogleBookSearchException(e.getMessage());
    }
    Log.d(TAG, String.format("Returning %s results", results.size()));
    Log.v(TAG, String.format("Results: %s", results.toString()));

    return results;
  }

  public static String buildQueryForTitleAndAuthor(String title, String author) {
    ArrayList<String> query = new ArrayList<>();
    if(!TextUtils.isEmpty(title)) {
      query.add("intitle:" + title);
    }

    if(!TextUtils.isEmpty(author)) {
      query.add("inauthor:" + author);
    }

    if(query.size() > 0) {
      return TextUtils.join(" ", query);
    }

    return null;
  }

  private static Uri buildQueryUri(String queryString) {
    String isbnQueryString = parseISBNQueryString(queryString);
    queryString = isbnQueryString == null ? queryString : isbnQueryString;

    Log.i(TAG, "Searching for query: " + queryString);

    return new Uri.Builder().
        scheme("https").
        authority("www.googleapis.com").
        path("books/v1/volumes").
        appendQueryParameter("q", queryString).
        build();
  }

  private static String parseISBNQueryString(String queryString) {
    Matcher isbnMatcher = ISBNPattern.matcher(queryString.toLowerCase().trim());
    if(isbnMatcher.matches()) {
      String cleanedNumber = isbnMatcher.group(0).replaceAll("[^0-9]+", "");
      return String.format("isbn:%s", cleanedNumber);
    }
    return null;
  }
}
