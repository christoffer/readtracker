package com.readtracker.android.support;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.support.v7.graphics.Palette;
import android.util.Log;

import com.readtracker.BuildConfig;
import com.readtracker.android.ReadTrackerApp;
import com.readtracker.android.db.Book;
import com.readtracker.android.db.DatabaseManager;
import com.squareup.picasso.Downloader;
import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.HashMap;

public class LoadBookCoverTask extends AsyncTask<Void, Void, Bitmap> {
  private static final String TAG = LoadBookCoverTask.class.getName();
  private final WeakReference<ReadTrackerApp> mAppContextRef;
  private final Book mBook;
  private final Callback mCallback;

  private static final HashMap<String, Bitmap> imageCache = new HashMap<>();
  // This is just a dummy bitmap used as a placeholder to mark the image as "tried but failed"
  private static final Bitmap ERROR_BITMAP = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);

  public LoadBookCoverTask(ReadTrackerApp app, Book book, Callback callback) {
    mCallback = callback;
    mAppContextRef = new WeakReference<>(app);
    mBook = book;
  }

  @Override protected Bitmap doInBackground(Void... ignored) {
    ReadTrackerApp app = mAppContextRef.get();
    if(app == null || mBook.getCoverImageUrl() == null) {
      // Nothing to do if the app shut down or the book doesn't have a cover
      return null;
    }

    final String url = mBook.getCoverImageUrl();

    Bitmap bitmap = imageCache.get(url);
    if(bitmap == null) {
      try {
        bitmap = Picasso
            .with(app)
            .load(mBook.getCoverImageUrl())
            .get();
        imageCache.put(url, bitmap);
      } catch(IOException ex) {
        if(BuildConfig.DEBUG) ex.printStackTrace();
        imageCache.put(url, ERROR_BITMAP);
      }
    }

    if(bitmap != null) {
      if(mBook.getBookPaletteJSON() == null) {
        // Book doesn't have a palette, generate one from the image and store it on the book
        final BookPalette bookPalette = new BookPalette(Palette.from(bitmap).generate());
        mBook.setBookPalette(bookPalette);
        DatabaseManager dbManager = app.getDatabaseManager();
        dbManager.save(mBook);
      }
    } else {
      Log.d(TAG, String.format("Failed to load %s", url));
    }

    return bitmap;
  }

  @Override protected void onPostExecute(Bitmap coverImageBitmap) {
    if(coverImageBitmap == null) {
      return;
    }

    if(mCallback != null) {
      mCallback.onBookCoverProcessed(mBook, coverImageBitmap);
    }
  }

  public interface Callback {
    void onBookCoverProcessed(Book book, Bitmap coverImageBitmap);
  }
}
