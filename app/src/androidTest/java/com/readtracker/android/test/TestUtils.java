package com.readtracker.android.test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class TestUtils {

  public static String readTestDataFile(String _filename) {
    return readTestDataFile(ClassLoader.getSystemClassLoader(), _filename);
  }

  /**
   * Use for instrumentation tests
   */
  public static String readTestDataFile(ClassLoader classloader, String filename) {
    InputStream inputStream = classloader.getResourceAsStream(filename);

    if(inputStream == null)
      throw new IllegalArgumentException("Test data file not found on classpath: " + filename);

    BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
    StringBuilder sb = new StringBuilder();
    String line;
    try {
      while((line = reader.readLine()) != null) {
        sb.append(line);
      }
      reader.close();
      return sb.toString();
    } catch(IOException ex) {
      throw new RuntimeException("Test data file could not be read: " + filename, ex);
    } finally {
      closeQuietly(inputStream);
    }
  }

  public static void closeQuietly(InputStream inputStream) {
    if(inputStream != null) {
      try {
        inputStream.close();
      } catch(IOException e) {
        // ignore
      }
    }
  }
}
