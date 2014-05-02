package googlebooks.model;

import com.google.gson.annotations.SerializedName;

public class Volume {

  @SerializedName("id") private String id;
  @SerializedName("volumeInfo") private VolumeInfo volumeInfo;

  public String getId() {
    return id;
  }

  public boolean isValid() {
    return id != null &&
        volumeInfo != null &&
        volumeInfo.title != null &&
        volumeInfo.authors != null &&
        volumeInfo.title.length() > 0 &&
        volumeInfo.authors.length > 0;
  }

  public VolumeInfo getVolumeInfo() {
    return volumeInfo;
  }
}
