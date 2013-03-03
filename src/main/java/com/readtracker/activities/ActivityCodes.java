package com.readtracker.activities;

import android.app.Activity;

class ActivityCodes {
  public static final int RESULT_OK = Activity.RESULT_OK;
  public static final int RESULT_CANCELED = Activity.RESULT_CANCELED;

  public static final int RESULT_SIGN_OUT = Activity.RESULT_FIRST_USER + 1;
  public static final int RESULT_DELETED_BOOK = Activity.RESULT_FIRST_USER + 2;

  public static final int REQUEST_ADD_BOOK = 0;
  public static final int REQUEST_READING_SESSION = 1;
  public static final int CREATE_PING = 2;
  public static final int SETTINGS = 3;
  public static final int CREATE_HIGHLIGHT = 5;
  public static final int REQUEST_CREATE_ACCOUNT = 6;
  public static final int REQUEST_SIGN_IN = 7;
  public static final int REQUEST_EDIT_PAGE_NUMBERS = 8;
  public static final int REQUEST_FINISH_READING = 9;
  public static final int REQUEST_BOOK_SETTINGS = 10;

  public static final int RESULT_REQUESTED_BOOK_SETTINGS = 11;
}
