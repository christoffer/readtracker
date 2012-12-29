package com.readtracker_beta.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.util.Log;
import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;

import java.sql.SQLException;
import java.util.*;

public class DatabaseHelper extends OrmLiteSqliteOpenHelper {

  public DatabaseHelper(Context context) {
    super(context, DATABASE_NAME, null, DATABASE_VERSION);
  }

  public static final String DATABASE_NAME = "readtracker_beta.db";
  public static final int DATABASE_VERSION = 8;
  private static final String TAG = DatabaseHelper.class.getName();

  private Dao<LocalReading, Integer> readingDao = null;
  private Dao<LocalSession, Integer> sessionDao = null;
  private Dao<LocalHighlight, Integer> highlightDao = null;

  public Dao<LocalReading, Integer> getReadingDao() throws SQLException {
    if(readingDao == null) {
      readingDao = getDao(LocalReading.class);
    }
    return readingDao;
  }

  public Dao<LocalSession, Integer> getSessionDao() throws SQLException {
    if(sessionDao == null) {
      sessionDao = getDao(LocalSession.class);
    }
    return sessionDao;
  }

  public Dao<LocalHighlight, Integer> getHighlightDao() throws SQLException {
    if(highlightDao == null) {
      highlightDao = getDao(LocalHighlight.class);
    }
    return highlightDao;
  }

  @Override
  public void onCreate(SQLiteDatabase db, ConnectionSource connectionSource) {
    Log.d(TAG, "Running database create");
    try {
      TableUtils.createTable(connectionSource, LocalReading.class);
      TableUtils.createTable(connectionSource, LocalSession.class);
      TableUtils.createTable(connectionSource, LocalHighlight.class);
    } catch(SQLException e) {
      Log.e(TAG, "Failed to create database: " + DATABASE_NAME);
      throw new RuntimeException(e);
    }
  }

  @Override
  public void onUpgrade(SQLiteDatabase db, ConnectionSource connectionSource, int oldVersion, int newVersion) {
    int runningVersion = oldVersion;
    try {
      if(runningVersion == 1) {
        _upgradeToVersion2(db, connectionSource);
        runningVersion++;
      }
      if(runningVersion == 2) {
        _upgradeToVersion3(db, connectionSource);
        runningVersion++;
      }
      if(runningVersion == 3) {
        _upgradeToVersion4(db, connectionSource);
        runningVersion++;
      }
      if(runningVersion == 4) {
        _upgradeToVersion5(db, connectionSource);
        runningVersion++;
      }
      if(runningVersion == 5) {
        _upgradeToVersion6(db, connectionSource);
        runningVersion++;
      }
      if(runningVersion == 6) {
        _upgradeToVersion7(db, connectionSource);
        runningVersion++;
      }
      if(runningVersion == 7) {
        _upgradeToVersion8(db, connectionSource);
        runningVersion++;
      }
      Log.d(TAG, "Ended on running version: " + runningVersion);
    } catch(SQLiteException e) {
      Log.e(TAG, "Failed to upgrade database: " + DATABASE_NAME, e);
      throw new RuntimeException(e);
    }
  }

  // Database Upgrade methods

  private void _upgradeToVersion2(SQLiteDatabase db, ConnectionSource cs) {
    Log.i(TAG, "Running database upgrade 2");
    db.execSQL("ALTER TABLE QueuedPing RENAME TO ReadingSession;");
    db.execSQL("ALTER TABLE ReadingSession ADD COLUMN " + LocalSession.SYNCED_WITH_READMILL_FIELD_NAME + " INTEGER NOT NULL DEFAULT 0;");
    db.execSQL("ALTER TABLE ReadingSession ADD COLUMN " + LocalSession.NEEDS_RECONNECT_FIELD_NAME + " INTEGER NOT NULL DEFAULT 0;");
    db.execSQL("ALTER TABLE ReadingSession ADD COLUMN " + LocalSession.STARTED_ON_PAGE_FIELD_NAME + " INTEGER NOT NULL DEFAULT -1;");
    db.execSQL("ALTER TABLE ReadingSession ADD COLUMN " + LocalSession.ENDED_ON_PAGE_FIELD_NAME + " INTEGER NOT NULL DEFAULT -1;");
    db.execSQL("ALTER TABLE ReadingSession ADD COLUMN " + LocalSession.IS_READTRACKER_SESSION_FIELD_NAME + " INTEGER NOT NULL DEFAULT 1;");
  }

