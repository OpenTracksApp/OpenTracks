/**
 * Copyright 2009 Google Inc. All Rights Reserved.
 */
package com.google.android.apps.mytracks.stats;

import junit.framework.TestCase;
import java.util.Random;

/**
 * This class test the ExtremityMonitor class.
 *
 * @author Sandor Dornbush
 */
public class ExtremityMonitorTest extends TestCase {

  public ExtremityMonitorTest(String name) {
    super(name);
  }

  public void testInitialize() {
    ExtremityMonitor monitor = new ExtremityMonitor();
    assertEquals(Double.POSITIVE_INFINITY, monitor.getMin());
    assertEquals(Double.NEGATIVE_INFINITY, monitor.getMax());
  }

  public void testSimple() {
    ExtremityMonitor monitor = new ExtremityMonitor();
    assertTrue(monitor.update(0));
    assertTrue(monitor.update(1));
    assertEquals(0.0, monitor.getMin());
    assertEquals(1.0, monitor.getMax());
    assertFalse(monitor.update(1));
    assertFalse(monitor.update(0.5));
  }

  /**
   * Throws a bunch of random numbers between [0,1] at the monitor.
   */
  public void testRandom() {
    ExtremityMonitor monitor = new ExtremityMonitor();
    Random random = new Random(42);
    for (int i = 0; i < 1000; i++) {
      monitor.update(random.nextDouble());
    }
    assertTrue(monitor.getMin() < 0.1);
    assertTrue(monitor.getMax() < 1.0);
    assertTrue(monitor.getMin() >= 0.0);
    assertTrue(monitor.getMax() > 0.9);
  }

  public void testReset() {
    ExtremityMonitor monitor = new ExtremityMonitor();
    assertTrue(monitor.update(0));
    assertTrue(monitor.update(1));
    monitor.reset();
    assertEquals(Double.POSITIVE_INFINITY, monitor.getMin());
    assertEquals(Double.NEGATIVE_INFINITY, monitor.getMax());
    assertTrue(monitor.update(0));
    assertTrue(monitor.update(1));
    assertEquals(0.0, monitor.getMin());
    assertEquals(1.0, monitor.getMax());
  }
}
