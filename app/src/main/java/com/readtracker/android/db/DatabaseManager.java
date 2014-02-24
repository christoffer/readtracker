package com.readtracker.android.db;

import com.j256.ormlite.dao.Dao;

import java.sql.SQLException;
import java.util.List;

/**
 * Helper class for facilitating database access.
 */
public class DatabaseManager {
  private final DatabaseHelper db;

  public DatabaseManager(DatabaseHelper databaseHelper) {
    db = databaseHelper;
  }

  /**
   * Returns the single model of a class with the id.
   */
  public <T extends Model> T get(Class<T> modelClass, int id) {
    Dao<T, Integer> dao = db.getDaoByClass(modelClass);
    try {
      return dao.queryForId(id);
    } catch(SQLException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Returns all persisted models of a class.
   */
  public <T extends Model> List<T> getAll(Class<T> modelClass) {
    Dao<T, Integer> dao = db.getDaoByClass(modelClass);
    try {
      return dao.queryForAll();
    } catch(SQLException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Saves the current instance to the database. Existing entries are updated, new ones are created.
   */
  public <T extends Model> T save(T instance) {
    @SuppressWarnings("unchecked")
    Dao<T, Integer> dao = (Dao<T, Integer>) db.getDaoByClass(instance.getClass());

    try {
      if(instance.getId() > 0) {
        dao.update(instance);
        return instance;
      } else {
        dao.create(instance);
        return instance;
      }
    } catch(SQLException e) {
      throw new RuntimeException(e);
    }
  }

  public <T extends Model> boolean delete(T instance) {
    @SuppressWarnings("unchecked")
    Dao<T, Integer> dao = (Dao<T, Integer>) db.getDaoByClass(instance.getClass());

    try {
      dao.delete(instance);
      return true;
    } catch(SQLException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Returns all Sessions belonging to the Book.
   */
  public List<Session> getSessionsForBook(Book book) {
    try {
      return db.getDaoByClass(Session.class).queryBuilder()
        .where().eq(Session.Columns.BOOK_ID, book.getId())
        .query();
    } catch(SQLException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Returns all Quotes belonging to the Book.
   */
  public List<Quote> getQuotesForBook(Book book) {
    try {
      return db.getDaoByClass(Quote.class).queryBuilder()
        .where().eq(Quote.Columns.BOOK_ID, book.getId())
        .query();
    } catch(SQLException e) {
      throw new RuntimeException(e);
    }

  }
}
