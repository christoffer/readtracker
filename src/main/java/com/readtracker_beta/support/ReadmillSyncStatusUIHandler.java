package com.readtracker_beta.support;

import android.app.Activity;
import android.util.Log;
import android.view.View;
import android.view.ViewStub;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.readtracker_beta.R;
import com.readtracker_beta.interfaces.ReadmillSyncProgressListener;
import com.readtracker_beta.db.LocalReading;

/**
 * Handles updates of the sync progress notification on home screen
 */
public class ReadmillSyncStatusUIHandler implements ReadmillSyncProgressListener {
  private static final String TAG = ReadmillSyncStatusUIHandler.class.getName();

  // Resource id of the view stub to inflate when the sync is show
  // for the first time
  private int mStubResourceId;

  // The handler for updated readings
  private SyncUpdateHandler mSyncUpdateHandler;

  private Activity mParentActivity;

  private LinearLayout mLayoutSyncProgress;
  private ProgressBar mProgressSync;
  private TextView mProgressMessage;

  /**
   * Describes the result of a sync. A successful sync will return with SyncStatus.OK,
   * anything else is a failure.
   */
  public enum SyncStatus {
    OK,            // Successful sync
    INVALID_TOKEN, // Provided token is invalid
    ERROR          // General error
  }

  /**
   * Hooks for the HomeActivity during the sync.
   */
  public interface SyncUpdateHandler {
    /**
     * Called when a local reading has been added or modified
     *
     * @param localReading the updated or added local reading
     */
    public void onReadingUpdate(LocalReading localReading);

    /**
     * Called when a local reading has been deleted
     *
     * @param localReadingId id of the deleted reading
     */
    public void onReadingDelete(int localReadingId);

    /**
     * Called when the synchronization has completed
     */
    public void onSyncComplete(SyncStatus status);
  }

  /**
   * Creates a sync handler that manages the visual details of showing a
   * notification when a Readmill sync is in progress.
   * Delegates any actual data changes to a ReadmillUpdateListener.
   *
   * @param stubResourceId id of the sync status resource to inflate on first show
   * @param parentActivity activity that holds the stub resource (used for inflating)
   * @param updateHandler  handler for doing something with changed readings
   */
  public ReadmillSyncStatusUIHandler(int stubResourceId, Activity parentActivity, SyncUpdateHandler updateHandler) {
    mParentActivity = parentActivity;
    mSyncUpdateHandler = updateHandler;
    mStubResourceId = stubResourceId;
  }

  @Override public void onSyncStart() {
    Log.d(TAG, "Readmill sync started");
    inflateSyncBar();
    showSyncBar();
    mProgressMessage.setText("Syncing with Readmill...");
    mProgressSync.setProgress(0);
  }

  @Override public void onSyncDone() {
    Log.d(TAG, "Readmill sync done");
    mProgressMessage.setText("Synchronized");
    mProgressSync.setProgress(100);
    hideSyncBar();
    mSyncUpdateHandler.onSyncComplete(SyncStatus.OK);
  }

  @Override public void onSyncProgress(String message, Float progress) {
    Log.d(TAG, "Readmill sync progress: " + (message == null ? "NULL" : message) + " progress: " + progress);
    if(message != null && message.length() > 0) {
      mProgressMessage.setText(message);
    }

    if(progress != null) {
      mProgressSync.setProgress((int) (progress * 100));
    }
  }

  @Override public void onSyncFailed(String message, int HTTPStatusCode) {
    Log.d(TAG, "Readmill sync failed: " + message + " with code: " + HTTPStatusCode);
    if(HTTPStatusCode == 401) {
      mSyncUpdateHandler.onSyncComplete(SyncStatus.INVALID_TOKEN);
    } else {
      mSyncUpdateHandler.onSyncComplete(SyncStatus.ERROR);
    }
  }

  @Override public void onReadingUpdated(LocalReading localReading) {
    Log.d(TAG, "Readmill sync reading updated: " + localReading.id);
    if(mSyncUpdateHandler != null) {
      mSyncUpdateHandler.onReadingUpdate(localReading);
    }
  }

  @Override public void onReadingDeleted(int localReadingId) {
    Log.d(TAG, "Readmill sync reading deleted: " + localReadingId);
    if(mSyncUpdateHandler != null) {
      mSyncUpdateHandler.onReadingDelete(localReadingId);
    }
  }

  /**
   * Inflates the stub if not already inflated
   */
  private void inflateSyncBar() {
    if(mLayoutSyncProgress == null) {
      mLayoutSyncProgress = (LinearLayout) ((ViewStub) mParentActivity.findViewById(mStubResourceId)).inflate();
      mLayoutSyncProgress.bringToFront();
      mProgressSync = (ProgressBar) mLayoutSyncProgress.findViewById(R.id.progressProgress);
      mProgressMessage = (TextView) mLayoutSyncProgress.findViewById(R.id.textProgressMessage);
    }
  }

