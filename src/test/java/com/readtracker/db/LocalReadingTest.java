package com.readtracker.db;

import com.readtracker.support.ReadmillApiHelper;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;

public class LocalReadingTest {
  private LocalReading localReading;

  private static final int SECONDS = 1000; // Multiplier for converting ms to sec

  @Before
  public void createTestSubject() {
    localReading = new LocalReading();
    localReading.readmillState = ReadmillApiHelper.ReadingState.READING;
  }

  @Test
  public void estimateTimeLeftWhenNotStarted() throws Exception {
    localReading.timeSpentMillis = 0;
    localReading.progress = 0.0;
    assertEquals(0, localReading.estimateTimeLeft());
  }

  @Test
  public void estimateTimeLeftWhenHalfRead() {
    localReading.progress = 0.5;
    localReading.timeSpentMillis = 10 * SECONDS;
    assertEquals(5, localReading.estimateTimeLeft());
  }

  @Test
  public void estimateTimeLeftWhenFinished() throws Exception {
    localReading.timeSpentMillis = 10 * SECONDS;
    localReading.progress = 0.5;
    localReading.readmillState = ReadmillApiHelper.ReadingState.FINISHED;
    assertEquals(0, localReading.estimateTimeLeft());
  }

  @Test
  public void estimateTimeLeftWhenReadTwoThirds() {
    localReading.progress = 0.666667;
    localReading.timeSpentMillis = 100 * SECONDS;
    assertEquals(33, localReading.estimateTimeLeft());
  }

  @Test
  public void estimateTimeLeftWhenReadOneThird() {
    localReading.progress = 0.333334;
    localReading.timeSpentMillis = 100 * SECONDS;
    assertEquals(66, localReading.estimateTimeLeft());
  }

  @Test
  public void estimateTimeLeftWhenRead99Percent() {
    localReading.progress = 0.99;
    localReading.timeSpentMillis = 100 * SECONDS;
    assertEquals(1, localReading.estimateTimeLeft());
  }
}
