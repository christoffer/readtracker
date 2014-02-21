package com.readtracker.android.activities;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.readtracker.android.ReadTrackerApp;
import com.readtracker.android.IntentKeys;
import com.readtracker.android.R;
import com.readtracker.android.custom_views.ProgressPicker;
import com.readtracker.android.db.LocalReading;
import com.readtracker.android.db.LocalSession;
import com.readtracker.android.interfaces.EndSessionDialogListener;
import com.readtracker.android.support.DrawableGenerator;
import com.readtracker.android.thirdparty.SafeViewFlipper;

import java.sql.SQLException;
import java.util.Date;
import java.util.UUID;

/**
 * Screen for input the ending page of a reading session
 */
public class EndSessionDialog extends DialogFragment {
  private static final String TAG = EndSessionDialog.class.getName();
  private static final int SAVE_BUTTON_PAGE = 0;
  private static final int FINISH_BUTTON_PAGE = 1;

  public static final String ARG_BOOK_ID = "BOOK_ID";
  public static final String ARG_SESSION_LENGTH_MS = "SESSION_LENGTH";

  private static Button mButtonSaveProgress;
  private static Button mButtonFinishBook;

  private static SafeViewFlipper mFlipperActionButtons;

  private static ProgressPicker mProgressPicker;

  // Local reading to edit
  private LocalReading mLocalReading;

  // Length of the reading session
  private long mSessionDuration;

  // The current page
  private int mCurrentPage;

  public EndSessionDialog() {
  }

  @Override public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Log.v(TAG, "onCreate()");

    if(savedInstanceState != null) {
      Log.v(TAG, "thawing");
      mLocalReading = savedInstanceState.getParcelable(IntentKeys.LOCAL_READING);
      mSessionDuration = savedInstanceState.getLong(IntentKeys.SESSION_LENGTH_MS);
      mCurrentPage = savedInstanceState.getInt(IntentKeys.PAGE);
    } else {
      Log.v(TAG, "init from arguments");
      mLocalReading = getArguments().getParcelable(IntentKeys.LOCAL_READING);
      mSessionDuration = getArguments().getLong(IntentKeys.SESSION_LENGTH_MS);
      mCurrentPage = (int) mLocalReading.currentPage;
    }

