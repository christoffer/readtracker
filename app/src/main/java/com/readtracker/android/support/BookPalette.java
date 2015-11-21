package com.readtracker.android.support;

import android.support.v7.graphics.Palette;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

public class BookPalette implements Serializable {
  // TODO(christoffer) Use actual colors here
  public static final int DEFAULT_MUTED_COLOR = 0xff00ff00;
  public static final int DEFAULT_DARK_MUTED_COLOR = 0xffff0000;
  public static final int DEFAULT_VIBRANT_COLOR = 0xff0000ff;

  private int mMutedColor;
  private int mDarkMutedColor;
  private int mVibrantColor;

  public BookPalette() {
    mMutedColor = DEFAULT_MUTED_COLOR;
    mDarkMutedColor = DEFAULT_DARK_MUTED_COLOR;
    mVibrantColor = DEFAULT_VIBRANT_COLOR;
  }

  /** Initializes the BookPalette from a JSON string representation */
  public BookPalette(String jsonString) throws JSONException {
    JSONObject json = new JSONObject(jsonString);
    mMutedColor = json.optInt("muted", DEFAULT_MUTED_COLOR);
    mDarkMutedColor = json.optInt("dark_muted", DEFAULT_DARK_MUTED_COLOR);
    mVibrantColor = json.optInt("vibrant", DEFAULT_VIBRANT_COLOR);
  }

  public BookPalette(Palette palette) {
    mMutedColor = palette.getMutedColor(DEFAULT_MUTED_COLOR);
    mDarkMutedColor = palette.getDarkMutedColor(DEFAULT_DARK_MUTED_COLOR);
    mVibrantColor = palette.getVibrantColor(DEFAULT_VIBRANT_COLOR);
  }

  /** Returns a JSON string representation of the book palette. */
  public String toJSON() throws JSONException {
    JSONObject json = new JSONObject();
    json.put("muted", mMutedColor);
    json.put("dark_muted", mDarkMutedColor);
    json.put("vibrant", mVibrantColor);
    return json.toString();
  }

  @Override public int hashCode() {
    return (
        Integer.toString(mMutedColor) +
            Integer.toString(mDarkMutedColor) +
            Integer.toString(mVibrantColor)
    ).hashCode();
  }

  @Override public boolean equals(Object o) {
    if(o instanceof BookPalette) {
      BookPalette other = (BookPalette) o;
      return (
          mMutedColor == other.mMutedColor &&
              mDarkMutedColor == other.mDarkMutedColor &&
              mVibrantColor == other.mVibrantColor
      );
    }
    return super.equals(o);
  }

  public int getMutedColor() { return mMutedColor; }

  public void setMutedColor(int mutedColor) { mMutedColor = mutedColor; }

  public int getDarkMutedColor() { return mDarkMutedColor; }

  public void setDarkMutedColor(int darkMutedColor) { mDarkMutedColor = darkMutedColor; }

  public int getVibrantColor() { return mVibrantColor; }

  public void setVibrantColor(int vibrantColor) { mVibrantColor = vibrantColor; }
}

