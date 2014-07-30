package com.readtracker.android.db.export;

/** Base class for import errors. */
public class ImportError extends Throwable {
  public ImportError() {
    this("Error while importing data");
  }

  public ImportError(String message) {
    super(message);
  }
}
