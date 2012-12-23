package com.readtracker_beta.interfaces;

import com.readtracker_beta.db.LocalReading;

public interface ConnectedReadingListener {
  public void onLocalReadingConnected(LocalReading localReading);
}
