package com.readtracker_beta.support;

import android.net.Uri;
import android.util.Log;
import com.readmill.api.HttpUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

public class GoogleBookSearch {
  private static final String TAG = GoogleBookSearch.class.getName();

  public static ArrayList<GoogleBook> search(String query) throws GoogleBookSearchException {
    Log.i(TAG, "Searching GoogleBooks for query:\"" + query + "\"");
    ArrayList<GoogleBook> result = new ArrayList<GoogleBook>();

    HttpClient httpClient = new DefaultHttpClient();
    Uri uri = new Uri.Builder().
                scheme("https").
                authority("www.googleapis.com").
                path("books/v1/volumes").
                appendQueryParameter("q", query).
                build();

    HttpGet httpGet = new HttpGet(uri.toString());
    try {
      HttpResponse httpResponse = httpClient.execute(httpGet);
      String responseBody = HttpUtils.getString(httpResponse);
      JSONObject json = new JSONObject(responseBody);
      JSONArray jsonItems = json.getJSONArray("items");
      GoogleBook googleBook;
      for(int i = 0; i < jsonItems.length(); i++) {
        JSONObject jsonObject = (JSONObject) jsonItems.get(i);
        googleBook = new GoogleBook(jsonObject);
        if(googleBook.isSuitableForReadmill()) {
          result.add(googleBook);
        }
      }
    } catch (Exception e) {
      Log.e(TAG, "Error while searching GoogleBooks", e);
      throw new GoogleBookSearchException(e.getMessage());
    }
    return result;
  }

  /**
   * Return a mocked list of books.
   * Useful for testing.
   * @return a list of mocked search results.
   * @throws GoogleBookSearchException never (just here for API compatibility)
   */
  public static ArrayList<GoogleBook> mockSearchResults() throws GoogleBookSearchException {
    return new ArrayList<GoogleBook>(20) {{
      add(new GoogleBook("Franz Kafka", "Letters to Milena", null));
      add(new GoogleBook("Franz Kafka", "The Penal Colony", null));
      add(new GoogleBook("Franz Kafka", "Letters to Felice", null));
      add(new GoogleBook("Franz Kafka", "Metamorphosis", null));
      add(new GoogleBook("Franz Kafka", "The Complete Stories", null));
      add(new GoogleBook("Franz Kafka", "America: With an introduction by Edwin Muir", null));
      add(new GoogleBook("Franz Kafka", "The Trial", null));
      add(new GoogleBook("June. O. Leavitt", "The Mystical Life of Franz Kafka", null));
      add(new GoogleBook("Richard T. Gray", "A Franz Kafka Encyclopedia", null));
      add(new GoogleBook("Sander L. Gilman", "Franz Kafka, the Jewish Patient", null));
    }};
  }
}
