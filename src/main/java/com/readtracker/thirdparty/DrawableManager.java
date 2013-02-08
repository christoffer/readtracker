package com.readtracker.thirdparty;

/*
 Licensed to the Apache Software Foundation (ASF) under one
 or more contributor license agreements.  See the NOTICE file
 distributed with this work for additional information
 regarding copyright ownership.  The ASF licenses this file
 to you under the Apache License, Version 2.0 (the
 "License"); you may not use this file except in compliance
 with the License.  You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing,
 software distributed under the License is distributed on an
 "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 KIND, either express or implied.  See the License for the
 specific language governing permissions and limitations
 under the License.
 */

// This code was written by James A Wilson
// and modified by Martin Flodin
// Found on stackoverflow:
// http://stackoverflow.com/questions/541966/android-how-do-i-do-a-lazy-load-of-images-in-listview

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.ImageView;
import com.readtracker.ApplicationReadTracker;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

public class DrawableManager {
  private final Map<String, Drawable> memoryCache;
  private static final String TAG = DrawableManager.class.getSimpleName();
  private static final String PACKAGE_NAME = ApplicationReadTracker.class.getPackage().getName();
  private static final String COVERS_FOLDER = "cache/covers/";

  private boolean mPersistDrawables = false;
  private boolean mExternalStorageAvailable = false;

  public DrawableManager() {
    memoryCache = new HashMap<String, Drawable>();
    String state = Environment.getExternalStorageState();
    mExternalStorageAvailable = Environment.MEDIA_MOUNTED.equals(state);
  }

  public void persistDrawables(boolean enable) {
    mPersistDrawables = enable;
  }

  private Drawable getCachedImage(String urlString) {
    if(memoryCache.get(urlString) != null) {
      return memoryCache.get(urlString);
    }

    // Try to fetch a stored image
    Drawable storedDrawable = readDrawable(urlString);
    if(storedDrawable != null) {
      // Put into memory cache for faster retrieval next time
      memoryCache.put(urlString, storedDrawable);
    }

    return storedDrawable;
  }

  public void fetchDrawableOnThread(final String urlString, final ImageView imageView) {
    Log.v(TAG, "Fetching image with url: " + urlString);

    Drawable cachedImage = getCachedImage(urlString);

    if(cachedImage != null) {
      Log.v(TAG, "Using cached image of " + urlString);
      imageView.setImageDrawable(cachedImage);
      return;
    }

    // Set the image drawable on the main thread
    final Handler handler = new Handler() {
      @Override
      public void handleMessage(Message message) {
        if(message.obj != null) {
          imageView.setImageDrawable((Drawable) message.obj);
        }
      }
    };

    // Download the image in a background thread
    Thread thread = new Thread() {
      @Override
      public void run() {
        BitmapDrawable drawable = fetchBitmapDrawable(urlString);
        if(mExternalStorageAvailable && mPersistDrawables && drawable != null) {
          writeBitmapDrawable(urlString, drawable);
        }
        Message message = handler.obtainMessage(1, drawable);
        handler.sendMessage(message);
      }
    };
    thread.start();
  }

  private String keyNameFromUrl(String urlString) {
    return GetMD5HashAsHexString(urlString);
  }

  private String GetMD5HashAsHexString(String s) {
    try {
      // Create MD5 Hash
      MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
      digest.update(s.getBytes());
      byte messageDigest[] = digest.digest();

      // Create Hex String
      StringBuffer hexString = new StringBuffer();
      for(int i = 0; i < messageDigest.length; i++)
        hexString.append(Integer.toHexString(0xFF & messageDigest[i]));
      return hexString.toString();
    } catch(NoSuchAlgorithmException e) {
      Log.e(TAG, "Could not get MD5 instance", e);
    }
    return "";
  }

  private BitmapDrawable fetchBitmapDrawable(String urlString) {
    Log.i(TAG, "Fetching remote drawable from URL: " + urlString);
    try {
      URI imageURI = URI.create(urlString);
      if(imageURI.getHost() == null) {
        throw new URISyntaxException(urlString, "A correct host is required");
      }
      InputStream is = getInputStreamForURI(imageURI);
      BitmapDrawable drawable = (BitmapDrawable) BitmapDrawable.createFromStream(is, "src");
      if(drawable != null) {
        memoryCache.put(urlString, drawable);
      }
      return drawable;
    } catch(URISyntaxException e) {
      Log.i(TAG, "Invalid URL: " + urlString + ". Not loading.");
    } catch(Exception e) {
      Log.e(TAG, "fetchDrawable failed", e);
    }
    return null;
  }

  private InputStream getInputStreamForURI(URI resourceURI) throws IOException, IllegalStateException, URISyntaxException {
    DefaultHttpClient httpClient = new DefaultHttpClient();
    HttpGet request = new HttpGet(resourceURI);
    HttpResponse response = httpClient.execute(request);
    return response.getEntity().getContent();
  }

  private void writeBitmapDrawable(String urlString, BitmapDrawable drawable) {
    String keyName = keyNameFromUrl(urlString);

    try {
      Bitmap image = drawable.getBitmap();
      File coversFolder = new File(getCoverFolderPath());
      coversFolder.mkdirs();

      String fullpath = coversFolder + "/" + keyName;
      Log.v(TAG, "Writing drawable to storage: " + fullpath);
      FileOutputStream fOut = new FileOutputStream(fullpath);
      image.compress(Bitmap.CompressFormat.PNG, 100, fOut);
      fOut.flush();
      fOut.close();
    } catch(IOException e) {
      Log.e(TAG, "Could not write file " + keyName, e);
    }
  }

  private String getCoverFolderPath() {
    return Environment.getExternalStorageDirectory().getPath() + "/Android/data/" + PACKAGE_NAME + "/" + COVERS_FOLDER;
  }

  private BitmapDrawable readDrawable(String urlString) {
    File coverFile = new File(getCoverFolderPath(), keyNameFromUrl(urlString));

    if(coverFile.canRead()) {
      Log.v(TAG, "Reading image from storage: " + coverFile.getAbsolutePath());
      return (BitmapDrawable) BitmapDrawable.createFromPath(coverFile.getPath());
    }
    Log.i(TAG, "Failed to read image: " + coverFile.getAbsolutePath());
    return null;
  }

  /**
   * Explicit reclaim of memory for all images in the memory cache.
   */
  public void recycleAll() {
    Log.d(TAG, "Recycling all bitmaps...");

    if(memoryCache.isEmpty()) {
      return;
    }

    for(Drawable drawable : memoryCache.values()) {
      try {
        BitmapDrawable bitmap = (BitmapDrawable) drawable;
        bitmap.getBitmap().recycle();
      } catch(Exception e) {
        Log.w(TAG, "Could not recycle drawable", e);
      }
    }
    memoryCache.clear();
  }

  public void deletePersistedCover(String coverURL) {
    String keyName = keyNameFromUrl(coverURL);
    File coverFile = new File(getCoverFolderPath(), keyName);
    Log.i(TAG, "Deleting image from storage: " + coverFile.getAbsolutePath());
    if(!coverFile.delete()) {
      Log.w(TAG, "File deletion was not successful.");
    }
  }
}
