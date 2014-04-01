package com.readtracker.android.interfaces;

/** Defines an interface for background jobs. */
public interface BackgroundJob {
  public void run();
  public void done();
}