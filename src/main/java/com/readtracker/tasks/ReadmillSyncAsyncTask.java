package com.readtracker.tasks;

import android.os.AsyncTask;
import android.util.Log;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.Where;
import com.readtracker.ApplicationReadTracker;
import com.readtracker.interfaces.ReadmillSyncProgressListener;
import com.readtracker.db.*;
import com.readtracker.readmill.Converter;
import com.readtracker.readmill.ReadmillApiHelper;
import com.readtracker.readmill.ReadmillException;
import org.json.JSONException;
import org.json.JSONObject;

import java.sql.SQLException;
import java.util.*;

/**
 * Syncs data for the current user with their Readmill profile.
 */
public class ReadmillSyncAsyncTask extends AsyncTask<Long, ReadmillSyncProgressMessage, Integer> {
  private static final int STATUS_ERROR = -1;
  private static final int STATUS_OK = 0;

  private static final String TAG = ReadmillSyncAsyncTask.class.getName();
  private Dao<LocalReading, Integer> mReadingDao;
  private Dao<LocalSession, Integer> mSessionDao;
  private Dao<LocalHighlight, Integer> mHighlightDao;
  private ReadmillApiHelper mReadmillApi;

  ReadmillSyncProgressListener mProgressListener;

  public ReadmillSyncAsyncTask(ReadmillSyncProgressListener progressListener, ReadmillApiHelper readmillApi) {
    super();
    mProgressListener = progressListener;
    mReadmillApi = readmillApi;
  }

  @Override
  protected Integer doInBackground(Long... id) {
    long readmillUserId = id[0];

    try {
      mReadingDao = ApplicationReadTracker.getReadingDao();
      mSessionDao = ApplicationReadTracker.getSessionDao();
      mHighlightDao = ApplicationReadTracker.getHighlightDao();
    } catch(SQLException e) {
      Log.e(TAG, "Failed to get DAOs", e);
      return STATUS_ERROR;
    }

    try {
      performFullSyncForUser(readmillUserId);
      return STATUS_OK;
    } catch(ReadmillException exception) {
      Log.w(TAG, "Readmill Exception while trying to sync readings", exception);
      int httpStatusCode = exception.getStatusCode();
      Log.d(TAG, "Finishing with status code " + httpStatusCode);
      return httpStatusCode == -1 ? STATUS_ERROR : httpStatusCode;
    } catch(JSONException e) {
      Log.w(TAG, "Unexpected JSON received from Readmill", e);
      return STATUS_ERROR;
    } catch(SQLException e) {
      Log.e(TAG, "Unexpected SQL error while syncing", e);
      return STATUS_ERROR;
    }
  }

  @Override
  protected void onPreExecute() {
    mProgressListener.onSyncStart();
  }

  @Override
  protected void onPostExecute(Integer statusCode) {
    Log.d(TAG, "onPostExecute(" + statusCode + ")");
    if(statusCode == STATUS_OK) {
      mProgressListener.onSyncDone();
    } else {
      mProgressListener.onSyncFailed("An error occurred while syncing", statusCode);
    }
  }

  @Override
  protected void onCancelled() {
    Log.d(TAG, "onCancelled()");
    mProgressListener.onSyncDone();
  }

  @Override
  protected void onProgressUpdate(ReadmillSyncProgressMessage... messages) {
    for(ReadmillSyncProgressMessage message : messages) {
      LocalReading localReading = message.getLocalReading();
      if(localReading != null) {
        mProgressListener.onReadingUpdated(localReading);
      }
      mProgressListener.onSyncProgress(message.toString(), message.getProgress());
    }
  }

  /**
   * Syncs all local and remote data for a given user.
   *
   * @param readmillUserId readmill id of user to sync data for
   * @throws ReadmillException if an error occurs while communicating with Readmill
   * @throws JSONException     if the response from Readmill is not properly formatted
   */
  private void performFullSyncForUser(long readmillUserId) throws ReadmillException, JSONException, SQLException {
    Log.i(TAG, "Performing a full sync for user connected to readmill user id: " + readmillUserId);

    ArrayList<LocalReading> localReadings = getAllConnectedLocalReadingForUserId(readmillUserId);
    ArrayList<JSONObject> remoteReadings = getAllRemoteReadingsForUserId(readmillUserId);

    syncLocalReadingsWithRemoteReadings(localReadings, remoteReadings);
  }

