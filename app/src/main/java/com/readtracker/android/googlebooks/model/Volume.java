package com.readtracker.android.googlebooks.model;

import com.google.gson.annotations.SerializedName;

public class Volume {

  @SerializedName("id") String id;
  @SerializedName("volumeInfo") VolumeInfo volumeInfo;

  public String getId() {
    return id;
  }

  public VolumeInfo getVolumeInfo() { return volumeInfo; }

  public boolean isValid() {
    return id != null
        && volumeInfo != null
        && volumeInfo.title != null
        && volumeInfo.authors != null
        && volumeInfo.title.length() > 0
        && volumeInfo.authors.length > 0;
  }

  public static class VolumeInfo {

    public static final long UNSET_PAGECOUNT = -1;

    @SerializedName("authors") String[] authors;
    @SerializedName("title") String title;

    @SerializedName("imageLinks") ImageLinks imageLinks;
    @SerializedName("pageCount") long pageCount = UNSET_PAGECOUNT;

    public String[] getAuthors() { return authors; }

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

  public static class ImageLinks {
    @SerializedName("thumbnail") String thumbNail;
    public String getThumbNail() {
      return thumbNail;
    }
  }
}
