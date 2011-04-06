package com.google.android.apps.mytracks.services.sensors;

import com.google.android.apps.mytracks.content.Sensor;

import junit.framework.TestCase;

public class ZephyrMessageParserTest extends TestCase {

  ZephyrMessageParser parser = new ZephyrMessageParser();

  public void testIsValid() {
    byte[] buf = new byte[60];
    assertFalse(parser.isValid(buf));
    buf[0] = 0x02;
    assertFalse(parser.isValid(buf));
    buf[59] = 0x03;
    assertTrue(parser.isValid(buf));
  }

  public void testParseBuffer() {
    byte[] buf = new byte[60];
    buf[12] = 50;
    Sensor.SensorDataSet sds = parser.parseBuffer(buf);
    assertTrue(sds.hasHeartRate());
    assertTrue(sds.getHeartRate().getState() == Sensor.SensorState.SENDING);
    assertEquals(50, sds.getHeartRate().getValue());
  }

  public void testFindNextAlignment() {
    byte[] buf = new byte[60];
    assertEquals(-1, parser.findNextAlignment(buf));
    buf[10] = 0x03;
    buf[11] = 0x02;
    assertEquals(10, parser.findNextAlignment(buf));
  }
}
