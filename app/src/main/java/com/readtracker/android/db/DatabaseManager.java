package com.readtracker.android.db;

import com.j256.ormlite.dao.Dao;

import java.sql.SQLException;

/** Helper class for facilitating database access. */
public class DatabaseManager {
  private final DatabaseHelper db;
  private final Dao<Book, Integer> books;

  public DatabaseManager(DatabaseHelper databaseHelper) {
    db = databaseHelper;

    try {
      books = db.getBookDao();
    } catch(SQLException e) {
      throw new RuntimeException("Failed to initalize DatabaseManager", e);
    }
  }
}
