package com.readtracker.android.db;

import com.j256.ormlite.dao.Dao;

import java.sql.SQLException;
import java.util.List;

/** Helper class for facilitating database access. */
public class DatabaseManager {
  private final DatabaseHelper db;

  public DatabaseManager(DatabaseHelper databaseHelper) {
    db = databaseHelper;
  }

  /** Returns all persisted models of a class. */
  public <T extends Model> List<T> getAll(Class<T> modelClass) {
    Dao<T, Integer> dao = db.getDaoByClass(modelClass);
    try {
      return dao.queryForAll();
    } catch(SQLException e) {
      throw new RuntimeException(e);
    }
  }

  /** Returns all sessions for the book. */
  public List<Session> getSessionsForBook(Book book) {
    try {
      return db.getDaoByClass(Session.class).queryBuilder()
        .where().eq(Session.Columns.BOOK_ID, book.getId())
        .query();
    } catch(SQLException e) {
      throw new RuntimeException(e);
    }
  }
}
