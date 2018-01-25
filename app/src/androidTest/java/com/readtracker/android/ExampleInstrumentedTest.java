package com.readtracker.android;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;

/**
 * <a href="http://d.android.com/tools/testing/testing_android.html">Testing Fundamentals</a>
 */
@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentedTest {

  public ExampleInstrumentedTest() {}

  /**
   * Application context under test
   */
  @Test
  public void usesAppContext() {
    Context appContext = InstrumentationRegistry.getTargetContext();
    assertEquals("com.readtracker", appContext.getPackageName());
  }
}
