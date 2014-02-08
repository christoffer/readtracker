package com.readtracker.android.db;

import com.j256.ormlite.field.DatabaseField;

/**
 * Base for objects that should be saved to the database.
 */
abstract public class Model {
  @DatabaseField(generatedId = true) private long mId;

  public long getId() {
    return mId;
  }
}
