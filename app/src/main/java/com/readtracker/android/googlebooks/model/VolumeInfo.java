package com.readtracker.android.googlebooks.model;

import com.google.gson.annotations.SerializedName;

/**
 * Refer to
 */
public class VolumeInfo {

  public static final long UNSET_PAGECOUNT = -1;

  @SerializedName("authors") String[] authors;
  @SerializedName("title") String title;

  @SerializedName("imageLinks") ImageLinks imageLinks;
  @SerializedName("pageCount") long pageCount = UNSET_PAGECOUNT;

  public String[] getAuthors() {
    return authors;
  }

  public String getTitle() {
    return title;
  }

  public ImageLinks getImageLinks() {
    return imageLinks;
  }

  public long getPageCount() {
    return pageCount;
  }
}
