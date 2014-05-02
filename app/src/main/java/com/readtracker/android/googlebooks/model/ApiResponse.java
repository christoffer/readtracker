package com.readtracker.android.googlebooks.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class ApiResponse<T> {
  @SerializedName("items") List<T> mItems;
  public List<T> getItems() { return mItems; }
}
