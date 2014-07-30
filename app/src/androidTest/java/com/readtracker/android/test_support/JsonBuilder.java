package com.readtracker.android.test_support;

import com.readtracker.android.db.export.JSONExporter;

import org.json.JSONException;
import org.json.JSONObject;

public class JsonBuilder {
  JSONObject mJsonObject = new JSONObject();

  public JsonBuilder add(String key, Object value) throws JSONException {
    mJsonObject.put(key, value);
    return this;
  }

  @Override public String toString() {
    return mJsonObject.toString();
  }

  public JSONObject build() {
    return mJsonObject;
  }
}
