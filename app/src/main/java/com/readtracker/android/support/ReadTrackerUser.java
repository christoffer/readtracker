package com.readtracker.android.support;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * A local ReadTracker user.
 */
public class ReadTrackerUser {
  private final long readmillId;
  private final String email;
  private final String displayName;
  private final String webURL;
  private final String avatarURL;

  /**
   * Initialize the instance from a Readmill JSON response representing a user.
   *
   * @param readmillUser The user to import
   */
  public ReadTrackerUser(JSONObject readmillUser) throws JSONException {
    this.readmillId = readmillUser.getLong("id");
    this.email = readmillUser.getString("email");
    this.displayName = readmillUser.getString("fullname");
    this.webURL = readmillUser.getString("permalink_url");
    this.avatarURL = readmillUser.getString("avatar_url");
  }

  public final long getReadmillId() {
    return this.readmillId;
  }

  public final String getEmail() {
    return this.email;
  }

  public final String getDisplayName() {
    return this.displayName;
  }

  public final String getWebURL() {
    return this.webURL;
  }

  public final String getAvatarURL() {
    return this.avatarURL;
  }

  public String toJSON() {
    try {
      JSONObject jsonBlob = new JSONObject();
      jsonBlob.put("id", this.readmillId);
      jsonBlob.put("email", this.email);
      jsonBlob.put("fullname", this.displayName);
      jsonBlob.put("permalink_url", this.webURL);
      jsonBlob.put("avatar_url", this.avatarURL);

      return jsonBlob.toString();
    } catch(JSONException ignored) {
      // Handles errors on load instead of save
    }
    return "";
  }

}
