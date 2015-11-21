package com.readtracker.android.test_support;

import com.readtracker.android.db.export.JSONExporter;

import org.json.JSONException;
import org.json.JSONObject;

public class JsonBuilder {
  JSONObject mJsonObject = new JSONObject();

  public JsonBuilder add(String key, Object value) {
    try {
      mJsonObject.put(key, value);
    } catch(JSONException e) {
      e.printStackTrace();
      throw new RuntimeException(e.getMessage());
    }
    return this;
  }

  @Override public String toString() {
    return mJsonObject.toString();
  }

  public JSONObject build() {
    return mJsonObject;
  }
}