  /**
   * Posts a generic message and a progress.
   *
   * @param message     Message to post
   * @param currentStep current step
   * @param totalSteps  total steps
   */
  private void postProgressUpdateMessage(String message, int currentStep, int totalSteps) {
    publishProgress(new ReadmillSyncProgressMessage(message, (float) currentStep / totalSteps));
  }

  /**
   * Puts a reading as a progress update message.
   *
   * @param localReading Reading to post as progress update.
   */
  private void postProgressUpdateData(LocalReading localReading) {
    if(localReading != null) {
      publishProgress(new ReadmillSyncProgressMessage(localReading));
    }
  }

  /**
   * Updates a LocalReading object with metadata from a Readmill Reading.
   * Updates title, author, cover, state, closing remark and touched at.
   * Triggers update of all reading sessions for the given LocalReading.
   *
   * @param localReading  LocalReading to update
   * @param remoteReading Readmill Reading containing canonical metadata
   * @throws JSONException if the readmill response was not properly formatted
   */
  private void updateLocalReading(LocalReading localReading, JSONObject remoteReading) throws JSONException, SQLException {
    Log.i(TAG, "Updating Metadata for Reading: " + localReading.readmillReadingId);

    JSONObject remoteBook = remoteReading.getJSONObject("book");

    localReading.title = remoteBook.getString("title");
    localReading.author = remoteBook.getString("author");
    localReading.readmillTouchedAt = ReadmillApiHelper.parseISO8601ToUnix(remoteReading.getString("touched_at"));
    localReading.readmillState = ReadmillApiHelper.toIntegerState(remoteReading.getString("state"));
    localReading.readmillClosingRemark = remoteReading.optString("closing_remark");

    String coverURL = remoteBook.getString("cover_url");
    if(coverURL.matches("default-cover")) {
      Log.d(TAG, "Not replacing with default cover");
    } else {
      Log.d(TAG, "Replacing old cover url: " + localReading.coverURL + " with server cover: " + coverURL);
      localReading.coverURL = coverURL;
    }

    localReading.lastReadAt = Math.max(localReading.readmillTouchedAt, localReading.lastReadAt);

    mReadingDao.update(localReading);
    syncDependentObjects(localReading, remoteReading);
  }

  private void syncDependentObjects(LocalReading localReading, JSONObject remoteReading) throws JSONException, SQLException {
    syncReadingSessions(localReading, remoteReading);
    syncHighlights(localReading, remoteReading);
  }

  /**
   * Create a LocalReading object from the provided Readmill Reading.
   * Triggers update of all reading sessions for the given LocalReading.
   *
   * @param remoteReading Readmill Reading to create LocalReading for
   * @return the created LocalReading or null
   * @throws JSONException if the readmill response was not properly formatted
   */
  private LocalReading createLocalReading(JSONObject remoteReading) throws JSONException, SQLException {
    LocalReading spawn = Converter.createLocalReadingFromReadingJSON(remoteReading);
    mReadingDao.create(spawn);
    Log.i(TAG, "Created LocalReading for " + spawn.title + " with Readmill id: " + spawn.readmillReadingId);
    syncDependentObjects(spawn, remoteReading);
    return spawn;
  }

  /**
   * Loop through the given list of remote readings and create a local reading
   * for each of them.
   *
   * @param remoteReadings Remote readings to create locally
   * @throws JSONException if the readmill response was not properly formatted
   */
  private void pullReadings(List<JSONObject> remoteReadings) throws JSONException, SQLException {
    int totalCount = remoteReadings.size();
    Log.d(TAG, "Pulling " + totalCount + " new readings");
    for(int currentCount = 0; currentCount < totalCount; currentCount++) {
      if(isCancelled()) { return; }

      JSONObject remoteReading = remoteReadings.get(currentCount);
      JSONObject remoteBook = remoteReading.getJSONObject("book");

      String message = "Adding " + remoteBook.getString("title");
      postProgressUpdateMessage(message, currentCount, totalCount - 1);

      LocalReading spawn = createLocalReading(remoteReading);
      postProgressUpdateData(spawn);
    }
  }