  // Database Upgrade methods
  private void _upgradeToVersion3(SQLiteDatabase db, ConnectionSource cs) {
    Log.i(TAG, "Running database upgrade 3");
    db.execSQL("ALTER TABLE LocalReading ADD COLUMN " + LocalReading.LAST_READ_AT_FIELD_NAME + " INTEGER NULL;");
    db.execSQL("ALTER TABLE LocalReading ADD COLUMN " + LocalReading.LOCALLY_CLOSED_AT_FIELD_NAME + " INTEGER NULL;");
    db.execSQL("ALTER TABLE LocalReading ADD COLUMN " + LocalReading.PROGRESS_FIELD_NAME + " DOUBLE NOT NULL DEFAULT 0;");

    db.execSQL("ALTER TABLE LocalReading ADD COLUMN " + LocalReading.READMILL_TOUCHED_AT_FIELD_NAME + " INTEGER NULL;");
    db.execSQL("ALTER TABLE LocalReading ADD COLUMN " + LocalReading.READMILL_STATE_FIELD_NAME + " INTEGER NULL;");

    db.execSQL("ALTER TABLE LocalReading ADD COLUMN " + LocalReading.READMILL_CLOSING_REMARK + " TEXT NULL;");

    try {
      TableUtils.createTableIfNotExists(cs, LocalHighlight.class);
      List<LocalReading> allLocalReadings = getReadingDao().queryForAll();
      for(LocalReading localReading : allLocalReadings) {
        localReading.refreshProgress();
        getReadingDao().update(localReading);
      }
    } catch(SQLException e) {
      Log.e(TAG, "Failed to upgrade database", e);
      throw new RuntimeException("Failed to upgrade Database", e);
    }
  }

  private void _upgradeToVersion4(SQLiteDatabase db, ConnectionSource connectionSource) {
    Log.i(TAG, "Running database upgrade 4");
    try {
      List<LocalHighlight> highlights = getHighlightDao().queryForAll();
      // If two highlights have the same reading id, same content and same synced at: delete the one that has readmill_reading_id -1

      // Group by highlighted at (most likely to be unique)
      HashMap<Long, List<LocalHighlight>> timestampMap = new HashMap<Long, List<LocalHighlight>>();

      for(LocalHighlight highlight : highlights) {
        if(highlight.highlightedAt == null) {
          Log.w(TAG, "Found Highlight without highlightedAt - ignoring: " + highlight);
          continue;
        }

        Long timestamp = (long) Math.floor(highlight.highlightedAt.getTime() / 1000);
        if(!timestampMap.containsKey(timestamp)) {
          timestampMap.put(timestamp, new ArrayList<LocalHighlight>());
        }
        timestampMap.get(timestamp).add(highlight);
      }

      for(List<LocalHighlight> dupes : timestampMap.values()) {
        if(dupes.size() == 1) {
          continue; // not a dupe
        }

        // Only deal with a specific issue this migration
        if(dupes.size() > 2) {
          Log.w(TAG, "Not dupe highlight bug (but probably still a bug)");
          continue;
        }

        LocalHighlight first = dupes.get(0);
        LocalHighlight second = dupes.get(1);

        if((first.readmillHighlightId != -1) && (second.readmillHighlightId != -1)) {
          Log.d(TAG, "Not dupe highlight bug. Neither has Readmill Highlight id -1: " + first.readmillHighlightId + " vs. " + second.readmillHighlightId);
          continue;
        }

        if(!first.content.equals(second.content)) {
          Log.d(TAG, "Not dupe highlight bug. Content doesn't match: " + first.content + " vs. " + second.content);
          continue;
        }

        Log.d(TAG, "Found dupe: " + first + " and " + second);
        LocalHighlight highlightToDelete = first.readmillHighlightId == -1 ? first : second;
        Log.d(TAG, "Deleting " + highlightToDelete);
        getHighlightDao().delete(highlightToDelete);
      }
    } catch(SQLException e) {
      Log.e(TAG, "Failed to clear old dupes", e);
    }
  }

  private void _upgradeToVersion5(SQLiteDatabase db, ConnectionSource connectionSource) {
    Log.i(TAG, "Running database upgrade 5");
    // Major refactoring - name tables to distinguish them from the "Remote"
    // data (from Readmill).
    db.execSQL("ALTER TABLE ReadingData RENAME TO LocalReading;");
    db.execSQL("ALTER TABLE ReadingHighlight RENAME TO LocalHighlight;");
    db.execSQL("ALTER TABLE ReadingSession RENAME TO LocalSession;");
  }

  private void _upgradeToVersion6(SQLiteDatabase db, ConnectionSource connectionSource) {
    Log.i(TAG, "Running database upgrade 6");
    // Add measure in percent flag
    db.execSQL("ALTER TABLE LocalReading ADD COLUMN " + LocalReading.MEASURE_IN_PERCENT + " INTEGER NOT NULL DEFAULT 0;");
  }

  private void _upgradeToVersion7(SQLiteDatabase db, ConnectionSource connectionSource) {
    Log.i(TAG, "Running database upgrade 7");
    db.execSQL("ALTER TABLE LocalHighlight ADD COLUMN " + LocalHighlight.COMMENT_COUNT_FIELD_NAME + " INTEGER NOT NULL DEFAULT 0;");
    db.execSQL("ALTER TABLE LocalHighlight ADD COLUMN " + LocalHighlight.LIKE_COUNT_FIELD_NAME + " INTEGER NOT NULL DEFAULT 0;");
    db.execSQL("ALTER TABLE LocalReading ADD COLUMN " + LocalReading.READMILL_RECOMMENDED + " INTEGER NOT NULL DEFAULT 0;");
  }

  private void _upgradeToVersion8(SQLiteDatabase db, ConnectionSource connectionSource) {
    Log.i(TAG, "Running database upgrade 8");
    db.execSQL("ALTER TABLE LocalReading ADD COLUMN " + LocalReading.DELETED_BY_USER_FIELD_NAME + " INTEGER NOT NULL DEFAULT 0;");
  }
}
