package com.readtracker.tasks;

import com.readtracker.db.LocalReading;

public interface ConnectedReadingListener {
  public void onLocalReadingConnected(LocalReading localReading);
}
