package com.readtracker.utils;

public class GoogleBookSearchException extends Exception {
  private static final long serialVersionUID = 7135772463808664968L;

  private String mMessage = "";

  public GoogleBookSearchException() {}

  public GoogleBookSearchException(String message) {
    mMessage = message;
  }

  @Override
  public String toString() {
    return mMessage;
  }
}
