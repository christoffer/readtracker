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
    return id != null &&
        volumeInfo != null &&
        volumeInfo.title != null &&
        volumeInfo.authors != null &&
        volumeInfo.title.length() > 0 &&
        volumeInfo.authors.length > 0;
  }
}