  /**
   * Shows the sync bar
   */
  private void showSyncBar() {
    final Animation slide = AnimationUtils.loadAnimation(mParentActivity, R.anim.slide_up_appear);
    slide.setFillAfter(true);
    mLayoutSyncProgress.clearAnimation();
    mLayoutSyncProgress.startAnimation(slide);
    mLayoutSyncProgress.setVisibility(View.VISIBLE);
  }

  /**
   * Hides the sync bar
   */
  private void hideSyncBar() {
    final Animation slide = AnimationUtils.loadAnimation(mParentActivity, R.anim.slide_down_disappear);
    slide.setFillAfter(true);
    slide.setStartOffset(900); // Let the bar linger for a little while to show the final message
    mLayoutSyncProgress.clearAnimation();
    slide.setAnimationListener(new Animation.AnimationListener() {
      @Override public void onAnimationStart(Animation animation) { }

      @Override public void onAnimationRepeat(Animation animation) { }

      @Override public void onAnimationEnd(Animation animation) {
        mLayoutSyncProgress.setVisibility(View.GONE);
      }
    });
    mLayoutSyncProgress.startAnimation(slide);
  }
}

//  public void onReadmillSyncStart() {
//    Log.i(TAG, "Readmill sync started");
//
//    if(mMenuReadmillSync != null) {
//      mMenuReadmillSync.setEnabled(false);
//      mButtonSyncReadmill.setEnabled(false);
//    }
//
//    if(mLayoutSyncProgress == null) {
//      mLayoutSyncProgress = (LinearLayout) ((ViewStub) findViewById(R.id.stub_sync_progress)).inflate();
//      mLayoutSyncProgress.bringToFront();
//      findViewById(R.id.layoutActionBar).bringToFront();
//      mProgressSync = (ProgressBar) mLayoutSyncProgress.findViewById(R.id.progressProgress);
//      mProgressMessage = (TextView) mLayoutSyncProgress.findViewById(R.id.textProgressMessage);
//      Animation slide = AnimationUtils.loadAnimation(this, R.anim.slide_up_appear);
//      slide.setFillAfter(true);
//      mLayoutSyncProgress.clearAnimation();
//      mLayoutSyncProgress.startAnimation(slide);
//    }
//    mLayoutSyncProgress.setVisibility(View.VISIBLE);
//    mProgressMessage.setText("Syncing with Readmill...");
//    mProgressSync.setProgress(0);
//  }
//
//  public void onReadmillSyncDone(Boolean success) {
//    if(success == null) {
//      Log.i(TAG, "Sync aborted");
//    }
//
//    Log.d(TAG, "Readmill Sync Completed with success: " + success);
//
//    if(mProgressMessage != null && success != null) {
//      if(mProgressSync != null) {
//        mProgressSync.setProgress(mProgressSync.getMax());
//      }
//      if(success) {
//        mProgressMessage.setText("Sync complete");
//      } else {
//        mProgressMessage.setText("There was an error syncing your data. Maybe bad internet connection?");
//      }
//      Animation slide = AnimationUtils.loadAnimation(this, R.anim.slide_down_appear);
//      slide.setFillAfter(true);
//      slide.setStartOffset(1000);
//      mLayoutSyncProgress.clearAnimation();
//      mLayoutSyncProgress.startAnimation(slide);
//    } else {
//      mLayoutSyncProgress.clearAnimation();
//    }
//
//    if(mMenuReadmillSync != null) {
//      mMenuReadmillSync.setEnabled(true);
//      mButtonSyncReadmill.setEnabled(true);
//      //      if(mReadingListAdapter.getCount() == 0) {
//      //        mButtonSyncReadmill.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in_half));
//      //      }
//    }
//
//  }
//
//  /**
//   * Called when an ongoing sync with readmill has a new update message
//   *
//   * @param message The update message
//   */
//  public void onReadmillSyncUpdate(ReadmillSyncProgressMessage message) {
//    if(message.toString().length() > 0 && mProgressMessage != null) {
//      mProgressMessage.setText(message.toString());
//    }
//
//    if(message.getProgress() != null && mProgressSync != null) {
//      mProgressSync.setProgress((int) (message.getProgress() * 100));
//    }
//
//    if(message.getLocalReading() != null) {
//      Log.i(TAG, "Add or update LocalReading: " + message.getLocalReading().toString());
//      mHomeFragmentAdapter.put(message.getLocalReading());
//    }
//  }
