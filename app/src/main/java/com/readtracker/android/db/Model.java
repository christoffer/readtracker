package com.readtracker.android.db;

import com.j256.ormlite.field.DatabaseField;

/**
 * Base for objects that should be saved to the database.
 */
abstract public class Model {
  /* Database fields */

  @DatabaseField(generatedId = true, columnName = Columns.ID, canBeNull = false, unique = true)
  int mId;

  /* End database fields */

  public int getId() {
    return mId;
  }

  public static abstract class Columns {
    public static final String ID = "id";
  }
}
