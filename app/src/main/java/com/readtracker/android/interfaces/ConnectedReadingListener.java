package com.readtracker.android.interfaces;

import com.readtracker.android.db.LocalReading;

public interface ConnectedReadingListener {
  public void onLocalReadingConnected(LocalReading localReading);
}
