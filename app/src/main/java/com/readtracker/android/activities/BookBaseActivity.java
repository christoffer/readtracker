package com.readtracker.android.activities;

import android.os.Build;
import android.support.v7.app.ActionBar;
import android.widget.ImageView;

import com.readtracker.android.ApplicationReadTracker;
import com.readtracker.android.db.LocalReading;
import com.readtracker.android.thirdparty.DrawableManager;

public class BookBaseActivity extends ReadTrackerActivity {
  private LocalReading mReading;

  void setReading(LocalReading reading) {
    mReading = reading;
    setupActionBar(mReading);
  }

  LocalReading getReading() {
    return mReading;
  }

  private void setupActionBar(LocalReading localReading) {
    ActionBar actionBar = getSupportActionBar();

    // Set the cover as the home icon. Unfortunately it seems like there's no easy way of getting
    // the imageview from the actionbar pre-11. So Gingerbread will be stuck with the default image...
    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
      ImageView homeIcon = (ImageView) findViewById(android.R.id.home);
      if(homeIcon != null) {
        DrawableManager drawMgr = ApplicationReadTracker.getDrawableManager();
        drawMgr.fetchDrawableOnThread(localReading.coverURL, homeIcon);
      }
    }

    actionBar.setTitle(localReading.title);
    actionBar.setSubtitle(localReading.author);
    actionBar.setDisplayHomeAsUpEnabled(true);
    actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_HOME | ActionBar.DISPLAY_SHOW_TITLE);
  }
}
