package com.readtracker.interfaces;

import com.readtracker.db.LocalReading;

public interface ConnectedReadingListener {
  public void onLocalReadingConnected(LocalReading localReading);
}
