package com.readtracker.customviews;

import android.app.Activity;
import android.graphics.Typeface;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;
import com.readtracker.ApplicationReadTracker;
import com.readtracker.R;
import com.readtracker.ReadTrackerActivity;
import com.readtracker.db.LocalReading;
import com.readtracker.thirdparty.DrawableManager;

public class ViewBindingBookHeader {
  private static final String TAG = ViewBindingBookHeader.class.getSimpleName();

  /**
   * Helper method for the common case of binding a reading data instance
   * to an included book header layout.
   *
   * @param activity    Activity with an included _book_header layout
   * @param localReading LocalReading to bind
   */
  public static void bind(Activity activity, LocalReading localReading) {
    if(localReading == null) {
      Log.w(TAG, "Got NULL LocalReading – not binding views");
      return;
    }

    ViewGroup view = (ViewGroup) activity.findViewById(R.id._bookHeader);

    if(view == null) {
      Log.w(TAG, "Called for a view that did not include book header layout - exiting.");
      return;
    }

    Log.d(TAG, "Binding views for reading data with id: " + localReading.id);

    TextView textTitle = (TextView) view.findViewById(R.id.textTitle);
    TextView textAuthor = (TextView) view.findViewById(R.id.textAuthor);

    textTitle.setVisibility(View.INVISIBLE);
    textAuthor.setVisibility(View.INVISIBLE);

    final Typeface robotoThin = ((ReadTrackerActivity) activity).getRobotoThin();
    textTitle.setTypeface(robotoThin);
    textAuthor.setTypeface(robotoThin);

    textTitle.setText(localReading.title);
    textAuthor.setText(localReading.author);

    Animation appear = AnimationUtils.loadAnimation(activity, R.anim.fade_in);
    textTitle.startAnimation(appear);
    textTitle.setVisibility(View.VISIBLE);
    textAuthor.startAnimation(appear);
    textAuthor.setVisibility(View.VISIBLE);
  }
}
