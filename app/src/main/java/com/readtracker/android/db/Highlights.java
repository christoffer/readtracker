package com.readtracker.android.db;

import android.util.Log;

import com.j256.ormlite.dao.Dao;
import com.readtracker.android.ReadTrackerApp;

import java.sql.SQLException;

/**
 * Provide a facade over the database actions for handling highlights.
 */
public class Highlights {
  private static final String TAG = Highlights.class.getName();

  public static Dao.CreateOrUpdateStatus createOrUpdate(LocalHighlight localHighlight) {
    try {
      return ReadTrackerApp.getHighlightDao().createOrUpdate(localHighlight);
    } catch(SQLException ex) {
      Log.e(TAG, "Failed to persist Highlight", ex);
      return null;
    }
  }

  /**
   * Deletes a highlight.
   * @param localHighlight highlight to delete
   * @return true if the highlight was deleted, false otherwise
   */
  public static boolean delete(LocalHighlight localHighlight) {
    try {
      ReadTrackerApp.getHighlightDao().delete(localHighlight);
      return true;
    } catch(SQLException ex) {
      Log.e(TAG, "Failed to delete highlight: " + String.valueOf(localHighlight));
    }
    return false;
  }
}
