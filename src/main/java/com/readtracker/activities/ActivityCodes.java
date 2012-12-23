package com.readtracker.activities;

import android.app.Activity;

public class ActivityCodes {
  public static final int RESULT_OK = Activity.RESULT_OK;
  public static final int RESULT_CANCELED = Activity.RESULT_CANCELED;
  public static final int RESULT_PAUSED = Activity.RESULT_FIRST_USER;
  public static final int RESULT_SIGN_OUT = Activity.RESULT_FIRST_USER + 1;

  public static final int REQUEST_ADD_BOOK = 0;
  public static final int REQUEST_READING_SESSION = 1;
  public static final int CREATE_PING = 2;
  public static final int SETTINGS = 3;
  public static final int CLOSE_BOOK = 4;
  public static final int CREATE_HIGHLIGHT = 5;
  public static final int REQUEST_CREATE_ACCOUNT = 6;
  public static final int REQUEST_SIGN_IN = 7;
  public static final int REQUEST_EDIT_BOOK = 8;

}