    setStyle(STYLE_NO_TITLE, android.R.style.Theme_Dialog);
  }

  @Override public void onSaveInstanceState(Bundle out) {
    super.onSaveInstanceState(out);
    Log.v(TAG, "onSaveInstanceState()");
    out.putParcelable(IntentKeys.LOCAL_READING, mLocalReading);
    out.putLong(IntentKeys.SESSION_LENGTH_MS, mSessionDuration);
    if(mProgressPicker != null) {
      out.putLong(IntentKeys.PAGE, mProgressPicker.getCurrentPage());
    } else {
      out.putLong(IntentKeys.PAGE, mLocalReading.currentPage);
    }
  }

  @Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    Log.v(TAG, "onCreateView()");
    View root = inflater.inflate(R.layout.end_session_dialog, null);

    bindViews(root);

    mProgressPicker.setupForLocalReading(mLocalReading);

    mProgressPicker.setCurrentPage(mCurrentPage);

    final boolean onLastPage = mCurrentPage == mLocalReading.totalPages;
    toggleFinishButton(onLastPage);

    Log.i(TAG, "Init for reading : " + mLocalReading.id + " with session length:" + mSessionDuration);
    bindEvents();

    mButtonSaveProgress.setBackgroundDrawable(DrawableGenerator.generateButtonBackground(mLocalReading.getColor()));
    mButtonFinishBook.setBackgroundDrawable(DrawableGenerator.generateButtonBackground(mLocalReading.getColor()));

    return root;
  }

  @Override public void onActivityResult(int requestCode, int resultCode, Intent data) {
    switch(requestCode) {
      case ActivityCodes.REQUEST_FINISH_READING:
        if(resultCode == ActivityCodes.RESULT_OK) {
          // User finished the reading, fall through
          Log.v(TAG, "Reading was finished, exit with success");
          mLocalReading = data.getParcelableExtra(IntentKeys.LOCAL_READING);
          if(mLocalReading != null) {
            final long page = mLocalReading.totalPages;
            saveSessionAndExit(page, mSessionDuration);
          } else {
            Log.w(TAG, "Did not receive expected intent extra for local reading ");
          }
        } else {
          // User cancelled the finish
          Log.v(TAG, "Reading was not finished. Ignoring.");
          return;
        }
        break;
    }
  }

  private void bindViews(View root) {
    mButtonSaveProgress = (Button) root.findViewById(R.id.buttonSaveProgress);
    mButtonFinishBook = (Button) root.findViewById(R.id.buttonFinishBook);
    mFlipperActionButtons = (SafeViewFlipper) root.findViewById(R.id.flipperActionButtons);
    mProgressPicker = (ProgressPicker) root.findViewById(R.id.progressPicker);
  }

  private void bindEvents() {
    mButtonSaveProgress.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        saveSessionAndExit(mProgressPicker.getCurrentPage(), mSessionDuration);
      }
    });

    mButtonFinishBook.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View view) {
        exitToFinishReading(mLocalReading);
      }
    });

    mProgressPicker.setOnProgressChangeListener(new ProgressPicker.OnProgressChangeListener() {
      @Override
      public void onChangeProgress(int newPage) {
        final boolean onLastPage = newPage == mLocalReading.totalPages;

        if(!onLastPage) {
          boolean hasChanged = mLocalReading.currentPage != newPage;
          mButtonSaveProgress.setEnabled(hasChanged);
        } else {
          // Always enable finishing
          mButtonSaveProgress.setEnabled(true);
        }

        toggleFinishButton(onLastPage);
      }
    });
  }

  private void toggleFinishButton(boolean finishMode) {
    if(finishMode && mFlipperActionButtons.getDisplayedChild() != FINISH_BUTTON_PAGE) {
      mFlipperActionButtons.setDisplayedChild(FINISH_BUTTON_PAGE);
    } else if(!finishMode && mFlipperActionButtons.getDisplayedChild() != SAVE_BUTTON_PAGE) {
      mFlipperActionButtons.setDisplayedChild(SAVE_BUTTON_PAGE);
    }
  }

  /**
   * Start the FinishBookActivity with the current reading and await the result.
   *
   * @param localReading The current local reading to finish.
   */
  private void exitToFinishReading(LocalReading localReading) {
    Intent finishActivity = new Intent(getActivity(), FinishBookActivity.class);
    finishActivity.putExtra(IntentKeys.LOCAL_READING, localReading);
    startActivityForResult(finishActivity, ActivityCodes.REQUEST_FINISH_READING);
  }

  private void saveSessionAndExit(long page, long durationMillis) {
    Log.v(TAG, "Exiting with page " + page + " and duration " + durationMillis);
    mLocalReading.setCurrentPage(page);
    mLocalReading.setLastReadAt(new Date());

    // Send off to background task
    UpdateAndCreateSession.createSession(mLocalReading, durationMillis,
            new UpdateAndCreateSession.OnCompleteListener() {
                @Override
                public void onCompleted(LocalSession localSession) {
                    onSessionSaved(localSession);
                }
            }
    );
  }

  private void onSessionSaved(LocalSession localSession) {
    Log.i(TAG, "onSessionSaved: " + localSession);

    if(localSession == null) {
      ((EndSessionDialogListener) getActivity()).onSessionFailed();
    } else {
      Log.i(TAG, "Saved locally, initializing process of queued pings...");
      ((EndSessionDialogListener) getActivity()).onSessionCreated(localSession);
      dismiss();
    }
  }

  // --------------------------------------------------------------------
  // ASyncTask Class
  // --------------------------------------------------------------------

  private static class UpdateAndCreateSession extends AsyncTask<Void, Void, LocalSession> {
    private static final String TAG = UpdateAndCreateSession.class.getName();
    private LocalReading mLocalReading;
    private long mSessionLength;
    private OnCompleteListener mListener;

    public interface OnCompleteListener {
      public void onCompleted(LocalSession localSession);
    }

    public static void createSession(LocalReading localReading, long sessionLengthMillis, OnCompleteListener listener) {
      UpdateAndCreateSession instance = new UpdateAndCreateSession(localReading, sessionLengthMillis, listener);
      //noinspection unchecked
      instance.execute();
    }

    private UpdateAndCreateSession(LocalReading localReading, long sessionLength, OnCompleteListener listener) {
      mLocalReading = localReading;
      mSessionLength = sessionLength;
      mListener = listener;
    }

    @Override protected void onPreExecute() {
      super.onPreExecute();
    }

    @Override protected void onCancelled() {
      super.onCancelled();
    }
    @Override
    protected LocalSession doInBackground(Void... args) {
      Log.i(TAG, "Saving reading with id " + mLocalReading.id);
      LocalSession newSession = generateReadingSession(mLocalReading, mSessionLength);
      Log.d(TAG, "Created session: " + newSession);
      boolean success = updateLocalReading(mLocalReading, mSessionLength) && saveReadingSession(newSession);
      return success ? newSession : null;
    }

    private LocalSession generateReadingSession(final LocalReading localReading, final long sessionLength) {
      final long sessionDurationSeconds = (long) Math.floor((double) sessionLength / 1000.0);

      return new LocalSession() {{
        readingId = localReading.id;
        readmillReadingId = localReading.readmillReadingId;
        durationSeconds = sessionDurationSeconds;
        progress = localReading.progress;
        endedOnPage = (int) localReading.currentPage;
        sessionIdentifier = UUID.randomUUID().toString();
        occurredAt = new Date();
      }};
    }

    private boolean updateLocalReading(LocalReading localReading, long addedDurationMillis) {
      Log.i(TAG, "Updating LocalReading: " + localReading.id + ", adding: " + addedDurationMillis + " milliseconds to time spent");
      try {
        localReading.timeSpentMillis += addedDurationMillis;
        ReadTrackerApp.getReadingDao().update(localReading);
        return true;
      } catch(SQLException e) {
        Log.e(TAG, "Failed to update data", e);
        return false;
      }
    }

    private boolean saveReadingSession(LocalSession session) {
      Log.i(TAG, "Saving session: " + session.readingId);
      try {
        ReadTrackerApp.getLocalSessionDao().create(session);
        return true;
      } catch(SQLException e) {
        Log.e(TAG, "Failed to create Session", e);
        return false;
      }
    }

    @Override
    protected void onPostExecute(LocalSession localSession) {
      if(mListener != null) {
        mListener.onCompleted(localSession);
      }
    }
  }
}