  /**
   * Get updated information about a reading from Readmill and save it locally.
   *
   * @param localToRemoteReadingMap Map of local readings to remote readings
   */
  private void pullChanges(Map<LocalReading, JSONObject> localToRemoteReadingMap) throws SQLException {
    int totalCount = localToRemoteReadingMap.size();
    int currentCount = 0;
    Log.i(TAG, "Pulling changes for " + totalCount + " readings");

    for(Map.Entry<LocalReading, JSONObject> entry : localToRemoteReadingMap.entrySet()) {
      if(isCancelled()) { return; }

      LocalReading localReading = entry.getKey();
      JSONObject remoteReading = entry.getValue();

      postProgressUpdateMessage("Updating " + localReading.title, currentCount++, totalCount - 1);
      try {
        updateLocalReading(localReading, remoteReading);
      } catch(JSONException e) {
        Log.w(TAG, "Failed to update, unexpected JSON format of remote reading: " + (remoteReading == null ? "NULL" : remoteReading.toString()));
      }
      postProgressUpdateData(localReading);
    }
  }

  /**
   * Goes through the list of local readings and remote readings and divides
   * them into three buckets:
   * - Items that have local changes that should be pushed
   * - Items that have remote changes that should be pulled
   * - Items that does not exist locally and should be created
   * <p/>
   * Note that this sync currently does not handle deleting of readings well.
   * A locally deleted reading will be recreated on the next sync from the
   * server, and a reading deleted on the server will not be deleted locally.
   *
   * @param localReadings  List of local readings
   * @param remoteReadings List of remote readings
   * @throws JSONException if the response from Readmill was not correct
   */
  private void syncLocalReadingsWithRemoteReadings(ArrayList<LocalReading> localReadings, ArrayList<JSONObject> remoteReadings) throws JSONException, SQLException {
    Map<LocalReading, JSONObject> pullChangesList = new HashMap<LocalReading, JSONObject>();
    List<LocalReading> pushClosedStateList = new ArrayList<LocalReading>();
    List<JSONObject> pullReadingsList = new ArrayList<JSONObject>();
    Log.i(TAG, "Performing a sync between " + localReadings.size() + " local readings and " + remoteReadings.size() + " remote readings");

    for(JSONObject remoteReading : remoteReadings) {
      long remoteReadingId = remoteReading.getLong("id");
      Log.d(TAG, "Looking for a local reading matching remote id: " + remoteReadingId);

      if(isCancelled()) { return; }

      boolean foundLocal = false;
      for(LocalReading localReading : localReadings) {
        foundLocal = localReading.readmillReadingId == remoteReadingId;
        if(!foundLocal) {
          continue; // Not this reading
        } else if(remoteReading.getString("state").equals("interesting")) {
          Log.d(TAG, " - Found but is state: interesting, ignoring");
          break; // Don't involve "interesting" readings
        }
        Log.d(TAG, " - Found in local reading with id: " + localReading.id);

        // Resolve sync
        long remoteTouchedAt = ReadmillApiHelper.parseISO8601ToUnix(remoteReading.getString("touched_at"));

        if(closedLocallyButNotRemotely(localReading, remoteReading)) {
          Log.d(TAG, "Local reading has been closed, readmill id:" + remoteReadingId);
          pushClosedStateList.add(localReading);
        } else if(remoteTouchedAt != localReading.readmillTouchedAt) {
          Log.d(TAG, "Remote reading has changed, readmill id: " + remoteReadingId);
          Log.d(TAG, " - Local timestamp: " + localReading.readmillTouchedAt + " vs remote: " + remoteTouchedAt);
          pullChangesList.put(localReading, remoteReading);
        } else {
          Log.d(TAG, "No changes detected, readmill id: " + remoteReadingId);
        }

        break;
      }

      if(!foundLocal) {
        Log.d(TAG, "Local reading is missing, readmill id: " + remoteReadingId);
        pullReadingsList.add(remoteReading);
      }
    }

    // TODO Handle deleted readings (need a deleted at on the reading for that)
    pushClosedStates(pushClosedStateList);
    pullChanges(pullChangesList);
    pullReadings(pullReadingsList);
  }

  /*
   * Push a local change to Readmill.
   * Currently only supports syncing the state and closing remark.
   */
  private void pushClosedStates(List<LocalReading> localReadings) {
    Log.d(TAG, "Pushing " + localReadings.size() + " readings");
    int totalCount = localReadings.size();
    int currentCount = 0;

    for(LocalReading localReading : localReadings) {
      if(isCancelled()) { return; }
      postProgressUpdateMessage("Updating " + localReading.title, currentCount++, totalCount - 1);
      mReadmillApi.closeReading(localReading.readmillReadingId, localReading.readmillState, localReading.readmillClosingRemark);
    }
  }

