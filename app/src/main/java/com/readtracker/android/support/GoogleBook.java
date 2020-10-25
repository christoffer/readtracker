package com.readtracker.android.support;

import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.UUID;

import okhttp3.HttpUrl;

public class GoogleBook {
  private String id = "";
  private String title = "";
  private String author = "";
  private String coverURL = null;
  private long pageCount = -1;

  public GoogleBook(JSONObject json) {
    if(json == null) {
      convertFromJSON(new JSONObject());
    } else {
      convertFromJSON(json);
    }
  }

  public GoogleBook(String title, String author, String coverURL) {
    this.title = title;
    this.author = author;
    this.coverURL = coverURL;
    this.id = UUID.randomUUID().toString();
  }

  @Override public String toString() {
    return String.format(
        "Id: %s, Title: %s, Author: %s, Cover: %s, Page count: %d",
        id, title, author, coverURL, pageCount
    );
  }

  public void convertFromJSON(JSONObject json) {
    id = getString(json, "id");

    JSONObject jsonVolumeInfo = json.optJSONObject("volumeInfo");
    if(jsonVolumeInfo == null) {
      id = null;
      return;
    }

    title = getString(jsonVolumeInfo, "title");

    JSONArray jsonAuthors = jsonVolumeInfo.optJSONArray("authors");
    if(jsonAuthors != null) {
      ArrayList<String> authors = new ArrayList<String>();
      for(int i = 0; i < jsonAuthors.length(); i++) {
        try {
          authors.add(jsonAuthors.getString(i));
        } catch(JSONException ignored) {
        }
      }
      author = TextUtils.join(", ", authors.toArray());
    }

    JSONObject jsonImageLinks = jsonVolumeInfo.optJSONObject("imageLinks");
    if(jsonImageLinks != null) {
      if(jsonImageLinks.has("small")) {
        coverURL = getString(jsonImageLinks, "small");
      } else if(jsonImageLinks.has("thumbnail")) {
        coverURL = getString(jsonImageLinks, "thumbnail");
      }

      if(coverURL != null) {
        // For some reason the Google Books API returns http urls for some images.
        // These urls can't be used with Picasso since Google blocks non-https requests
        // for images. Fix this by forcing https scheme, regardless of what the response
        // is.
        final HttpUrl url = HttpUrl.parse(coverURL);
        if(url != null) {
          coverURL = url.newBuilder().scheme("https").build().toString();
        }
      }
    }

    this.pageCount = jsonVolumeInfo.optLong("pageCount", -1);
  }

  protected String getString(JSONObject json, String key) {
    return json == null || json.isNull(key) ? "" : json.optString(key);
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getAuthor() {
    return author;
  }

  public long getPageCount() {
    return pageCount;
  }

  public void setAuthor(String author) {
    this.author = author;
  }

  public String getCoverURL() {
    return coverURL;
  }

  public void setCoverURL(String coverURL) {
    this.coverURL = coverURL;
  }

  public boolean isValid() {
    return id != null &&
        title != null &&
        author != null &&
        title.length() > 0 &&
        author.length() > 0;
  }
}
