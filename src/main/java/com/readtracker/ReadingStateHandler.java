package com.readtracker;

import android.content.SharedPreferences;
import android.util.Log;
import com.readtracker.value_objects.ReadingState;

/**
 * Handles storing and loading of a temporary reading state
 */
class ReadingStateHandler {
  public static final String KEY_IS_ACTIVE = "reading-state-handler-is-active";
  public static final String KEY_ELAPSED = "reading-state-handler-elapsed";
  public static final String KEY_LOCAL_READING_ID = "reading-state-handler-local-reading-id";

  private static final String TAG = ReadingStateHandler.class.getName();

  /**
   * Stores the given reading state in preferences.
   *
   * @param localReadingId      the id of the LocalReading of the session
   * @param elapsedMilliseconds the elapsed time in ms
   * @param isActive            if the session was active when stored or not
   */
  public static void store(int localReadingId, long elapsedMilliseconds, boolean isActive) {
    store(new ReadingState(localReadingId, elapsedMilliseconds, isActive));
  }

  /**
   * Stores the given reading state in preferences.
   *
   * @param readingState reading state to store
   */
  public static void store(ReadingState readingState) {
    Log.d(TAG, "Storing reading state: " + (readingState == null ? "NULL" : readingState));
    ApplicationReadTracker.getApplicationPreferences().
        edit().
        putInt(KEY_LOCAL_READING_ID, readingState.getLocalReadingId()).
        putLong(KEY_ELAPSED, readingState.getElapsedMilliseconds()).
        putBoolean(KEY_IS_ACTIVE, readingState.getIsActive()).
        commit();
  }

  /**
   * Loads a previously stored ReadingState
   *
   * @return the ReadingState or null
   */
  public static ReadingState load() {
    Log.d(TAG, "Loading reading state");
    SharedPreferences pref = ApplicationReadTracker.getApplicationPreferences();

    int localReadingId = pref.getInt(KEY_LOCAL_READING_ID, -1);
    long elapsedMilliseconds = pref.getLong(KEY_ELAPSED, 0);
    boolean isActive = pref.getBoolean(KEY_IS_ACTIVE, false);

    if(localReadingId == -1) {
      Log.d(TAG, " - No reading state found");
      return null;
    }

    ReadingState readingState = new ReadingState(localReadingId, elapsedMilliseconds, isActive);
    Log.d(TAG, " - Found reading state: " + readingState);
    return readingState;
  }

  public static void clear() {
    Log.d(TAG, "Clearing reading state");
    ApplicationReadTracker.getApplicationPreferences().
        edit().
        remove(KEY_LOCAL_READING_ID).
        remove(KEY_ELAPSED).
        remove(KEY_IS_ACTIVE).
        commit();
  }
}