  /**
   * Determine if the local reading has been closed locally, but is not yet
   * closed on Readmill.
   *
   * @param localReading  the local reading
   * @param remoteReading the co-responding remote reading
   * @return true if the the local reading should be pushed to Readmill
   * @throws JSONException if the response from Readmill is not properly formatted
   */
  private boolean closedLocallyButNotRemotely(LocalReading localReading, JSONObject remoteReading) throws JSONException {
    boolean isClosedLocally = localReading.locallyClosedAt != 0;

    String remoteState = remoteReading.getString("state");
    boolean isClosedRemotely = remoteState.equals("finished") || remoteState.equals("abandoned");

    return isClosedLocally && !isClosedRemotely;
  }

  /**
   * Syncs all reading sessions between from a remote reading to a local reading
   *
   * @param localReading  LocalReading to sync reading sessions for
   * @param remoteReading Readmill Reading for which to get sessions
   * @throws JSONException if the remote reading is not in the expected format
   */
  private void syncReadingSessions(LocalReading localReading, JSONObject remoteReading) throws JSONException, SQLException {
    long remoteId = remoteReading.getLong("id");
    Log.i(TAG, "Syncing reading sessions for reading with id:" + remoteId);

    ArrayList<LocalSession> localSessions = (ArrayList<LocalSession>) mSessionDao.queryBuilder()
      .where().eq(LocalSession.READING_ID_FIELD_NAME, localReading.id)
      .query();

    ArrayList<JSONObject> remoteSessions = mReadmillApi.getPeriodsForReadingId(remoteId);

    localSessions = mergeSessions(localReading, remoteSessions, localSessions);

    updateLocalReadingFromSessions(localReading, localSessions);
  }

  /**
   * Syncs highlights from a remote reading to a local reading.
   *
   * @param localReading  local reading
   * @param remoteReading remote reading
   * @return true if the sync was successful, false otherwise
   * @throws JSONException if the response from Readmill is not properly formatted
   */
  private boolean syncHighlights(LocalReading localReading,
                                 JSONObject remoteReading) throws JSONException, SQLException {
    long remoteId = remoteReading.getLong("id");
    Log.i(TAG, "Syncing all highlights for local reading " + localReading.toString() + " with remote reading: " + remoteId);

    ArrayList<LocalHighlight> localHighlights = (ArrayList<LocalHighlight>) mHighlightDao.queryBuilder()
      .where().eq(LocalHighlight.READMILL_READING_ID_FIELD_NAME, remoteId)
      .query();

    ArrayList<JSONObject> remoteHighlights =
      mReadmillApi.getHighlightsWithReadingId(remoteId);

    mergeHighlights(localReading, localHighlights, remoteHighlights);
    return true;
  }

  /**
   * Merges a list of local highlights with a list of remote highlights.
   * <p/>
   * Highlights that are in the remote list but not in the local set are added to the device.
   *
   * @param localReading     LocalReading that owns the local highlights
   * @param localHighlights  list of local highlights
   * @param remoteHighlights list of remote highlights
   * @throws JSONException if the Readmill response is not properly formatted
   */
  private void mergeHighlights(LocalReading localReading, List<LocalHighlight> localHighlights, ArrayList<JSONObject> remoteHighlights) throws JSONException, SQLException {
    if(remoteHighlights == null || localHighlights == null) {
      Log.d(TAG, "Received NULL list - aborting");
      return;
    }

    Log.i(TAG, "Merging " + localHighlights.size() +
      " local highlights with " + remoteHighlights.size() +
      " remote highlights for reading " + localReading.id);

    // Store all local highlights in a cache for fast access
    HashMap<Long, LocalHighlight> localIds = new HashMap<Long, LocalHighlight>(localHighlights.size());
    for(LocalHighlight highlight : localHighlights) {
      localIds.put(highlight.readmillHighlightId, highlight);
    }

    for(JSONObject remoteHighlight : remoteHighlights) {
      long remoteId = remoteHighlight.getLong("id");
      LocalHighlight localHighlight = localIds.get(remoteId);
      if(localHighlight == null) { // Create highlight

        String remoteContent = remoteHighlight.getString("content");
        Log.i(TAG, "Adding highlight: " + remoteId + " " + remoteContent);

        LocalHighlight spawn = Converter.createHighlightFromReadmillJSON(remoteHighlight);

        // Attributes only available to ReadTracker
        spawn.syncedAt = new Date();
        spawn.readingId = localReading.id;

        mHighlightDao.create(spawn);
        Log.d(TAG, "   Created Highlight: " + spawn.toString());
      } else { // Update highlight
      }
    }
  }

