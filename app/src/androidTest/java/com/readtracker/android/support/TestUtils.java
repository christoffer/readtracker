package com.readtracker.android.support;

import java.util.Random;

public class TestUtils {
  public static String generateRandomString(int length) {
    final String ALPHABET = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789 -_+/\"'";
    final Random random = new Random();
    String result = "";
    for(int i = 0; i < length; i++) {
      result += ALPHABET.charAt(random.nextInt(ALPHABET.length()));
    }

    return result;
  }
}
