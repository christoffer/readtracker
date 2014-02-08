package com.readtracker.android.activities;

import android.os.Build;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBar;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.MenuItem;
import android.widget.ImageView;

import com.readtracker.android.R;
import com.readtracker.android.db.LocalReading;
import com.squareup.picasso.Picasso;

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
      if(homeIcon != null && !TextUtils.isEmpty(localReading.coverURL)) {
        int size = getActionBarHeight();
        if(size == 0) size = 48; // Arbitrary default value
        Picasso.with(this).load(localReading.coverURL).placeholder(R.drawable.readmill_sync).resize(size, size).centerCrop().into(homeIcon);
        actionBar.setDisplayShowHomeEnabled(true);
      }
    }

    actionBar.setTitle(localReading.title);
    actionBar.setSubtitle(localReading.author);
    actionBar.setDisplayHomeAsUpEnabled(true);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    if(item.getItemId() == android.R.id.home) {
      NavUtils.navigateUpFromSameTask(this);
      return true;
    }
    return super.onOptionsItemSelected(item);
  }

  public int getActionBarHeight() {
    // Calculate ActionBar height
    TypedValue tv = new TypedValue();
    if(getTheme() != null && getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true)) {
      return TypedValue.complexToDimensionPixelSize(tv.data, getResources().getDisplayMetrics());
    }
    return 0;
  }
}
