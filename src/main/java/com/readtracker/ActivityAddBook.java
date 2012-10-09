package com.readtracker;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ToggleButton;
import com.readtracker.db.LocalReading;
import com.readtracker.tasks.ConnectReadingTask;
import com.readtracker.tasks.ConnectedReadingListener;

/**
 * Screen for adding a new book manually
 */
public class ActivityAddBook extends ReadTrackerActivity {
  public static final String TAG = ActivityAddBook.class.getName();
  private static EditText mEditTitle;
  private static EditText mEditAuthor;
  private static EditText mEditPageCount;

  private static ToggleButton mTogglePagesPercent;
  private static ToggleButton mTogglePublicPrivate;

  // Store the cover url from the intent that starts the activity
  private String mCoverURL;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_add_book);

    bindViews();
    bindEvents();

    Bundle extras = getIntent().getExtras();
    if(extras != null) {
      mEditTitle.setText(extras.getString(IntentKeys.TITLE));
      mEditAuthor.setText(extras.getString(IntentKeys.AUTHOR));

      mCoverURL = extras.getString(IntentKeys.COVER_URL);

      long pageCount = extras.getLong(IntentKeys.PAGE_COUNT, 0);
      if(pageCount > 0) {
        mEditPageCount.setText(Long.toString(pageCount));
      }
    }
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    switch(requestCode) {
      case ActivityCodes.REQUEST_READING_SESSION:
        if(resultCode == RESULT_OK) {
          setResult(RESULT_OK);
          finish();
        }
    }
  }

  private void bindViews() {
    mEditTitle = (EditText) findViewById(R.id.editTitle);
    mEditAuthor = (EditText) findViewById(R.id.editAuthor);
    mEditPageCount = (EditText) findViewById(R.id.editPageCount);
    mTogglePagesPercent = (ToggleButton) findViewById(R.id.togglePagesPercent);
    mTogglePublicPrivate = (ToggleButton) findViewById(R.id.togglePublicPrivate);
  }

  private void bindEvents() {
    Button buttonAddBook = (Button) findViewById(R.id.buttonAddBook);
    buttonAddBook.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View view) {
        onAddBookClicked();
      }
    });

    mTogglePagesPercent.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
      @Override
      public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
        mEditPageCount.setEnabled(checked);
        String rememberedValue = (String) mEditPageCount.getTag();
        if(checked && rememberedValue != null) {
          mEditPageCount.setText(rememberedValue);
        } else {
          mEditPageCount.setTag(mEditPageCount.getText().toString());
          mEditPageCount.setText("100.00%");
        }
      }
    });
  }

  private void onAddBookClicked() {
    if(!validateFields()) {
      return;
    }

    LocalReading localReading = new LocalReading();

    localReading.title = mEditTitle.getText().toString();
    localReading.author = mEditAuthor.getText().toString();
    localReading.coverURL = mCoverURL;

    if(mTogglePagesPercent.isChecked()) {
      localReading.totalPages = Integer.parseInt(mEditPageCount.getText().toString());
    } else {
      // TODO Add percentage support
    }

    boolean isPublic = mTogglePublicPrivate.isChecked();

    // TODO inline spinner on the button
    getApp().showProgressDialog(this, "Connecting your book to Readmill...");

    ConnectReadingTask.connect(localReading, isPublic, new ConnectedReadingListener() {
      @Override public void onLocalReadingConnected(LocalReading localReading) {
        getApp().clearProgressDialog();
        if(localReading == null) {
          toastLong("Could not connect with Readmill");
        } else {
          exitToReadingSession(localReading);
        }
      }
    });
  }

  /**
   * Validates the input fields and moves focus to any invalid ones.
   *
   * @return true if all fields were valid, otherwise false
   */
  private boolean validateFields() {
    if(mEditTitle.getText().length() < 1) {
      toast("Please enter the title");
      mEditTitle.requestFocus();
      return false;
    }

    if(mEditAuthor.getText().length() < 1) {
      toast("Please enter the name of the author");
      mEditAuthor.requestFocus();
      return false;
    }

    // Validate a reasonable amount of page numbers
    if(mTogglePagesPercent.isChecked()) {
      int pageCount = 0;

      try {
        pageCount = Integer.parseInt(mEditPageCount.getText().toString());
      } catch(NumberFormatException ignored) {}

      if(pageCount < 1) {
        toast("Please enter a reasonable number of pages");
        mEditPageCount.requestFocus();
        return false;
      }
    }

    return true;
  }

  private void exitToReadingSession(LocalReading localReading) {
    Intent readingSessionIntent = new Intent(this, ActivityBook.class);
    readingSessionIntent.putExtra(IntentKeys.READING_ID, localReading.id);
    readingSessionIntent.putExtra(IntentKeys.START_READING_SESSION, true);
    startActivity(readingSessionIntent);
    setResult(ActivityCodes.RESULT_OK);
    finish();
  }
}
