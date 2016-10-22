package com.readtracker.android.support;

import java.util.concurrent.atomic.AtomicBoolean;

public class RuntimeTestUtils {
  private static AtomicBoolean isRunningTest;

  public static synchronized boolean isRunningTest() {
    if(isRunningTest == null) {
      boolean istest;

      try {
        Class.forName("android.support.test.espresso.Espresso");
        istest = true;
      } catch(ClassNotFoundException e) {
        istest = false;
      }

      isRunningTest = new AtomicBoolean(istest);
    }

    return isRunningTest.get();
  }
}
