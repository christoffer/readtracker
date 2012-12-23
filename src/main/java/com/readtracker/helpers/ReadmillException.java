package com.readtracker.helpers;

/**
 * Represents a generic error that occurred while sending data to Readmill.
 */
public class ReadmillException extends Exception {
  String message;
  int statusCode;

  ReadmillException(String message, int statusCode) {
    this.message = message;
    this.statusCode = statusCode;
  }

  ReadmillException(String message) {
    this.message = message;
    this.statusCode = -1;
  }

  public String toString() {
    return "ReadmillException: Status code: " + statusCode + ", message: " + message;
  }

  public int getStatusCode() {
    return statusCode;
  }
}
