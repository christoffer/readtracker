package googlebooks;

import android.net.Uri;
import android.util.Log;

import com.readtracker.android.support.HttpUtils;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import googlebooks.model.Volume;

public class GoogleBookSearch {
  private static final String TAG = GoogleBookSearch.class.getName();

//  /**
//   * Return a mocked list of books.
//   * Useful for testing.
//   * @return a list of mocked search results.
//   * @throws GoogleBookSearchException never (just here for API compatibility)
//   */
//  public static ArrayList<Volume> mockSearchResults() throws GoogleBookSearchException {
//    return new ArrayList<Volume>(20) {{
//      add(new Volume("Franz Kafka", "Letters to Milena", null));
//      add(new Volume("Franz Kafka", "The Penal Colony", null));
//      add(new Volume("Franz Kafka", "Letters to Felice", null));
//      add(new Volume("Franz Kafka", "Metamorphosis", null));
//      add(new Volume("Franz Kafka", "The Complete Stories", null));
//      add(new Volume("Franz Kafka", "America: With an introduction by Edwin Muir", null));
//      add(new Volume("Franz Kafka", "The Trial", null));
//      add(new Volume("June. O. Leavitt", "The Mystical Life of Franz Kafka", null));
//      add(new Volume("Richard T. Gray", "A Franz Kafka Encyclopedia", null));
//      add(new Volume("Sander L. Gilman", "Franz Kafka, the Jewish Patient", null));
//    }};
//  }
}
