package com.google.android.apps.mytracks.services.sensors;

import com.google.android.apps.mytracks.Constants;
import com.google.android.apps.mytracks.services.sensors.ant.AntDirectSensorManager;
import com.google.android.apps.mytracks.services.sensors.ant.AntSRMSensorManager;
import com.google.android.maps.mytracks.R;

import android.content.SharedPreferences;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;

public class SensorManagerFactoryTest extends AndroidTestCase {

  private SharedPreferences sharedPreferences;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    
    sharedPreferences = getContext().getSharedPreferences(
        Constants.SETTINGS_NAME, 0);
    // Let's use default values.
    sharedPreferences.edit().clear().apply();
  }
  
  @SmallTest
  public void testDefaultSettings() throws Exception {
    assertNull(SensorManagerFactory.getSensorManager(getContext()));
  }
  
  @SmallTest
  public void testCreateZephyr() throws Exception {
    assertClassForName(ZephyrSensorManager.class, R.string.sensor_type_value_zephyr);
  }
  
  @SmallTest
  public void testCreateAnt() throws Exception {
    assertClassForName(AntDirectSensorManager.class, R.string.sensor_type_value_ant);
  }

  @SmallTest
  public void testCreateAntSRM() throws Exception {
    assertClassForName(AntSRMSensorManager.class, R.string.sensor_type_value_srm_ant_bridge);
  }

  private void assertClassForName(Class<?> c, int i) {
    sharedPreferences.edit()
        .putString(getContext().getString(R.string.sensor_type_key),
            getContext().getString(i))
        .apply();
    SensorManager sm = SensorManagerFactory.getSensorManager(getContext());
    assertNotNull(sm);
    assertTrue(c.isInstance(sm));
  }
}
