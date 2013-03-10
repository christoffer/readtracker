package com.readtracker.db;

import android.util.Log;
import com.j256.ormlite.dao.Dao;
import com.readtracker.ApplicationReadTracker;

import java.sql.SQLException;

/**
 * Provide a facade over the database actions for handling highlights.
 */
public class Highlights {
  private static final String TAG = Highlights.class.getName();

  public static Dao.CreateOrUpdateStatus createOrUpdate(LocalHighlight localHighlight) {
    try {
      return ApplicationReadTracker.getHighlightDao().createOrUpdate(localHighlight);
    } catch(SQLException e) {
      Log.e(TAG, "Failed to persist Highlight", e);
      return null;
    }
  }
}
