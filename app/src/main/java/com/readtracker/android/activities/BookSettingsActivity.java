package com.readtracker.android.activities;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.util.Log;
import android.widget.Toast;

import com.readtracker.android.ApplicationReadTracker;
import com.readtracker.android.IntentKeys;
import com.readtracker.android.R;
import com.readtracker.android.SettingsKeys;
import com.readtracker.android.db.LocalReading;
import com.readtracker.android.interfaces.SaveLocalReadingListener;
import com.readtracker.android.tasks.SaveLocalReadingTask;

import java.util.Date;

public class BookSettingsActivity extends PreferenceActivity {
  private static final String TAG = BookSettingsActivity.class.getName();

  private LocalReading mLocalReading;

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    addPreferencesFromResource(R.xml.book_settings);

    if(savedInstanceState == null) {
      mLocalReading = getIntent().getExtras().getParcelable(IntentKeys.LOCAL_READING);
    } else {
      mLocalReading = savedInstanceState.getParcelable(IntentKeys.LOCAL_READING);
    }

    Preference prefDeleteBook = findPreference(SettingsKeys.BOOK_DELETE_BOOK);
    Preference prefEditBookPages = findPreference(SettingsKeys.BOOK_EDIT_PAGES);


    if(((ApplicationReadTracker) getApplication()).getCurrentUser() == null) {
      // Remove Readmill settings for anonymous users
      PreferenceCategory readmillCategory = (PreferenceCategory) findPreference(SettingsKeys.READMILL);
      getPreferenceScreen().removePreference(readmillCategory);
    } else {
      final CheckBoxPreference checkboxReadmillPrivacy = (CheckBoxPreference) findPreference(SettingsKeys.READMILL_PRIVACY);
      checkboxReadmillPrivacy.setChecked(!mLocalReading.readmillPrivate);
      checkboxReadmillPrivacy.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
        @Override public boolean onPreferenceChange(Preference preference, Object newValue) {
          boolean isPrivate = !((Boolean) newValue);
          Log.d(TAG, "Changed privacy to: " + (isPrivate ? "private" : "public"));

          toggleReadingPrivate(isPrivate);
          return true;
        }
      });
    }

    prefDeleteBook.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
      @Override public boolean onPreferenceClick(Preference preference) {
        showConfirmDeleteDialog();
        return true;
      }
    });

    prefEditBookPages.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
      @Override public boolean onPreferenceClick(Preference preference) {
        exitWithRequestPageEdit();
        return true;
      }
    });
  }

  @Override protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putParcelable(IntentKeys.LOCAL_READING, mLocalReading);
  }

  private void toastPrivacyChanged() {
    String message = "Reading is now " + (mLocalReading.readmillPrivate ? "private" : "public") + ".\n";
    message += "Changes to Readmill will be applied on next sync.";

    Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
  }

  private void toastDeleted(boolean isConnected) {
    String message = "Book deleted.";

    if(isConnected) {
      message += " Changes to Readmill will be applied on next sync.";
    }

    Toast.makeText(this, message, Toast.LENGTH_LONG).show();
  }

  private void showConfirmDeleteDialog() {
    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    builder.setTitle("Delete book and all data?");

    if(mLocalReading.isConnected()) {
      builder.setMessage(R.string.delete_reading_connected);
    } else {
      builder.setMessage(R.string.delete_reading_unconnected);
    }

    builder.setCancelable(true);

    builder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
      @Override public void onClick(DialogInterface dialog, int id) {
        deleteReading(mLocalReading);
      }
    });

    builder.setNegativeButton("Cancel", null);

    builder.show();
  }

  private void toggleReadingPrivate(boolean newIsPrivate) {
    if(newIsPrivate == mLocalReading.readmillPrivate) {
      return;
    }

    mLocalReading.readmillPrivate = newIsPrivate;
    mLocalReading.setUpdatedAt(new Date());
    SaveLocalReadingTask.save(mLocalReading, new SaveLocalReadingListener() {
      @Override public void onLocalReadingSaved(LocalReading localReading) {
        toastPrivacyChanged();
      }
    });
  }

  private void deleteReading(LocalReading localReading) {
    Log.i(TAG, "Deleting reading: " + localReading);
    localReading.deletedByUser = true;
    SaveLocalReadingTask.save(localReading, new SaveLocalReadingListener() {
      @Override public void onLocalReadingSaved(LocalReading localReading) {
        toastDeleted(localReading.isConnected());
        setResult(ActivityCodes.RESULT_DELETED_BOOK);
        finish();
      }
    });
  }

  private void exitWithRequestPageEdit() {
    Intent data = new Intent();
    // We need to pass the reading back here since the original activity
    // might have been destroyed, and thus lost it's reference to the LocalReading.
    data.putExtra(IntentKeys.LOCAL_READING, mLocalReading);
    setResult(ActivityCodes.RESULT_REQUESTED_BOOK_SETTINGS, data);
    finish();
  }
}
