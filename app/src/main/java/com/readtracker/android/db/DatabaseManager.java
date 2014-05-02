package com.readtracker.android.db;

import android.util.Log;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.SelectArg;

import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.util.List;

/**
 * Helper class for facilitating database access.
 */
public class DatabaseManager {
  private static final String TAG = DatabaseManager.class.getSimpleName();

  private final DatabaseHelper db;

  public DatabaseManager(DatabaseHelper databaseHelper) {
    db = databaseHelper;
  }

  /**
   * Returns the single model of a class with the id, or null if id did not exist.
   */
  @Nullable public <T extends Model> T get(Class<T> modelClass, int id) {
    Dao<T, Integer> dao = db.getDaoByClass(modelClass);
    try {
      return dao.queryForId(id);
    } catch(SQLException originalException) {
      try {
        if(!dao.idExists(id)) {
          return null;
        }
      } catch(SQLException innerException) {
        Log.e(TAG, "Failure trying to check for id", innerException);
      }
      throw new RuntimeException(originalException);
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
   * @return true if saved or created.
   */
  public <T extends Model> boolean save(T instance) {
    @SuppressWarnings("unchecked")
    Dao<T, Integer> dao = (Dao<T, Integer>) db.getDaoByClass(instance.getClass());

    try {
      if(instance.getId() > 0) {
        dao.update(instance);
        return true;
      } else {
        dao.create(instance);
        return true;
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

  /** Returns true if the database contains a book with the given title, false otherwise. */
  public boolean isUniqueTitle(String title) {
    try {
      SelectArg titleArg = new SelectArg();
      titleArg.setValue(title);
      return db.getDaoByClass(Book.class).queryBuilder()
          .where().eq(Book.Columns.TITLE, titleArg)
          .countOf() == 0;
    } catch(SQLException e) {
      throw new RuntimeException(e);
    }
  }
}
