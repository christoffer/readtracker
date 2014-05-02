package googlebooks.model;

import com.google.gson.annotations.SerializedName;


public class ImageLinks {

  /**
   * "imageLinks": {
   "smallThumbnail": string,
   "thumbnail": string,
   "small": string,
   "medium": string,
   "large": string,
   "extraLarge": string
   },
   */

  @SerializedName("thumbnail") String thumbNail;

  public String getThumbNail() {
    return thumbNail;
  }
}
