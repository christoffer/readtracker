package com.readtracker.android.googlebooks.model;

import android.test.AndroidTestCase;
import android.test.InstrumentationTestCase;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.readtracker.android.test.TestUtils;

import junit.framework.TestCase;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;

import dalvik.system.PathClassLoader;

import static org.fest.assertions.api.Assertions.assertThat;

/**
 * Model structure is -
 *  ApiResponse
 *   List<Volume>
 *
 *  Volume
 *   VolumeInfo
 *    ImageLinks
 *
 * This test uses actual response data to verify our ability to parse
 * the response data
 *
 * TODO This could become a JUnit4 test once betterGradle Android plugin support is available
 */
public class ModelDeserializationTest extends AndroidTestCase {

  public void test_searchQueryResponse() {
    String json = TestUtils.readTestDataFile(getContext().getClassLoader(), "google_books/search_query_response.json");
    Gson gson = new Gson();
    ApiResponse<Volume> apiResponse = gson.fromJson(json, new TypeToken<ApiResponse<Volume>>() {}.getType());

    assertThat(apiResponse).isNotNull();
    assertThat(apiResponse.getItems()).isNotNull();
    assertThat(apiResponse.getItems().size()).isEqualTo(10);
  }

  public void test_singleVolumeModel() {
    String json = TestUtils.readTestDataFile(getContext().getClassLoader(), "google_books/single_volume.json");
    Gson gson = new Gson();
    Volume volume = gson.fromJson(json, Volume.class);

    // Verify Volume wrapper
    assertThat(volume).isNotNull();
    assertThat(volume.getId()).isEqualTo("KPjmuogFmU0C");

    // Verify VolumeInfo contents
    VolumeInfo volumeInfo = volume.getVolumeInfo();
    assertThat(volumeInfo).isNotNull();
    assertThat(volumeInfo.getTitle()).isEqualTo("Android Apps Entwicklung für Dummies");
    assertThat(volumeInfo.getAuthors()).isEqualTo(new String[]{ "Donn Felker" });
    assertThat(volumeInfo.getPageCount()).isEqualTo(344);

    // ImageLinks contents
    ImageLinks imageLinks = volumeInfo.getImageLinks();
    assertThat(imageLinks).isNotNull();
    assertThat(imageLinks.getThumbNail()).isEqualTo("http://bks8.books.google.de/books?id=KPjmuogFmU0C&printsec=frontcover&img=1&zoom=1&edge=curl&source=gbs_api");

  }

}
