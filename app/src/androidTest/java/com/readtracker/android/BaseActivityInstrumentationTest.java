package com.readtracker.android;

import android.app.Application;
import android.test.ApplicationTestCase;

/**
 * Created by anthonymonori on 05/10/15.
 * <a href="http://d.android.com/tools/testing/testing_android.html">Testing Fundamentals</a>
 */
public class BaseActivityInstrumentationTest extends ApplicationTestCase<Application> {

  public BaseActivityInstrumentationTest() {
    super(Application.class);
  }
}
