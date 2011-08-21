package com.google.android.apps.mytracks.services.sensors;

import junit.framework.TestCase;

public class StrideReadingsTest extends TestCase {
  
  public void testNoReadingOnStartup() {
    StrideReadings strideReadings = new StrideReadings();
    assertTrue(strideReadings.getCadence() == StrideReadings.CADENCE_NOT_AVAILABLE);
  }
  
  public void testAverageCadenceAvailable() {
    StrideReadings strideReadings = new StrideReadings();
    for(int i=0;i<30;i++) {
      strideReadings.updateStrideReading(i*1000, i*2);
    }
    assertTrue(strideReadings.getCadence() > 0);
  }
  
  /* testing rollover at 127, just like the HxM seems to do it */
  public void testRollover() {
    StrideReadings strideReadings = new StrideReadings();
    for(int i=0;i<=10;i++) {
      strideReadings.updateStrideReading(i*1000, (120 + i*2) );
    }
    assertTrue(strideReadings.getCadence() > 0);
  }

}
