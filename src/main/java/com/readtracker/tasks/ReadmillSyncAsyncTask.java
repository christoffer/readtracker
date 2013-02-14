package com.readtracker.tasks;

import android.os.AsyncTask;
import android.util.Log;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.Where;
import com.readtracker.ApplicationReadTracker;
import com.readtracker.interfaces.ReadmillSyncProgressListener;
import com.readtracker.db.*;
import com.readtracker.support.*;
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
  private boolean mIsFullSync = false;

  ReadmillSyncProgressListener mProgressListener;

  public ReadmillSyncAsyncTask(ReadmillSyncProgressListener progressListener, ReadmillApiHelper readmillApi, boolean fullSync) {
    super();
    mProgressListener = progressListener;
    mReadmillApi = readmillApi;
    mIsFullSync = fullSync;
  }

  @Override
  protected Integer doInBackground(Long... id) {
    long readmillUserId = id[0];

    Log.i(TAG, "Starting " + (mIsFullSync ? "full " : "quick ") + " Readmill sync");

    try {
      mReadingDao = ApplicationReadTracker.getReadingDao();
      mSessionDao = ApplicationReadTracker.getSessionDao();
      mHighlightDao = ApplicationReadTracker.getHighlightDao();
    } catch(SQLException e) {
      Log.e(TAG, "Failed to get DAOs", e);
      return STATUS_ERROR;
    }

    try {
      syncUser(readmillUserId, mIsFullSync);
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
      switch(message.getMessageType()) {
        case READING_CHANGED:
          mProgressListener.onReadingUpdated(message.getLocalReading());
          break;
        case READING_DELETED:
          mProgressListener.onReadingDeleted(message.getLocalReading().id);
          break;
        case SYNC_PROGRESS:
          mProgressListener.onSyncProgress(message.getMessage(), message.getProgress());
          break;
      }
    }
  }

  /**
   * Syncs all local and remote data for a given user.
   *
   * @param readmillUserId readmill id of user to sync data for
   * @param fullSync       skip change detection and force update of local data
   * @throws ReadmillException if an error occurs while communicating with Readmill
   * @throws JSONException     if the response from Readmill is not properly formatted
   */
  private void syncUser(long readmillUserId, boolean fullSync) throws ReadmillException, JSONException, SQLException {
    Log.i(TAG, "Syncing user with Readmill id: " + readmillUserId);

    ArrayList<LocalReading> localReadings = getAllConnectedLocalReadingForUserId(readmillUserId);
    ArrayList<JSONObject> remoteReadings = getAllRemoteReadingsForUserId(readmillUserId);

    syncLocalReadingsWithRemoteReadings(localReadings, remoteReadings, fullSync);
  }

  /**
   * Posts a generic message and a progress.
   *
   * @param message     Message to post
   * @param currentStep current step
   * @param totalSteps  total steps
   */
  private void postProgressUpdateMessage(String message, int currentStep, int totalSteps) {
    final float progress = ((float) currentStep / totalSteps);
    publishProgress(ReadmillSyncProgressMessage.syncProgress(progress, message));
  }

  /**
   * Puts a reading as a progress update message.
   *
   * @param localReading Reading to post as progress update.
   */
  private void postProgressUpdateData(LocalReading localReading) {
    if(localReading != null) {
      publishProgress(ReadmillSyncProgressMessage.readingChanged(localReading));
    }
  }

  private void postProgressUpdateDeletedReading(LocalReading deletedLocalReading) {
    if(deletedLocalReading != null) {
      publishProgress(ReadmillSyncProgressMessage.readingDeleted(deletedLocalReading));
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
  private void updateLocalReadingMetadata(LocalReading localReading, JSONObject remoteReading) throws JSONException, SQLException {
    Log.i(TAG, "Updating Metadata for Reading: " + localReading.readmillReadingId);

    JSONObject remoteBook = remoteReading.getJSONObject("book");

    localReading.title = remoteBook.getString("title");
    localReading.author = remoteBook.getString("author");
    localReading.setTouchedAt(ReadmillApiHelper.parseISO8601(remoteReading.getString("touched_at")));
    localReading.readmillState = ReadmillApiHelper.toIntegerState(remoteReading.getString("state"));
    localReading.readmillClosingRemark = ReadmillConverter.optString("closing_remark", null, remoteReading);

    String coverURL = remoteBook.getString("cover_url");
    if(coverURL.matches("default-cover")) {
      Log.v(TAG, "Not replacing with default cover");
    } else {
      Log.i(TAG, "Replacing old cover url: " + localReading.coverURL + " with server cover: " + coverURL);
      localReading.coverURL = coverURL;
    }

    if(localReading.getTouchedAt().after(localReading.getLastReadAt())) {
      localReading.setLastReadAt(localReading.getTouchedAt());
    }

    mReadingDao.update(localReading);
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
    LocalReading spawn = ReadmillConverter.createLocalReadingFromReadingJSON(remoteReading);
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
  private void createLocalReadings(List<JSONObject> remoteReadings) throws JSONException, SQLException {
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
        updateLocalReadingMetadata(localReading, remoteReading);
        syncDependentObjects(localReading, remoteReading);
      } catch(JSONException e) {
        Log.w(TAG, "Failed to update, unexpected JSON format of remote reading: " + (remoteReading == null ? "NULL" : remoteReading.toString()));
      }
      postProgressUpdateData(localReading);
    }
  }

  /**
   * Goes through the list of local readings and remote readings and divides
   * them into these buckets:
   * - Items that have local changes that should be pushed
   * - Items that have remote changes that should be pulled
   * - Items that does not exist locally and should be created
   * - Items that have been deleted locally and should be deleted remotely
   * - Items that maybe have become orphans (deleted remotely but not locally)
   * <p/>
   * Note that this sync currently does not handle deleting of readings well.
   * A locally deleted reading will be recreated on the next sync from the
   * server, and a reading deleted on the server will not be deleted locally.
   *
   * @param localReadings  List of local readings
   * @param remoteReadings List of remote readings
   * @throws JSONException if the response from Readmill was not correct
   */
  private void syncLocalReadingsWithRemoteReadings(ArrayList<LocalReading> localReadings, ArrayList<JSONObject> remoteReadings, boolean fullSync)
    throws JSONException, SQLException, ReadmillException {
    Map<LocalReading, JSONObject> pullChangesList = new HashMap<LocalReading, JSONObject>();
    List<LocalReading> pushClosedStateList = new ArrayList<LocalReading>();
    List<JSONObject> pullReadingsList = new ArrayList<JSONObject>();
    List<LocalReading> readingsToDelete = new ArrayList<LocalReading>();
    List<LocalReading> possibleOrphans = new ArrayList<LocalReading>();

    Log.i(TAG, "Performing a sync between " + localReadings.size() + " local readings and " + remoteReadings.size() + " remote readings");

    // Check for orphaned local readings. That is readings that have been connected,
    // but are now deleted on the server.
    for(LocalReading localReading : localReadings) {
      if(!localReading.isConnected()) {
        // Unconnected readings can't be orphans
        continue;
      }

      boolean remoteFound = false;
      for(JSONObject remoteReading : remoteReadings) {
        final long remoteReadingId = remoteReading.getLong("id");
        if(remoteReadingId == localReading.readmillReadingId) {
          remoteFound = true;
          break;
        }
      }

      if(!remoteFound) {
        possibleOrphans.add(localReading);
      }
    }

    for(JSONObject remoteReading : remoteReadings) {
      final long remoteReadingId = remoteReading.getLong("id");
      Log.v(TAG, "Looking for a local reading matching remote id: " + remoteReadingId);

      if(isCancelled()) { return; }

      boolean foundLocal = false;
      for(LocalReading localReading : localReadings) {
        foundLocal = localReading.readmillReadingId == remoteReadingId;
        if(!foundLocal) {
          continue; // Not this reading
        } else if(remoteReading.getString("state").equals("interesting")) {
          Log.v(TAG, " - Found but is state: interesting, ignoring");
          break; // Don't involve "interesting" readings
        }
        Log.v(TAG, " - Found in local reading with id: " + localReading.id);

        // Resolve sync
        long remoteTouchedAt = ReadmillApiHelper.parseISO8601ToUnix(remoteReading.getString("touched_at"));

        if(localReading.deletedByUser) {
          Log.d(TAG, "Local reading has been deleted: " + localReading.readmillReadingId);
          readingsToDelete.add(localReading);
        } else if(closedLocallyButNotRemotely(localReading, remoteReading)) {
          Log.d(TAG, "Local reading has been closed, readmill id:" + remoteReadingId);
          pushClosedStateList.add(localReading);
        } else if(fullSync || localReading.touchedAtDifferentFrom(remoteTouchedAt)) {
          Log.d(TAG, "Remote reading has changed, readmill id: " + remoteReadingId);
          Log.d(TAG, " - Local timestamp: " + (0.001 * localReading.getTouchedAt().getTime()) + " vs remote: " + remoteTouchedAt);
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

    confirmAndDeleteOrphans(possibleOrphans);
    pushDeletions(readingsToDelete);
    pushClosedStates(pushClosedStateList);
    pullChanges(pullChangesList);
    createLocalReadings(pullReadingsList);
  }

  /**
   * Delete local readings after being confirmed as orphans.
   * Confirming a reading as orphan is done by verifying that the Readmill server
   * responds with a 404 for the given reading.
   */
  private void confirmAndDeleteOrphans(List<LocalReading> maybeOrphanLocalReadings) throws SQLException {
    for(LocalReading localReading : maybeOrphanLocalReadings) {
      if(verifyReadingNotOnReadmill(localReading.readmillReadingId)) {
        Log.v(TAG, "Verified reading not on Readmill: " + localReading);
        Log.i(TAG, "Deleting remotely deleted reading: " + localReading);
        mReadingDao.delete(localReading);
        postProgressUpdateDeletedReading(localReading);
      } else {
        Log.v(TAG, "Could not verify that reading is not readmill: " + localReading);
      }
    }
  }

  /**
   * Delete the remote and the local reading.
   *
   * @param localReadings Local readings to delete
   */
  private void pushDeletions(List<LocalReading> localReadings) throws SQLException, ReadmillException {
    Log.d(TAG, "Deleting " + localReadings.size() + " readings");
    for(LocalReading localReading : localReadings) {
      Log.v(TAG, "Deleting Readmill Reading with id: " + localReading.readmillReadingId);
      mReadmillApi.deleteReading(localReading.readmillReadingId);
      Log.v(TAG, "Reading deleted on Readmill. Deleting locally: " + localReading);
      mReadingDao.delete(localReading);
    }
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
   * Sends a request to readmill for the given ID and asserts that the response is a 404,
   * and is in fact coming from Readmill (to avoid airport proxy hijacking etc).
   *
   * @param readingId Readmill id to verify
   * @return true if Readmill does not have a reading with the given ID.
   */
  private boolean verifyReadingNotOnReadmill(long readingId) {
    return mReadmillApi.verifyReadingMissing(readingId);
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
    boolean isClosedLocally = localReading.hasClosedAt();

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
  private void mergeHighlights(LocalReading localReading,
                               List<LocalHighlight> localHighlights,
                               ArrayList<JSONObject> remoteHighlights) throws JSONException, SQLException {
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

        LocalHighlight spawn = ReadmillConverter.createHighlightFromReadmillJSON(remoteHighlight);

        // Attributes only available to ReadTracker
        spawn.syncedAt = new Date();
        spawn.readingId = localReading.id;

        mHighlightDao.create(spawn);
        Log.d(TAG, "   Created Highlight: " + spawn.toString());
      } else { // Update highlight
        ReadmillConverter.mergeLocalHighlightWithJson(localHighlight, remoteHighlight);
        localHighlight.syncedAt = new Date();

        mHighlightDao.update(localHighlight);
        Log.d(TAG, "   Updated Highlight: " + localHighlight.toString());
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

    localReading.setProgressStops(sessions);

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
      // TODO Use a set here instead for faster look ups
      for(LocalSession localSession : localSessions) {
        String remoteSessionIdentifier = remoteSession.getString("identifier");
        if(localSession.sessionIdentifier != null &&
          localSession.sessionIdentifier.equals(remoteSessionIdentifier)) {
          foundLocal = true;
          break;
        }
      }

      // session already available so no need to create a new one
      if(foundLocal) { continue; }

      LocalSession spawn = ReadmillConverter.createReadingSessionFromReadmillPeriod(remoteSession);
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

