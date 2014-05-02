package com.readtracker.android.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DatabaseHelper extends OrmLiteSqliteOpenHelper {

  public DatabaseHelper(Context context) {
    super(context, DATABASE_NAME, null, DATABASE_VERSION);
  }

  public static final String DATABASE_NAME = "readtracker.db";
  public static final int DATABASE_VERSION = 11;
  private static final String TAG = DatabaseHelper.class.getName();

  private Dao<LocalReading, Integer> readingDao = null;
  private Dao<LocalSession, Integer> sessionDao = null;
  private Dao<LocalHighlight, Integer> highlightDao = null;

  private final Map<Class<? extends Model>, Dao<? extends Model, Integer>> mDaoCache =
    new HashMap<Class<? extends Model>, Dao<? extends Model, Integer>>();

  /** Cached lookup of DAOs by class. */
  <T extends Model> Dao<T, Integer> getDaoByClass(Class<T> modelClass) {
    if(mDaoCache.containsKey(modelClass)) {
      //noinspection unchecked
      return (Dao<T, Integer>) mDaoCache.get(modelClass);
    }

    try {
      Dao<T, Integer> dao = getDao(modelClass);
      mDaoCache.put(modelClass, dao);
      return dao;
    } catch(SQLException e) {
      throw new RuntimeException("Failed to get DAO for class: " + modelClass, e);
    }
  }

  public Dao<LocalReading, Integer> getReadingDao() throws SQLException {
    if(readingDao == null) {
      readingDao = getDao(LocalReading.class);
    }
    return readingDao;
  }

  public Dao<LocalSession, Integer> getLocalSessionDao() throws SQLException {
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
      TableUtils.createTableIfNotExists(connectionSource, LocalReading.class);
      TableUtils.createTableIfNotExists(connectionSource, LocalSession.class);
      TableUtils.createTableIfNotExists(connectionSource, LocalHighlight.class);

      TableUtils.createTableIfNotExists(connectionSource, Book.class);
      TableUtils.createTableIfNotExists(connectionSource, Session.class);
      TableUtils.createTableIfNotExists(connectionSource, Quote.class);
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
      if(runningVersion == 8) {
        _upgradeToVersion9(db, connectionSource);
        runningVersion++;
      }
      if(runningVersion == 9) {
        _upgradeToVersion10(db, connectionSource);
        runningVersion++;
      }

      if(runningVersion == 10) {
        _upgradeToVersion11(db, connectionSource);
        runningVersion++;
      }

      Log.d(TAG, "Ended on running version: " + runningVersion);
    } catch(SQLException e) {
      Log.e(TAG, "Failed to upgrade database: " + DATABASE_NAME, e);
      throw new RuntimeException(e);
    }
  }

  /**
   * This migration will extend and rename the session table.
   */
  private void _upgradeToVersion2(SQLiteDatabase db, ConnectionSource cs) {
    Log.i(TAG, "Running database upgrade 2");
    db.execSQL("ALTER TABLE QueuedPing RENAME TO ReadingSession;");
    db.execSQL("ALTER TABLE ReadingSession ADD COLUMN " + LocalSession.SYNCED_WITH_READMILL_FIELD_NAME + " INTEGER NOT NULL DEFAULT 0;");
    db.execSQL("ALTER TABLE ReadingSession ADD COLUMN " + LocalSession.NEEDS_RECONNECT_FIELD_NAME + " INTEGER NOT NULL DEFAULT 0;");
    db.execSQL("ALTER TABLE ReadingSession ADD COLUMN " + LocalSession.STARTED_ON_PAGE_FIELD_NAME + " INTEGER NOT NULL DEFAULT -1;");
    db.execSQL("ALTER TABLE ReadingSession ADD COLUMN " + LocalSession.ENDED_ON_PAGE_FIELD_NAME + " INTEGER NOT NULL DEFAULT -1;");
    db.execSQL("ALTER TABLE ReadingSession ADD COLUMN " + LocalSession.IS_READTRACKER_SESSION_FIELD_NAME + " INTEGER NOT NULL DEFAULT 1;");
  }

  /**
   * This migration will add highlights table and syncing fields to readings.
   */
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

  /**
   * This migration will fix an issue with duplicate highlights being created
   * once they were synced to Readmill.
   * A local one would be created, and then the sync failed to match it against
   * the remote one once, causing an identical highlight to be created.
   */
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

  /**
   * This migration will rename all tables to a new naming scheme.
   */
  private void _upgradeToVersion5(SQLiteDatabase db, ConnectionSource connectionSource) {
    Log.i(TAG, "Running database upgrade 5");
    db.execSQL("ALTER TABLE ReadingData RENAME TO LocalReading;");
    db.execSQL("ALTER TABLE ReadingHighlight RENAME TO LocalHighlight;");
    db.execSQL("ALTER TABLE ReadingSession RENAME TO LocalSession;");
  }

  /**
   * This migration will add the measure in percent flag to readings.
   */
  private void _upgradeToVersion6(SQLiteDatabase db, ConnectionSource connectionSource) {
    Log.i(TAG, "Running database upgrade 6");
    db.execSQL("ALTER TABLE LocalReading ADD COLUMN " + LocalReading.MEASURE_IN_PERCENT + " INTEGER NOT NULL DEFAULT 0;");
  }

  /**
   * This migration will add counters to highlights and a recommended flag to
   * readings.
   */
  private void _upgradeToVersion7(SQLiteDatabase db, ConnectionSource connectionSource) {
    Log.i(TAG, "Running database upgrade 7");
    db.execSQL("ALTER TABLE LocalHighlight ADD COLUMN " + LocalHighlight.COMMENT_COUNT_FIELD_NAME + " INTEGER NOT NULL DEFAULT 0;");
    db.execSQL("ALTER TABLE LocalHighlight ADD COLUMN " + LocalHighlight.LIKE_COUNT_FIELD_NAME + " INTEGER NOT NULL DEFAULT 0;");
    db.execSQL("ALTER TABLE LocalReading ADD COLUMN " + LocalReading.READMILL_RECOMMENDED_FIELD_NAME + " INTEGER NOT NULL DEFAULT 0;");
  }

  private void _upgradeToVersion8(SQLiteDatabase db, ConnectionSource connectionSource) {
    Log.i(TAG, "Running database upgrade 8");
    db.execSQL("ALTER TABLE LocalReading ADD COLUMN " + LocalReading.DELETED_BY_USER_FIELD_NAME + " INTEGER NOT NULL DEFAULT 0;");
    db.execSQL("ALTER TABLE LocalReading ADD COLUMN " + LocalReading.READMILL_IS_PRIVATE_FIELD_NAME + " INTEGER NOT NULL DEFAULT 0;");
    db.execSQL("ALTER TABLE LocalReading ADD COLUMN " + LocalReading.STARTED_AT_FIELD_NAME + " INTEGER NOT NULL DEFAULT 0;");
  }

  /**
   * This upgrade fixes an issue where sessions and highlights for readings that
   * were started offline never got the readmill reading id set on them once the
   * reading was connected to Readmill.
   * <p/>
   * It goes through all sessions and highlights with a missing readmill reading
   * id. For each of these it adopts the readmill reading id of the parent reading
   * (if set).
   */
  private void _upgradeToVersion9(SQLiteDatabase db, ConnectionSource connectionSource) throws SQLException {
    Log.i(TAG, "Running database upgrade 9");

    List<LocalReading> connectedReadings = getReadingDao().queryBuilder()
      .where()
      .gt(LocalReading.READMILL_READING_ID_FIELD_NAME, 0).query();

    Log.d(TAG, String.format("Found %d connected readings", connectedReadings.size()));

    for(LocalReading localReading : connectedReadings) {
      List<LocalSession> sessions = getLocalSessionDao().queryBuilder().where()
        .eq(LocalSession.READING_ID_FIELD_NAME, localReading.id).query();

      Log.d(TAG, String.format("Found %d sessions for reading with id %d", sessions.size(), localReading.id));

      for(LocalSession session : sessions) {
        if(session.readmillReadingId > 0) continue;
        Log.i(TAG, String.format("Updating session %s, setting Readmill Reading id to %d", session.sessionIdentifier, localReading.readmillReadingId));
        session.readmillReadingId = localReading.readmillReadingId;
        getLocalSessionDao().update(session);
      }

      List<LocalHighlight> highlights = getHighlightDao().queryBuilder().where()
        .eq(LocalHighlight.READING_ID_FIELD_NAME, localReading.id).query();

      Log.d(TAG, String.format("Found %d highlights for reading with id %d", highlights.size(), localReading.id));

      for(LocalHighlight highlight : highlights) {
        if(highlight.readmillReadingId > 0) continue;
        Log.i(TAG, String.format("Updating highlight %d, setting Readmill Reading id to %d", highlight.id, localReading.readmillReadingId));
        highlight.readmillReadingId = localReading.readmillReadingId;
        getHighlightDao().update(highlight);
      }
    }
  }

  /**
   * This upgrade adds the comment, editedAt and deletedByUser field to highlights and updatedAt to readings
   */
  private void _upgradeToVersion10(SQLiteDatabase db, ConnectionSource connectionSource) throws SQLException {
    Log.i(TAG, "Running database upgrade 10");
    db.execSQL("ALTER TABLE LocalHighlight ADD COLUMN " + LocalHighlight.COMMENT_FIELD_NAME + " TEXT NULL;");
    db.execSQL("ALTER TABLE LocalReading ADD COLUMN " + LocalReading.UPDATED_AT_FIELD_NAME + " INTEGER NULL;");
    db.execSQL("ALTER TABLE LocalHighlight ADD COLUMN " + LocalHighlight.EDITED_AT_FIELD_NAME + " INTEGER NULL;");
    db.execSQL("ALTER TABLE LocalHighlight ADD COLUMN " + LocalHighlight.DELETED_BY_USER_FIELD_NAME + " INTEGER NOT NULL DEFAULT 0;");
  }

  private void _upgradeToVersion11(SQLiteDatabase db, ConnectionSource connectionSource) throws SQLException {
    Log.i(TAG, "Running database upgrade 11");
    TableUtils.createTableIfNotExists(connectionSource, Book.class);
    TableUtils.createTableIfNotExists(connectionSource, Session.class);
    TableUtils.createTableIfNotExists(connectionSource, Quote.class);

    convertLocalReadingsToBook(db);
    convertLocalSessionToSession(db);
    convertLocalHighlightToQuote(db);
  }

  private void convertLocalReadingsToBook(SQLiteDatabase db) {
    final String query = "INSERT INTO books " +
      "(id, title, author, cover_image_url, page_count, state, current_position, current_position_timestamp, first_position_timestamp, closing_remark) " +
      "SELECT " +
      "id, title, author, coverURL, case(measure_in_percent) when 1 then null else case totalPages when 0 then null else totalPages end end, " +
      "case rm_state when 2 then 'Reading' when 3 then 'Finished' when 4 then 'Finished' else 'Unknown' end, ifnull(1.0 * currentPage / totalPages , null), " +
      "lastReadAt * 1000, started_at * 1000, rm_closing_remark " +
      "FROM localreading;";
    db.execSQL(query);
  }

  private void convertLocalHighlightToQuote(SQLiteDatabase db) {
    final String query = "insert into quotes " +
      "(id, book_id, content, position, add_timestamp) " +
      "select id, reading_id, content, position, strftime('%s', highlighted_at) * 1000 from localhighlight;";
    db.execSQL(query);
  }

  private void convertLocalSessionToSession(SQLiteDatabase db) {
    final String query = "insert into sessions " +
      "(id, book_id, end_position, start_position, timestamp, duration_seconds) " +
      "select outer_session.id, readingId, " +
      "progress as end_pos, " +
      "(select inner_session.progress from localsession as inner_session where inner_session.progress < outer_session.progress AND inner_session.readingId = outer_session.readingId order by inner_session.progress desc limit 1) as start_pos, " +
      "strftime('%s', occurredAt) * 1000, durationSeconds " +
      "from localsession as outer_session inner join localreading on localreading.id = readingId " +
      "order by readingId;";
    db.execSQL(query);
  }
}
