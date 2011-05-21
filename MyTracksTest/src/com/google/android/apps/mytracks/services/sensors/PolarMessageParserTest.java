package com.google.android.apps.mytracks.services.sensors;

import com.google.android.apps.mytracks.content.Sensor;

import junit.framework.TestCase;

public class PolarMessageParserTest extends TestCase {

  PolarMessageParser parser = new PolarMessageParser();

  public void testIsValid() {
    // A complete and valid Polar HxM packet
    //   FE08F701D1001104FE08F702D1001104
    byte[] buf = { 0xFE,0x08,0xF7,0x01,0xD1,0x00,0x11,0x04,0xFE,0x08,0xF7,0x02,0xD1,0x00,0x11,0x04 };

    buf[0] = 0x03;                          // Invalidate header
    assertFalse(parser.isValid(buf));

    buf[0] = 0xFE;                          // Good header
    buf[2] = 0x03;                          // Invalidate checkbyte
    assertFalse(parser.isValid(buf));

    buf[2] = 0xF7;                          // Good checkbyte
    buf[3] = 0x11;                          // Invalidate sequence
    assertFalse(parser.isValid(buf));
  }

  public void testParseBuffer() {
    byte[] buf = { 0xFE,0x08,0xF7,0x01,0xD1,0x00,0x11,0x04,0xFE,0x08,0xF7,0x02,0xD1,0x00,0x11,0x04 };
    buf[5] = 70;
    Sensor.SensorDataSet sds = parser.parseBuffer(buf);
    assertTrue(sds.hasHeartRate());
    assertTrue(sds.getHeartRate().getState() == Sensor.SensorState.SENDING);
    assertEquals(70, sds.getHeartRate().getValue());
  }

  public void testFindNextAlignment() {
    byte[] buf = { 0x0E,0x08,0xF7,0x01,0xD1,0x00,0x11,0x04,0x0E,0x08,0xF7,0x02,0xD1,0x00,0x11,0x04 };
    assertEquals(-1, parser.findNextAlignment(buf));
    buf[8] = 0xFE;
    assertEquals(8, parser.findNextAlignment(buf));
  }
}
