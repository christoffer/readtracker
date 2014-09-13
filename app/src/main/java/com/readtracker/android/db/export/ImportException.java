package com.readtracker.android.db.export;

/** Base class for import exceptions. */
public class ImportException extends Exception {
  public ImportException() {
    this("Error while importing data");
  }

  public ImportException(String message) {
    super(message);
  }
}
