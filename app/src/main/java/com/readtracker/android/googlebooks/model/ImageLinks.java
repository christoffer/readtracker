package com.readtracker.android.googlebooks.model;

import com.google.gson.annotations.SerializedName;


public class ImageLinks {

  @SerializedName("thumbnail") String thumbNail;

  public String getThumbNail() {
    return thumbNail;
  }
}