  /**
   * Gets a list of local readings that matches the remote user id.
   *
   * @param remoteUserId Remote user id to get local reading data for
   * @return the list of matched local readings
   */
  private ArrayList<LocalReading> getAllConnectedLocalReadingForUserId(long remoteUserId) throws SQLException {
    Where<LocalReading, Integer> stmt = mReadingDao.queryBuilder().where().
      eq(LocalReading.READMILL_USER_ID_FIELD_NAME, remoteUserId).
      and().
      gt(LocalReading.READMILL_READING_ID_FIELD_NAME, 0);
    ArrayList<LocalReading> result = (ArrayList<LocalReading>) stmt.query();
    Log.i(TAG, "Found " + result.size() + " local connected readings");
    return result;
  }

  /**
   * Get all readings for a given user from Readmill.
   *
   * @param remoteUserId readmill user id to fetch readings for
   * @return the list of user readings
   * @throws ReadmillException if the request to readmill was not successful
   */
  private ArrayList<JSONObject> getAllRemoteReadingsForUserId(long remoteUserId) throws ReadmillException {
    ArrayList<JSONObject> result = mReadmillApi.getReadingsForUserId(remoteUserId);
    Log.i(TAG, "Found " + result.size() + " remote readings");
    return result;
  }

  /**
   * Loop through all reading sessions and update the correct time spent,
   * as well as the most recent progress.
   *
   * @param localReading LocalReading to update
   * @param sessions     A prefetched list of Reading Sessions for the given LocalReading
   */
  private void updateLocalReadingFromSessions(LocalReading localReading, List<LocalSession> sessions) throws SQLException {
    long totalSpentSeconds = 0;
    LocalSession mostRecentSession = null;

    for(LocalSession session : sessions) {
      totalSpentSeconds += session.durationSeconds;
      if(mostRecentSession == null || session.occurredAt.after(mostRecentSession.occurredAt)) {
        mostRecentSession = session;
      }
    }

    localReading.timeSpentMillis = totalSpentSeconds * 1000;

    // Set the progress of the reading to that of the latest reading
    if(mostRecentSession != null) {
      if(mostRecentSession.endedOnPage == -1) {
        Log.d(TAG, "Updating reading progress to progress of most recent session: " + mostRecentSession.progress + " | page " + (long) Math.floor(mostRecentSession.progress * localReading.totalPages));
        localReading.currentPage = (long) Math.floor(mostRecentSession.progress * localReading.totalPages);
      } else {
        localReading.currentPage = mostRecentSession.endedOnPage;
      }
    }

    mReadingDao.update(localReading);
  }

  /**
   * Merges a set of local reading sessions with a set of remote reading
   * sessions (Readmill periods).
   *
   * @param localReading   Parent reading data for the local reading sessions
   * @param remoteSessions local reading sessions
   * @param localSessions  remote reading sessions (Readmill periods)
   * @return the list of updated local sessions
   * @throws JSONException if any of the remote sessions is not correctly formatted
   */
  private ArrayList<LocalSession> mergeSessions(LocalReading localReading,
                                                ArrayList<JSONObject> remoteSessions,
                                                List<LocalSession> localSessions)
    throws JSONException, SQLException {
    Log.d(TAG, "Merging " + remoteSessions.size() + " remote sessions with " + localSessions.size() + " local sessions");

    ArrayList<LocalSession> createdSessions = new ArrayList<LocalSession>();

    for(JSONObject remoteSession : remoteSessions) {
      // Figure out if we need to create the reading session locally or not
      boolean foundLocal = false;
      for(LocalSession localSession : localSessions) {
        String remoteSessionIdentifier = remoteSession.getString("identifier");
        Log.v(TAG, "Comparing local session-id: " + localSession.sessionIdentifier + " to remote session-id: " + remoteSessionIdentifier);
        if(localSession.sessionIdentifier != null &&
          localSession.sessionIdentifier.equals(remoteSessionIdentifier)) {
          foundLocal = true;
          break;
        }
      }

      // session already available so no need to create a new one
      if(foundLocal) { continue; }

      LocalSession spawn = Converter.createReadingSessionFromReadmillPeriod(remoteSession);
      spawn.readingId = localReading.id;
      spawn.syncedWithReadmill = true;

      mSessionDao.create(spawn);
      Log.d(TAG, "Created Session");
      createdSessions.add(spawn);
    }

    createdSessions.addAll(localSessions);
    return createdSessions;
  }
}

