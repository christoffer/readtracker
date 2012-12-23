package com.readtracker_beta.activities;

import android.app.Activity;
import android.graphics.Typeface;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.TextView;
import com.readtracker_beta.R;
import com.readtracker_beta.db.LocalReading;

public class ViewBindingBookHeader {
  private static final String TAG = ViewBindingBookHeader.class.getSimpleName();

  public interface BookHeaderClickListener {
    public void onBackButtonClick();
  }

  /**
   * Helper method for the common case of binding a reading data instance
   * to an included book header layout.
   *
   * @param activity    Activity with an included _book_header layout
   * @param localReading LocalReading to bind
   */
  public static void bind(final Activity activity, LocalReading localReading, final BookHeaderClickListener clickListener) {
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
    ImageButton buttonBack = (ImageButton) view.findViewById(R.id.buttonBack);

    if(clickListener != null) {
      buttonBack.setOnClickListener(new View.OnClickListener() {
        @Override public void onClick(View view) {
          clickListener.onBackButtonClick();
        }
      });
    } else {
      buttonBack.setVisibility(View.GONE);
    }

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

  /**
   * Convenience method for simply finishing the activity when the back button is pressed.
   * @param activity activity to bind to
   * @param localReading local reading to display data for
   * @see ViewBindingBookHeader#bind(Activity, LocalReading, BookHeaderClickListener)
   */
  public static void bindWithDefaultClickHandler(final Activity activity, final LocalReading localReading) {
    BookHeaderClickListener clickListener = new BookHeaderClickListener() {
      @Override public void onBackButtonClick() {
        activity.finish();
      }
    };
    bind(activity, localReading, clickListener);
  }
}
